package com.mqtt.core.client

import com.mqtt.core.model.MqttConfig
import com.mqtt.core.model.MqttConnectionState
import com.mqtt.core.model.MqttMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for MQTT client operations
 */
interface MqttClient {

    /**
     * Current connection state as a StateFlow
     */
    val connectionState: StateFlow<MqttConnectionState>

    /**
     * Connect to the MQTT broker
     */
    suspend fun connect(config: MqttConfig)

    /**
     * Disconnect from the MQTT broker
     */
    suspend fun disconnect()

    /**
     * Publish a message to a topic
     */
    suspend fun publish(message: MqttMessage): Result<Unit>

    /**
     * Subscribe to a topic and receive messages as a Flow
     * @param topic The topic to subscribe to (can include wildcards like +, #)
     * @param qos Quality of Service level
     */
    fun subscribe(topic: String, qos: Int = 1): Flow<MqttMessage>

    /**
     * Unsubscribe from a topic
     */
    suspend fun unsubscribe(topic: String): Result<Unit>

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean

    /**
     * Get the current configuration
     */
    fun getConfig(): MqttConfig?

    /**
     * Cleanup and release resources
     */
    fun close()
}
