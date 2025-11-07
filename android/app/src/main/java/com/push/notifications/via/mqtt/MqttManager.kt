package com.push.notifications.via.mqtt

import android.content.Context
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.nio.charset.StandardCharsets
import java.util.UUID

class MqttManager(private val context: Context) {
    private var mqttClient: Mqtt3AsyncClient? = null
    private val TAG = "MqttManager"

    // Callbacks
    var onConnectionSuccess: (() -> Unit)? = null
    var onConnectionLost: ((String) -> Unit)? = null
    var onMessageArrived: ((String, String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun connect(
        serverUri: String,
        username: String = "",
        password: String = "",
        clientId: String = "AndroidClient_${UUID.randomUUID()}"
    ) {
        try {
            // Parse server URI to extract host and port
            val uri = serverUri.replace("tcp://", "")
            val parts = uri.split(":")
            val host = parts[0]
            val port = if (parts.size > 1) parts[1].toInt() else 1883

            // Build MQTT client
            val clientBuilder = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .automaticReconnectWithDefaultConfig()

            mqttClient = clientBuilder.buildAsync()

            // Connect options
            val connectBuilder = mqttClient!!.connectWith()
                .cleanSession(true)
                .keepAlive(60)

            if (username.isNotEmpty()) {
                connectBuilder.simpleAuth()
                    .username(username)
                    .password(password.toByteArray(StandardCharsets.UTF_8))
                    .applySimpleAuth()
            }

            // Connect
            connectBuilder.send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Failed to connect: ${throwable.message}")
                        onError?.invoke("Connection failed: ${throwable.message}")
                    } else {
                        Log.d(TAG, "Connected to MQTT broker: $serverUri")
                        onConnectionSuccess?.invoke()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting: ${e.message}")
            onError?.invoke("Error: ${e.message}")
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttClient?.subscribeWith()
                ?.topicFilter(topic)
                ?.qos(com.hivemq.client.mqtt.datatypes.MqttQos.fromCode(qos)!!)
                ?.callback { publish ->
                    handleIncomingMessage(publish)
                }
                ?.send()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Failed to subscribe to $topic: ${throwable.message}")
                        onError?.invoke("Subscribe failed: ${throwable.message}")
                    } else {
                        Log.d(TAG, "Subscribed to topic: $topic")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing: ${e.message}")
            onError?.invoke("Subscribe error: ${e.message}")
        }
    }

    private fun handleIncomingMessage(publish: Mqtt3Publish) {
        val topic = publish.topic.toString()
        val payload = publish.payloadAsBytes
        val message = String(payload, StandardCharsets.UTF_8)
        Log.d(TAG, "Message arrived on topic $topic: $message")
        onMessageArrived?.invoke(topic, message)
    }

    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        try {
            mqttClient?.publishWith()
                ?.topic(topic)
                ?.payload(message.toByteArray(StandardCharsets.UTF_8))
                ?.qos(com.hivemq.client.mqtt.datatypes.MqttQos.fromCode(qos)!!)
                ?.retain(retained)
                ?.send()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Failed to publish to $topic: ${throwable.message}")
                        onError?.invoke("Publish failed: ${throwable.message}")
                    } else {
                        Log.d(TAG, "Message published to $topic: $message")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing: ${e.message}")
            onError?.invoke("Publish error: ${e.message}")
        }
    }

    fun unsubscribe(topic: String) {
        try {
            mqttClient?.unsubscribeWith()
                ?.topicFilter(topic)
                ?.send()
            Log.d(TAG, "Unsubscribed from topic: $topic")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            Log.d(TAG, "Disconnected from MQTT broker")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return mqttClient?.state?.isConnected ?: false
    }
}
