package com.mqtt.core.model

/**
 * Represents an MQTT message
 *
 * @param topic The MQTT topic
 * @param payload The message payload as ByteArray
 * @param qos Quality of Service level (0, 1, or 2)
 * @param retained Whether the message should be retained by the broker
 * @param timestamp When the message was created/received
 */
data class MqttMessage(
    val topic: String,
    val payload: ByteArray,
    val qos: Int = 1,
    val retained: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Convenience constructor for String payloads
     */
    constructor(
        topic: String,
        payload: String,
        qos: Int = 1,
        retained: Boolean = false
    ) : this(
        topic = topic,
        payload = payload.toByteArray(Charsets.UTF_8),
        qos = qos,
        retained = retained
    )

    /**
     * Get payload as String (assuming UTF-8 encoding)
     */
    fun payloadAsString(): String = payload.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MqttMessage

        if (topic != other.topic) return false
        if (!payload.contentEquals(other.payload)) return false
        if (qos != other.qos) return false
        if (retained != other.retained) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topic.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + qos
        result = 31 * result + retained.hashCode()
        return result
    }
}
