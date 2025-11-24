# MQTT Android Demo

A production-ready Android application demonstrating battery-optimized MQTT implementation with clean architecture.

---

## Overview

This project showcases a **modular, battery-efficient MQTT client** for Android, built with modern best practices:

- **Clean Architecture** - Clear separation of Data, Domain, and UI layers
- **Battery Optimized** - Research-backed keep-alive intervals (~0.16% drain/hour)
- **Production Ready** - Foreground service, automatic reconnection, network monitoring
- **Modern Android** - Kotlin Coroutines, Flow/StateFlow, Jetpack Compose UI
- **Testable** - Repository pattern with dependency injection (Koin)

---

## Documentation Index

Complete documentation for the MQTT Android Demo project.

### Main Guides

- **[MQTT Integration Guide](docs/mqtt/README.md)** - Complete MQTT setup, SSL/TLS configuration,
  troubleshooting, and production deployment
- [Authentication Setup](docs/authentication/README.md) - Broker authentication and test credentials
- [Koin Dependency Injection Guide](docs/koin/README.md) - Koin DI implementation

---

## Quick Links

- [Quick Start Guide](../QUICK_START.md) - Get running in 5 minutes
- [Main README](../README.md) - Project overview
- [Setup Timeline](../MQTT_ANDROID_SETUP.md) - Implementation history
- [Recent Improvements](../IMPROVEMENTS_SUMMARY.md) - Latest changes

---

## Project Structure

```
android/
├── app/                          # Demo Android application
│   └── src/main/java/com/push/notifications/via/mqtt/
│       ├── MainActivity.kt       # Jetpack Compose UI
│       ├── MqttViewModel.kt      # UI state management
│       ├── MqttApplication.kt    # App initialization (Koin)
│       └── di/
│           └── AppModule.kt      # Dependency injection setup
│
├── mqtt/                         # Reusable MQTT module 
│   ├── src/main/java/com/mqtt/core/
│   │   ├── data/                # Data Layer
│   │   │   ├── datasource/      # MQTT client interface & implementation
│   │   │   │   ├── MqttClient.kt
│   │   │   │   └── HiveMqttClient.kt (HiveMQ implementation)
│   │   │   ├── repository/      # Repository pattern
│   │   │   │   └── MqttRepository.kt
│   │   │   └── util/            # Network monitoring
│   │   │       └── NetworkMonitor.kt
│   │   │
│   │   ├── domain/              # Domain Layer (Pure Kotlin)
│   │   │   └── model/           # Business models
│   │   │       ├── MqttConfig.kt
│   │   │       ├── MqttConnectionState.kt
│   │   │       └── MqttMessage.kt
│   │   │
│   │   └── ui/                  # UI Layer (Android-specific)
│   │       └── service/         # Foreground service
│   │           └── MqttService.kt
│   │
│   └── README.md                # Detailed module documentation
│
└── README.md                    # This file
```

---

## MQTT Module Features

- **Adaptive Keep-Alive**: 4-8 minute intervals (research-backed)
- **Smart WakeLock**: Only during critical operations with 60s timeout
- **Exponential Backoff**: Prevents reconnection storms (1s → 2s → 4s → ... → 120s)
- **Network-Aware**: Automatic reconnection on WiFi/Mobile data switches
- **Battery Drain**: ~0.16% per hour with default settings

### Reliability

- **Foreground Service**: Survives app backgrounding and Doze mode
- **START_STICKY**: Automatic restart if killed by system
- **Automatic Reconnection**: Smart retry with exponential backoff
- **QoS Support**: All MQTT QoS levels (0, 1, 2)
- **Persistent Sessions**: Optional to avoid resubscribing after reconnection

### Clean Architecture

```
┌─────────────────────┐
│   App (ViewModel)   │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  MqttRepository     │  ← Data Layer (service binding)
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│   MqttService       │  ← UI Layer (foreground service)
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  HiveMqttClient     │  ← Data Layer (MQTT implementation)
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  Domain Models      │  ← Domain Layer (pure Kotlin)
└─────────────────────┘
```

**Layer Responsibilities:**

- **Data**: MQTT communication, network monitoring, repository
- **Domain**: Business models (MqttConfig, MqttMessage, MqttConnectionState)
- **UI**: Android service, system integration

### Modern Android Stack

- **Kotlin Coroutines**: Async/await patterns
- **Flow & StateFlow**: Reactive streams for messages and state
- **Jetpack Compose**: Modern declarative UI
- **Koin**: Dependency injection
- **HiveMQ Client**: Modern MQTT 3.1.1 and 5.0 support

## Features

