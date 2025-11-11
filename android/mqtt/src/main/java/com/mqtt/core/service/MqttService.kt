package com.mqtt.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mqtt.core.client.HiveMqttClient
import com.mqtt.core.client.MqttClient
import com.mqtt.core.model.MqttConfig
import com.mqtt.core.model.MqttConnectionState
import com.mqtt.core.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Foreground service for maintaining persistent MQTT connection
 * with battery optimization
 */
class MqttService : Service() {

    private companion object {
        const val TAG = "MqttService"
        const val NOTIFICATION_CHANNEL_ID = "mqtt_service_channel"
        const val NOTIFICATION_ID = 1001
        const val WAKE_LOCK_TAG = "MqttService:WakeLock"
        const val WAKE_LOCK_TIMEOUT = 60_000L // 1 minute
    }

    private val binder = MqttBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var mqttClient: MqttClient
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var networkJob: Job? = null
    private var reconnectAttempts = 0

    inner class MqttBinder : Binder() {
        fun getService(): MqttService = this@MqttService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        mqttClient = HiveMqttClient()
        networkMonitor = NetworkMonitor(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        createNotificationChannel()
        startForegroundService()
        observeConnectionState()
        observeNetworkChanges()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY // Restart service if killed
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        networkJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        mqttClient.close()
        super.onDestroy()
    }

    /**
     * Connect to MQTT broker
     */
    fun connect(config: MqttConfig) {
        serviceScope.launch {
            try {
                if (networkMonitor.isNetworkAvailable()) {
                    mqttClient.connect(config)
                } else {
                    Log.w(TAG, "No network available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
            }
        }
    }

    /**
     * Disconnect from MQTT broker
     */
    fun disconnect() {
        serviceScope.launch {
            mqttClient.disconnect()
        }
    }

    /**
     * Get the MQTT client instance
     */
    fun getClient(): MqttClient = mqttClient

    private fun startForegroundService() {
        val notification = createNotification("Starting MQTT Service...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MQTT Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains MQTT connection in background"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        // Note: In real app, you should provide a valid PendingIntent to your main activity
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MQTT Service")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        return builder.build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun observeConnectionState() {
        mqttClient.connectionState
            .onEach { state ->
                Log.d(TAG, "Connection state: $state")
                when (state) {
                    is MqttConnectionState.Connected -> {
                        reconnectAttempts = 0
                        updateNotification("MQTT Connected")
                        releaseWakeLock()
                    }

                    is MqttConnectionState.Connecting -> {
                        updateNotification("Connecting to MQTT...")
                        acquireWakeLock() // Ensure connection completes
                    }

                    is MqttConnectionState.Disconnected -> {
                        updateNotification("MQTT Disconnected")
                        releaseWakeLock()
                    }

                    is MqttConnectionState.Reconnecting -> {
                        updateNotification("Reconnecting (${state.attempt})...")
                        acquireWakeLock()
                    }

                    is MqttConnectionState.Error -> {
                        updateNotification("MQTT Error")
                        releaseWakeLock()
                        handleConnectionError(state.error)
                    }

                    is MqttConnectionState.WaitingForNetwork -> {
                        updateNotification("Waiting for network...")
                        releaseWakeLock()
                    }
                }
            }
            .launchIn(serviceScope)
    }

    private fun observeNetworkChanges() {
        networkJob = networkMonitor.observeNetworkChanges()
            .onEach { isAvailable ->
                Log.d(TAG, "Network available: $isAvailable")
                if (isAvailable) {
                    handleNetworkAvailable()
                } else {
                    handleNetworkLost()
                }
            }
            .launchIn(serviceScope)
    }

    private fun handleNetworkAvailable() {
        val currentState = mqttClient.connectionState.value
        if (currentState is MqttConnectionState.WaitingForNetwork) {
            // Try to reconnect
            mqttClient.getConfig()?.let { config ->
                connect(config)
            }
        }
    }

    private fun handleNetworkLost() {
        // Connection will be lost automatically, but we can update state
        Log.d(TAG, "Network lost")
    }

    private fun handleConnectionError(error: Throwable) {
        Log.e(TAG, "Connection error", error)

        // Implement exponential backoff for reconnection
        reconnectAttempts++
        val delay = calculateBackoffDelay(reconnectAttempts)

        serviceScope.launch {
            delay(delay)

            if (networkMonitor.isNetworkAvailable()) {
                mqttClient.getConfig()?.let { config ->
                    Log.d(TAG, "Attempting reconnection #$reconnectAttempts")
                    connect(config)
                }
            }
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s, ..., max 120s
        val base = 1000L
        val maxDelay = 120_000L
        return minOf(base * (1 shl (attempt - 1)), maxDelay)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return

        try {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            ).apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT)
            }
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release WakeLock", e)
        }
    }
}
