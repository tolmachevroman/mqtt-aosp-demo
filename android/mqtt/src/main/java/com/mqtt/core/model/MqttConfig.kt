package com.mqtt.core.model

import java.util.concurrent.TimeUnit

/**
 * Configuration for MQTT connection.
 *
 * @param brokerUrl The MQTT broker URL (e.g., "tcp://broker.hivemq.com:1883")
 * @param clientId Unique client identifier. If null, will be auto-generated
 * @param username Optional username for authentication
 * @param password Optional password for authentication
 * @param cleanSession If true, broker will not store session data
 * @param keepAliveInterval Keep-alive interval in seconds (default: 240s for battery optimization)
 * @param connectionTimeout Connection timeout in seconds
 * @param automaticReconnect Enable automatic reconnection
 * @param maxReconnectDelay Maximum delay between reconnection attempts
 * @param qos Default Quality of Service level (0, 1, or 2)
 * @param retained Whether messages should be retained by default
 */
data class MqttConfig(
    val brokerUrl: String,
    val clientId: String? = null,
    val username: String? = null,
    val password: String? = null,
    val cleanSession: Boolean = true,
    val keepAliveInterval: Long = 240, // 4 minutes - good balance for battery
    val connectionTimeout: Long = 30,
    val automaticReconnect: Boolean = true,
    val maxReconnectDelay: Long = 120,
    val qos: Int = 1, // At least once delivery
    val retained: Boolean = false
) {
    init {
        require(qos in 0..2) { "QoS must be 0, 1, or 2" }
        require(keepAliveInterval > 0) { "Keep alive interval must be positive" }
        require(connectionTimeout > 0) { "Connection timeout must be positive" }
    }

    companion object {
        /**
         * Creates a battery-optimized config for mobile devices
         */
        fun batteryOptimized(
            brokerUrl: String,
            clientId: String? = null,
            username: String? = null,
            password: String? = null
        ) = MqttConfig(
            brokerUrl = brokerUrl,
            clientId = clientId,
            username = username,
            password = password,
            cleanSession = false, // Persistent session to avoid resubscribing
            keepAliveInterval = 480, // 8 minutes - very battery friendly
            qos = 1, // Balance between reliability and battery
            automaticReconnect = true
        )

        /**
         * Creates a low-latency config for real-time applications
         */
        fun lowLatency(
            brokerUrl: String,
            clientId: String? = null,
            username: String? = null,
            password: String? = null
        ) = MqttConfig(
            brokerUrl = brokerUrl,
            clientId = clientId,
            username = username,
            password = password,
            cleanSession = true,
            keepAliveInterval = 60, // 1 minute - more responsive
            qos = 1,
            automaticReconnect = true
        )
    }
}
