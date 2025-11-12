package com.mqtt.core.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.mqtt.core.data.datasource.MqttClient
import com.mqtt.core.domain.model.MqttConfig
import com.mqtt.core.domain.model.MqttConnectionState
import com.mqtt.core.domain.model.MqttMessage
import com.mqtt.core.ui.service.MqttService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.java

/**
 * Repository for MQTT operations
 * Handles service binding and provides a clean API for the app layer
 */
class MqttRepository(private val context: Context) {

    private companion object {
        const val TAG = "MqttRepository"
    }

    private var mqttService: MqttService? = null
    private var isBound = false

    private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Unbound)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as MqttService.MqttBinder
            mqttService = binder.getService()
            isBound = true
            _serviceState.value = ServiceState.Bound
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            mqttService = null
            isBound = false
            _serviceState.value = ServiceState.Unbound
        }
    }

    /**
     * Start and bind to the MQTT service
     */
    suspend fun startService(): Result<Unit> = suspendCoroutine { continuation ->
        try {
            val intent = Intent(context, MqttService::class.java)

            // Start service
            context.startForegroundService(intent)

            // Bind to service
            val bound = context.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            if (bound) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(Result.failure(Exception("Failed to bind service")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Stop and unbind from the MQTT service
     */
    fun stopService() {
        try {
            if (isBound) {
                context.unbindService(serviceConnection)
                isBound = false
            }

            val intent = Intent(context, MqttService::class.java)
            context.stopService(intent)

            _serviceState.value = ServiceState.Unbound
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop service", e)
        }
    }

    /**
     * Connect to MQTT broker
     */
    fun connect(config: MqttConfig) {
        mqttService?.connect(config)
            ?: Log.e(TAG, "Service not bound, cannot connect")
    }

    /**
     * Disconnect from MQTT broker
     */
    fun disconnect() {
        mqttService?.disconnect()
            ?: Log.e(TAG, "Service not bound, cannot disconnect")
    }

    /**
     * Get the MQTT client instance
     */
    fun getClient(): MqttClient? {
        return mqttService?.getClient()
    }

    /**
     * Get the connection state
     */
    fun getConnectionState(): StateFlow<MqttConnectionState>? {
        return mqttService?.getClient()?.connectionState
    }

    /**
     * Publish a message
     */
    suspend fun publish(message: MqttMessage): Result<Unit> {
        val client = mqttService?.getClient()
        return if (client != null) {
            client.publish(message)
        } else {
            Result.failure(IllegalStateException("Service not bound"))
        }
    }

    /**
     * Subscribe to a topic
     */
    fun subscribe(topic: String, qos: Int = 1): Flow<MqttMessage>? {
        return mqttService?.getClient()?.subscribe(topic, qos)
    }

    /**
     * Unsubscribe from a topic
     */
    suspend fun unsubscribe(topic: String): Result<Unit> {
        val client = mqttService?.getClient()
        return if (client != null) {
            client.unsubscribe(topic)
        } else {
            Result.failure(IllegalStateException("Service not bound"))
        }
    }

    /**
     * Check if connected to broker
     */
    fun isConnected(): Boolean {
        return mqttService?.getClient()?.isConnected() ?: false
    }

    sealed class ServiceState {
        data object Unbound : ServiceState()
        data object Bound : ServiceState()
    }
}
