package com.push.notifications.via.mqtt

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MqttMessageItem(
    val topic: String,
    val message: String,
    val timestamp: String,
    val isOutgoing: Boolean
)

class MqttViewModel(
    private val mqttManager: MqttManager
) : ViewModel() {

    // State
    val isConnected = mutableStateOf(false)
    val brokerUrl = mutableStateOf("tcp://10.0.2.2:1883") // Android emulator localhost
    val subscribedTopic = mutableStateOf("test/topic")
    val publishTopic = mutableStateOf("test/topic")
    val messageToSend = mutableStateOf("")
    val messages = mutableStateListOf<MqttMessageItem>()
    val statusMessage = mutableStateOf("Disconnected")

    init {
        setupMqttCallbacks()
    }

    private fun setupMqttCallbacks() {
        mqttManager.onConnectionSuccess = {
            isConnected.value = true
            statusMessage.value = "Connected to broker"
            // Auto-subscribe to default topic
            if (subscribedTopic.value.isNotEmpty()) {
                mqttManager.subscribe(subscribedTopic.value)
            }
        }

        mqttManager.onConnectionLost = { reason ->
            isConnected.value = false
            statusMessage.value = "Connection lost: $reason"
        }

        mqttManager.onMessageArrived = { topic, message ->
            addMessage(topic, message, isOutgoing = false)
        }

        mqttManager.onError = { error ->
            statusMessage.value = "Error: $error"
        }
    }

    fun connect() {
        statusMessage.value = "Connecting..."
        mqttManager.connect(brokerUrl.value)
    }

    fun disconnect() {
        mqttManager.disconnect()
        isConnected.value = false
        statusMessage.value = "Disconnected"
    }

    fun subscribe() {
        if (subscribedTopic.value.isNotEmpty()) {
            mqttManager.subscribe(subscribedTopic.value)
            statusMessage.value = "Subscribed to ${subscribedTopic.value}"
        }
    }

    fun publishMessage() {
        if (messageToSend.value.isNotEmpty() && publishTopic.value.isNotEmpty()) {
            mqttManager.publish(publishTopic.value, messageToSend.value)
            addMessage(publishTopic.value, messageToSend.value, isOutgoing = true)
            messageToSend.value = ""
        }
    }

    private fun addMessage(topic: String, message: String, isOutgoing: Boolean) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        messages.add(0, MqttMessageItem(topic, message, timestamp, isOutgoing))
    }

    fun clearMessages() {
        messages.clear()
    }

    override fun onCleared() {
        super.onCleared()
        mqttManager.disconnect()
    }
}
