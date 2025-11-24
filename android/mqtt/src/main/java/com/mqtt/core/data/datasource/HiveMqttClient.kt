package com.mqtt.core.data.datasource

import android.util.Log
import com.hivemq.client.mqtt.MqttClient as HiveMqClient
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.mqtt.core.domain.model.MqttConfig
import com.mqtt.core.domain.model.MqttConnectionState
import com.mqtt.core.domain.model.MqttMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.security.KeyStore
import java.security.cert.X509Certificate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * HiveMQ implementation of MqttClient with battery optimization
 */
class HiveMqttClient : MqttClient {

    private companion object {
        const val TAG = "HiveMqttClient"
    }

    private var client: Mqtt3AsyncClient? = null
    private var currentConfig: MqttConfig? = null

    private val _connectionState =
        MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(config: MqttConfig) {
        if (isConnected()) {
            Log.d(TAG, "Already connected")
            return
        }

        _connectionState.value = MqttConnectionState.Connecting
        currentConfig = config

        try {
            val clientId = config.clientId ?: generateClientId()
            val brokerUri = parseBrokerUrl(config.brokerUrl)

            Log.d(TAG, "Connecting with config:")
            Log.d(TAG, "  Client ID: $clientId")
            Log.d(TAG, "  Broker: ${brokerUri.host}:${brokerUri.port}")
            Log.d(TAG, "  SSL: ${brokerUri.ssl}")
            Log.d(TAG, "  Username: ${config.username}")
            Log.d(TAG, "  Has Password: ${config.password != null}")

            // Build the MQTT client
            val clientBuilder = Mqtt3Client.builder()
                .identifier(clientId)
                .serverHost(brokerUri.host)
                .serverPort(brokerUri.port)

            // Add SSL/TLS if needed
            if (brokerUri.ssl) {
                Log.d(TAG, "Configuring SSL for ${brokerUri.host}")

                try {
                    // Create TrustManagerFactory using Android's system trust store
                    val trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm()
                    )
                    // Initialize with null to use the system's default trust store
                    // This will trust all certificates signed by well-known CAs including Let's Encrypt
                    trustManagerFactory.init(null as KeyStore?)

                    clientBuilder.sslConfig()
                        .trustManagerFactory(trustManagerFactory)
                        .handshakeTimeout(30, TimeUnit.SECONDS)
                        .applySslConfig()

                    Log.d(TAG, "SSL configured with Android system trust store")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to configure SSL", e)
                    // Fall back to default SSL config
                    clientBuilder.sslWithDefaultConfig()
                    Log.d(TAG, "SSL configured with default config (fallback)")
                }
            }

            // Configure automatic reconnection with exponential backoff
            clientBuilder.automaticReconnect()
                .initialDelay(1, TimeUnit.SECONDS)
                .maxDelay(120, TimeUnit.SECONDS)
                .applyAutomaticReconnect()

            client = clientBuilder.buildAsync()
            Log.d(TAG, "MQTT client built successfully")

            // Build connect options
            val connectBuilder = com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect.builder()
                .keepAlive(config.keepAliveInterval.toInt())
                .cleanSession(config.cleanSession)

            // Add authentication if provided
            if (config.username != null && config.password != null) {
                Log.d(TAG, "Adding authentication credentials")
                connectBuilder.simpleAuth()
                    .username(config.username)
                    .password(config.password.toByteArray(Charsets.UTF_8))
                    .applySimpleAuth()
            }

            Log.d(TAG, "Initiating connection...")
            // Connect with timeout (use config timeout value)
            val timeoutMillis = config.connectionTimeout * 1000
            val result = try {
                withTimeout(timeoutMillis) {
                    suspendCoroutine<Result<Mqtt3ConnAck>> { continuation ->
                        client?.connect(connectBuilder.build())
                            ?.whenComplete { ack, throwable ->
                                if (throwable != null) {
                                    Log.e(TAG, "Connection failed in whenComplete", throwable)
                                    // Log the full exception chain for debugging
                                    var cause: Throwable? = throwable
                                    var depth = 0
                                    while (cause != null && depth < 5) {
                                        Log.e(
                                            TAG,
                                            "  Cause [$depth]: ${cause::class.java.simpleName}: ${cause.message}"
                                        )
                                        cause = cause.cause
                                        depth++
                                    }
                                    continuation.resume(Result.failure(throwable))
                                } else {
                                    Log.d(TAG, "Connection successful in whenComplete")
                                    continuation.resume(Result.success(ack))
                                }
                            }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Connection timed out after $timeoutMillis ms", e)
                Result.failure(Exception("Connection timed out after $timeoutMillis ms", e))
            }

            result.onSuccess {
                _connectionState.value = MqttConnectionState.Connected
                Log.d(TAG, "Connected successfully")
            }.onFailure { error ->
                _connectionState.value = MqttConnectionState.Error(error)
                Log.e(TAG, "Connection failed", error)
            }

        } catch (e: Exception) {
            _connectionState.value = MqttConnectionState.Error(e)
            Log.e(TAG, "Connection error", e)
            throw e
        }
    }

    override suspend fun disconnect() {
        try {
            client?.let { mqttClient ->
                suspendCoroutine { continuation ->
                    mqttClient.disconnect()
                        .whenComplete { _, throwable ->
                            if (throwable != null) {
                                Log.e(TAG, "Disconnect error", throwable)
                            }
                            continuation.resume(Unit)
                        }
                }
            }
            _connectionState.value = MqttConnectionState.Disconnected
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    override suspend fun publish(message: MqttMessage): Result<Unit> {
        val mqttClient = client ?: return Result.failure(IllegalStateException("Not connected"))

        if (!isConnected()) {
            return Result.failure(IllegalStateException("Not connected"))
        }

        return try {
            suspendCoroutine { continuation ->
                val qos = when (message.qos) {
                    0 -> MqttQos.AT_MOST_ONCE
                    1 -> MqttQos.AT_LEAST_ONCE
                    2 -> MqttQos.EXACTLY_ONCE
                    else -> MqttQos.AT_LEAST_ONCE
                }

                val publishMessage =
                    com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish.builder()
                        .topic(message.topic)
                        .payload(message.payload)
                        .qos(qos)
                        .retain(message.retained)
                        .build()

                mqttClient.publish(publishMessage)
                    .whenComplete { _, throwable ->
                        if (throwable != null) {
                            Log.e(TAG, "Publish failed", throwable)
                            continuation.resume(Result.failure(throwable))
                        } else {
                            Log.d(TAG, "Published to ${message.topic}")
                            continuation.resume(Result.success(Unit))
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Publish error", e)
            Result.failure(e)
        }
    }

    override fun subscribe(topic: String, qos: Int): Flow<MqttMessage> = callbackFlow {
        val mqttClient = client ?: run {
            close(IllegalStateException("Not connected"))
            return@callbackFlow
        }

        if (!isConnected()) {
            close(IllegalStateException("Not connected"))
            return@callbackFlow
        }

        val mqttQos = when (qos) {
            0 -> MqttQos.AT_MOST_ONCE
            1 -> MqttQos.AT_LEAST_ONCE
            2 -> MqttQos.EXACTLY_ONCE
            else -> MqttQos.AT_LEAST_ONCE
        }

        try {
            // Subscribe and set up message callback
            mqttClient.subscribeWith()
                .topicFilter(topic)
                .qos(mqttQos)
                .callback { publish ->
                    val message = MqttMessage(
                        topic = publish.topic.toString(),
                        payload = publish.payloadAsBytes,
                        qos = publish.qos.code,
                        retained = publish.isRetain
                    )
                    trySend(message)
                }
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Subscribe failed", throwable)
                        close(throwable)
                    } else {
                        Log.d(TAG, "Subscribed to $topic")
                    }
                }

            awaitClose {
                // Unsubscribe when flow is closed
                try {
                    mqttClient.unsubscribeWith()
                        .topicFilter(topic)
                        .send()
                        .whenComplete { _, _ ->
                            Log.d(TAG, "Unsubscribed from $topic")
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error unsubscribing", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Subscribe error", e)
            close(e)
        }
    }

    override suspend fun unsubscribe(topic: String): Result<Unit> {
        val mqttClient = client ?: return Result.failure(IllegalStateException("Not connected"))

        return try {
            suspendCoroutine { continuation ->
                mqttClient.unsubscribeWith()
                    .topicFilter(topic)
                    .send()
                    .whenComplete { _, throwable ->
                        if (throwable != null) {
                            Log.e(TAG, "Unsubscribe failed", throwable)
                            continuation.resume(Result.failure(throwable))
                        } else {
                            Log.d(TAG, "Unsubscribed from $topic")
                            continuation.resume(Result.success(Unit))
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unsubscribe error", e)
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean {
        return client?.state == MqttClientState.CONNECTED
    }

    override fun getConfig(): MqttConfig? = currentConfig

    override fun close() {
        try {
            client?.disconnect()
            client = null
            currentConfig = null
            _connectionState.value = MqttConnectionState.Disconnected
        } catch (e: Exception) {
            Log.e(TAG, "Error closing client", e)
        }
    }

    private fun generateClientId(): String {
        return "android_${UUID.randomUUID().toString().take(8)}"
    }

    private data class BrokerUri(
        val host: String,
        val port: Int,
        val ssl: Boolean
    )

    private fun parseBrokerUrl(url: String): BrokerUri {
        // Parse URLs like "tcp://broker.hivemq.com:1883" or "ssl://broker.hivemq.com:8883"
        val parts = url.split("://")
        require(parts.size == 2) { "Invalid broker URL format. Expected: protocol://host:port" }

        val protocol = parts[0].lowercase()
        val ssl = protocol == "ssl" || protocol == "wss" || protocol == "mqtts"

        val hostPort = parts[1].split(":")
        val host = hostPort[0]
        val port = hostPort.getOrNull(1)?.toIntOrNull()
            ?: if (ssl) 8883 else 1883

        return BrokerUri(host, port, ssl)
    }
}