- **SSL/TLS Encrypted Connections** - Secure MQTT over TLS using trusted CA certificates
- **Foreground Service** - Maintains persistent MQTT connection in background
- **Battery Optimized** - Smart wake lock management and adaptive keep-alive intervals
- **Network Resilience** - Automatic reconnection with exponential backoff
- **Clean Architecture** - Repository pattern with dependency injection (Koin)
- **Jetpack Compose UI** - Modern declarative UI with Material 3
- **Persistent Client ID** - UUID-based client identification using DataStore

## SSL/TLS Configuration

The app successfully connects to `broker.hivemq.com:8883` using SSL/TLS encryption with a **trusted
CA-signed certificate**.

**Key features:**
- Uses Android's system trust store for certificate validation
- Works with trusted certificates signed by well-known CAs
- Proper SSL handshake timeout (30 seconds)
- Detailed error logging for troubleshooting

## Documentation

- **[MQTT Integration Guide](docs/mqtt/README.md)** - Complete MQTT setup, SSL/TLS configuration,
  troubleshooting, and production deployment
- [Authentication Setup](docs/authentication/README.md) - Broker authentication and test credentials
- [Koin Dependency Injection Guide](docs/koin/README.md) - Koin DI implementation

## Quick Start

```bash
# Clone and open in Android Studio
cd android

# Run on device/emulator
./gradlew :app:installDebug

# Or click Run in Android Studio
```

### Test Connection

1. Launch the app
2. Click "Connect" (default broker: `ssl://broker.hivemq.com:8883`)
3. Once connected, subscribe to topic: `demo/messages`
4. Publish a message to see it appear in the message list

### Broker Settings

Edit in `MqttViewModel.kt`:

```kotlin
val brokerUrl = mutableStateOf("ssl://broker.hivemq.com:8883")
val username = mutableStateOf("") // Optional
val password = mutableStateOf("") // Optional
```

### Connection Parameters

Edit in `MqttConfig`:

```kotlin
val config = MqttConfig(
    brokerUrl = "ssl://broker.hivemq.com:8883",
    clientId = "android_client_123",
    username = "your_username",
    password = "your_password",
    keepAliveInterval = 240, // seconds
    connectionTimeout = 30,  // seconds
    qos = 1
)
```

## Integrating the MQTT Module

The MQTT module is designed to be reusable in any Android project:

#### 1. Add the Module

```kotlin
// settings.gradle.kts
include(":app", ":mqtt")

// app/build.gradle.kts
dependencies {
    implementation(project(":mqtt"))
}
```

#### 2. Setup Dependency Injection (Koin)

```kotlin
// In your Application class
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            modules(
                module {
                    single { MqttRepository(androidContext()) }
                    viewModel { MqttViewModel(get()) }
                }
            )
        }
    }
}
```

#### 3. Use in ViewModel

```kotlin
class MqttViewModel(private val mqttRepository: MqttRepository) : ViewModel() {
    
    init {
        // Start the MQTT service
        viewModelScope.launch {
            mqttRepository.startService()
        }
        
        // Observe connection state
        viewModelScope.launch {
            mqttRepository.getConnectionState()?.collect { state ->
                when (state) {
                    is MqttConnectionState.Connected -> {
                        // Update UI - connected
                    }
                    is MqttConnectionState.Error -> {
                        // Handle error
                    }
                    is MqttConnectionState.Reconnecting -> {
                        // Show reconnecting status
                    }
                    // ... other states
                }
            }
        }
    }
    
    fun connect(brokerUrl: String) {
        val config = MqttConfig.batteryOptimized(
            brokerUrl = brokerUrl,
            clientId = "android_${UUID.randomUUID()}"
        )
        mqttRepository.connect(config)
    }
    
    fun subscribe(topic: String) {
        viewModelScope.launch {
            mqttRepository.subscribe(topic, qos = 1)?.collect { message ->
                // Handle received message
                println("Received: ${message.payloadAsString()}")
            }
        }
    }
    
    fun publish(topic: String, payload: String) {
        viewModelScope.launch {
            val message = MqttMessage(topic, payload, qos = 1)
            mqttRepository.publish(message).onFailure { error ->
                // Handle publish failure
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        mqttRepository.disconnect()
        mqttRepository.stopService()
    }
}
```

## Configuration Presets

### Battery-Optimized (Default)

```kotlin
val config = MqttConfig.batteryOptimized(
    brokerUrl = "ssl://broker.hivemq.com:8883"
)
// Uses: 8-minute keep-alive, persistent sessions, QoS 1
```

### Low-Latency

```kotlin
val config = MqttConfig.lowLatency(
    brokerUrl = "ssl://broker.hivemq.com:8883"
)
// Uses: 1-minute keep-alive, clean sessions, QoS 1
```

### Custom

```kotlin
val config = MqttConfig(
    brokerUrl = "ssl://broker.hivemq.com:8883",
    clientId = "android_client_123",
    username = "your_username",
    password = "your_password",
    keepAliveInterval = 240, // seconds
    cleanSession = false,
    qos = 1
)
```

