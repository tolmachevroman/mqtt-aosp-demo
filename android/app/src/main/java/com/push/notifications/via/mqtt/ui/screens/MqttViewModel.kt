package com.push.notifications.via.mqtt.ui.screens

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mqtt.core.domain.model.MqttConfig
import com.mqtt.core.domain.model.MqttConnectionState
import com.mqtt.core.domain.model.MqttMessage
import com.mqtt.core.data.repository.MqttRepository
import com.push.notifications.via.mqtt.utils.ClientIdHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
    private val mqttRepository: MqttRepository
) : ViewModel(), KoinComponent {

    private val context: Context by inject()

    // State
    val isConnected = mutableStateOf(false)
    val brokerUrl =
        mutableStateOf("ssl://broker.hivemq.com:8883") // HiveMQ Public Broker with SSL/TLS
    val username = mutableStateOf("")
    val password = mutableStateOf("")
    val subscribedTopic = mutableStateOf("demo/messages")
    val publishTopic = mutableStateOf("demo/messages")
    val messageToSend = mutableStateOf("")
    val messages = mutableStateListOf<MqttMessageItem>()
    val statusMessage = mutableStateOf("Disconnected")

    private val isServiceReady = mutableStateOf(false)
    private var connectionStateJob: Job? = null
    private var subscriptionJob: Job? = null
    private var currentSubscribedTopic: String? = null

    // Track sent messages to identify them when they come back through subscription
    private val sentMessages = mutableSetOf<String>()

    init {
        startMqttService()
        observeServiceState()
    }

    private fun startMqttService() {
        viewModelScope.launch {
            val result = mqttRepository.startService()
            result.onSuccess {
                isServiceReady.value = true
                statusMessage.value = "Service ready"
            }.onFailure {
                statusMessage.value = "Failed to start service: ${it.message}"
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            mqttRepository.serviceState.collect { state ->
                isServiceReady.value = state is MqttRepository.ServiceState.Bound

                // When service becomes bound, start observing connection state
                if (state is MqttRepository.ServiceState.Bound) {
                    observeConnectionState()
                } else {
                    // Cancel connection state observation when service is unbound
                    connectionStateJob?.cancel()
                    connectionStateJob = null
                }
            }
        }
    }

    private fun observeConnectionState() {
        // Cancel previous observation if any
        connectionStateJob?.cancel()

        connectionStateJob = viewModelScope.launch {
            mqttRepository.getConnectionState()?.collect { state ->
                when (state) {
                    is MqttConnectionState.Connected -> {
                        isConnected.value = true
                        statusMessage.value = "Connected to broker"
                    }

                    is MqttConnectionState.Connecting -> {
                        isConnected.value = false
                        statusMessage.value = "Connecting..."
                    }

                    is MqttConnectionState.Disconnected -> {
                        isConnected.value = false
                        statusMessage.value = "Disconnected"
                        // Cancel subscription when disconnected
                        subscriptionJob?.cancel()
                        subscriptionJob = null
                        currentSubscribedTopic = null
                    }

                    is MqttConnectionState.Error -> {
                        isConnected.value = false
                        statusMessage.value = "Error: ${state.error.message}"
                        // Cancel subscription on error
                        subscriptionJob?.cancel()
                        subscriptionJob = null
                        currentSubscribedTopic = null
                    }

                    is MqttConnectionState.Reconnecting -> {
                        isConnected.value = false
                        statusMessage.value = "Reconnecting (${state.attempt})..."
                    }

                    is MqttConnectionState.WaitingForNetwork -> {
                        isConnected.value = false
                        statusMessage.value = "Waiting for network..."
                    }
                }
            }
        }
    }

    fun connect() {
        if (!isServiceReady.value) {
            statusMessage.value = "Service not ready yet..."
            return
        }

        viewModelScope.launch {
            val clientId = ClientIdHelper.getClientId(context)

            val config = MqttConfig(
                brokerUrl = brokerUrl.value,
                clientId = clientId,
                username = username.value.takeIf { it.isNotEmpty() },
                password = password.value.takeIf { it.isNotEmpty() },
                keepAliveInterval = 240, // 4 minutes - balanced
                qos = 1
            )
            mqttRepository.connect(config)
        }
    }

    fun disconnect() {
        mqttRepository.disconnect()
        // Cancel any active subscriptions
        subscriptionJob?.cancel()
        subscriptionJob = null
        currentSubscribedTopic = null
    }

    fun subscribe() {
        if (!isConnected.value) {
            statusMessage.value = "Cannot subscribe - not connected"
            return
        }

        if (subscribedTopic.value.isEmpty()) {
            statusMessage.value = "Cannot subscribe - topic is empty"
            return
        }

        // If already subscribed to this topic, don't subscribe again
        if (currentSubscribedTopic == subscribedTopic.value) {
            statusMessage.value = "Already subscribed to ${subscribedTopic.value}"
            return
        }

        // Cancel previous subscription if any
        subscriptionJob?.cancel()

        // Start new subscription
        subscriptionJob = viewModelScope.launch {
            mqttRepository.subscribe(subscribedTopic.value, qos = 1)?.collect { message ->
                val payload = message.payloadAsString()
                val messageKey = "${message.topic}:$payload"

                // Check if this is a message we sent
                val isOutgoing = synchronized(sentMessages) {
                    sentMessages.remove(messageKey)
                }

                addMessage(message.topic, payload, isOutgoing = isOutgoing)
            }
        }

        currentSubscribedTopic = subscribedTopic.value
        statusMessage.value = "Subscribed to ${subscribedTopic.value}"
    }

    fun publishMessage() {
        if (messageToSend.value.isNotEmpty() && publishTopic.value.isNotEmpty()) {
            val payload = messageToSend.value
            val topic = publishTopic.value

            // Mark this message as sent before publishing
            val messageKey = "$topic:$payload"
            synchronized(sentMessages) {
                sentMessages.add(messageKey)
            }

            viewModelScope.launch {
                val message = MqttMessage(
                    topic = topic,
                    payload = payload,
                    qos = 1
                )
                mqttRepository.publish(message).onFailure { error ->
                    statusMessage.value = "Publish failed: ${error.message}"
                    // Remove from sent messages if publish failed
                    synchronized(sentMessages) {
                        sentMessages.remove(messageKey)
                    }
                }
            }
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
        connectionStateJob?.cancel()
        subscriptionJob?.cancel()
        mqttRepository.disconnect()
        mqttRepository.stopService()
    }
}
