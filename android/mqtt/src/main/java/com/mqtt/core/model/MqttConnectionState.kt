package com.mqtt.core.model

/**
 * Represents the current state of the MQTT connection
 */
sealed class MqttConnectionState {
    /**
     * Not connected and not attempting to connect
     */
    data object Disconnected : MqttConnectionState()

    /**
     * Attempting to establish connection
     */
    data object Connecting : MqttConnectionState()

    /**
     * Successfully connected to broker
     */
    data object Connected : MqttConnectionState()

    /**
     * Connection lost, attempting to reconnect
     * @param attempt Current reconnection attempt number
     */
    data class Reconnecting(val attempt: Int) : MqttConnectionState()

    /**
     * Connection failed with error
     * @param error The error that caused the failure
     */
    data class Error(val error: Throwable) : MqttConnectionState()

    /**
     * Waiting for network connectivity
     */
    data object WaitingForNetwork : MqttConnectionState()
}