## Battery Consumption Data

Based on research and testing:

| Keep-Alive   | Battery Drain (3G) | Use Case                |
|--------------|--------------------|-------------------------|
| 60s          | ~0.8% per hour     | Real-time messaging     |
| 240s (4 min) | **~0.16%/hour**    | **Balanced (Default)**  |
| 480s (8 min) | ~0.08% per hour    | Maximum battery savings |

## Architecture Benefits

### Clean Architecture

- **Testability**: Domain layer has zero Android dependencies
- **Maintainability**: Clear boundaries between layers
- **Flexibility**: Easy to swap MQTT implementations

### Repository Pattern

- **Abstraction**: Clean API for the app layer
- **Service Binding**: Handles Android service lifecycle
- **Coordination**: Manages multiple data sources

### Foreground Service

- **Persistence**: Connection survives app backgrounding
- **Doze Mode**: Exempt from battery optimization restrictions
- **Reliability**: START_STICKY ensures automatic restart

## Testing

### Quick Test with Public Broker

```kotlin
// HiveMQ Public Broker
val config = MqttConfig(
    brokerUrl = "ssl://broker.hivemq.com:8883"
)

// Test.mosquitto.org (Another popular test broker)
val config = MqttConfig(
    brokerUrl = "ssl://test.mosquitto.org:8886"
)

// With SSL/TLS
val config = MqttConfig(
    brokerUrl = "ssl://broker.hivemq.com:8883"
)
```

### Monitor Battery Usage

```bash
# Reset battery stats
adb shell dumpsys batterystats --reset

# Use the app for a few hours

# Dump battery stats
adb shell dumpsys batterystats > battery_stats.txt

# Analyze with Android Studio Profiler (recommended)
# Tools → Profiler → Energy
# Or use command line:
adb shell dumpsys battery
```

**Modern Tools:**

- **Android Studio Energy Profiler** - Built-in, real-time monitoring
- **Perfetto** - Advanced system tracing (https://perfetto.dev)
- **adb shell dumpsys batterystats** - Command-line analysis

## Build

```bash
# Build MQTT module
./gradlew :mqtt:build

# Build demo app
./gradlew :app:assembleDebug

# Run tests
./gradlew test

# Install on device
./gradlew :app:installDebug
```

## Requirements

- **Android API 24+** (Android 7.0 Nougat and above)
- **Kotlin 2.2.21**
- **AGP 8.13.1**
- **HiveMQ MQTT Client 1.3.10**

## Permissions

The module automatically includes these permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**Note**: On Android 13+, you must request `POST_NOTIFICATIONS` permission at runtime.

## Mobile MQTT Challenges Solved

| Challenge               | Solution                                                            |
|-------------------------|---------------------------------------------------------------------|
| **Battery Drain**       | Optimized keep-alive (4-8 min), smart WakeLock, exponential backoff |
| **Background Killing**  | Foreground service with START_STICKY and notification               |
| **Network Switching**   | NetworkMonitor with automatic reconnection on WiFi ↔ Mobile         |
| **Doze Mode**           | Foreground service exemption from Doze restrictions                 |
| **Reconnection Storms** | Exponential backoff (1s → 2s → 4s → 8s → ... → 120s max)            |
| **Message Loss**        | QoS 1/2 support with persistent sessions                            |
| **Thread Management**   | Kotlin Coroutines with proper scope management                      |
| **Memory Leaks**        | Lifecycle-aware components and proper cleanup                       |

## Documentation

**[mqtt/README.md](mqtt/README.md)** - Comprehensive module documentation

Includes:

- Complete API reference
- Battery optimization details
- Architecture deep dive
- HiveMQ client comparison
- Troubleshooting guide
- Best practices

## Demo App Features

The included demo app showcases:

- Connect/disconnect to MQTT broker
- Subscribe to topics with wildcard support
- Publish messages with QoS selection
- Real-time connection state display
- Message history with timestamps
- Battery-optimized configuration
- Jetpack Compose UI

## Contributing

This is a demo/reference project. Feel free to:

1. Use the MQTT module in your own projects
2. Modify it to suit your needs
3. Report issues or suggest improvements
4. Share your experiences

## License

- **Project Code**: MIT License (modify as needed)
- **HiveMQ MQTT Client**: Apache License 2.0
- **Kotlin Coroutines**: Apache License 2.0

## References

- [MQTT 3.1.1 Specification](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/mqtt-v3.1.1.html)
- [HiveMQ MQTT Client](https://github.com/hivemq/hivemq-mqtt-client)
- [Android Foreground Services](https://developer.android.com/guide/components/foreground-services)
- [Android Battery Optimization](https://developer.android.com/topic/performance/power)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

## Credits

Built with care for the Android community, demonstrating production-ready MQTT implementation with
modern Android architecture.
