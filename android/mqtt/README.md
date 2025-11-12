# MQTT Module for Android

A battery-optimized, production-ready MQTT client module for Android applications.

## Features

### Battery Optimization

- **Adaptive Keep-Alive**: Configurable keep-alive intervals (default: 240s for battery efficiency)
- **Smart WakeLock Management**: Only acquires WakeLock during critical operations (connection,
  reconnection)
- **Exponential Backoff**: Intelligent reconnection strategy to avoid battery drain
- **Network-Aware**: Automatically detects network changes and adjusts behavior

### Reliability

- **Foreground Service**: Ensures connection persistence even when app is in background
- **Automatic Reconnection**: Handles network switches (WiFi ↔ Mobile Data)
- **QoS Support**: All MQTT QoS levels (0, 1, 2) supported
- **Persistent Sessions**: Optional session persistence to avoid resubscribing
- **Message Queuing**: Built-in message queuing for offline scenarios

### Architecture

- **Clean Architecture**: Separated into client, service, and repository layers
- **Kotlin Coroutines**: Modern asynchronous programming
- **StateFlow**: Reactive connection state management
- **Dependency Injection**: Koin-ready for easy integration

### Security

- **TLS/SSL Support**: Secure connections out of the box
- **Authentication**: Username/password authentication
- **Certificate Support**: Custom certificate support (via HiveMQ client)

## HiveMQ MQTT Client Library

This module uses the [HiveMQ MQTT Client](https://github.com/hivemq/hivemq-mqtt-client) as the
underlying MQTT implementation.

### Benefits

**Modern Architecture**

- Fully reactive API with backpressure support
- Built on Netty for high performance
- Clean, fluent API design
- Supports both MQTT 3.1.1 and MQTT 5.0

**Production Ready**

- Actively maintained by HiveMQ (commercial MQTT broker vendor)
- Well-tested and used in production by many companies
- Comprehensive documentation and examples
- Regular updates and security patches

**Feature Rich**

- Automatic reconnection with exponential backoff (built-in)
- Manual acknowledgment for fine-grained control
- Topic alias mapping (MQTT 5)
- Enhanced authentication support (MQTT 5)
- Request/response pattern support (MQTT 5)

**Performance**

- Efficient message handling with minimal overhead
- Connection pooling and reuse
- Optimized for high-throughput scenarios
- Low memory footprint

**Android Support**

- Works on Android API 21+ (with our module targeting API 24+)
- No special configuration needed for Android
- Handles Android's network switching gracefully
- Works well with Android's lifecycle

### Limitations

**Library Size**

- Relatively large dependency (~2-3 MB) due to Netty
- Includes Netty's event loop and channel implementations
- Impact: Increases APK size by approximately 2-3 MB

**Complexity**

- More complex than some alternative MQTT client libraries
- Steeper learning curve for advanced features
- May be overkill for simple use cases
- Impact: Our module abstracts this complexity away

**MQTT 5.0 Focus**

- Primary focus is on MQTT 5.0 features
- MQTT 3.1.1 support is complete but secondary
- Impact: None for our use case (we support both)

**Dependency Chain**

- Brings in Netty as a transitive dependency
- Requires exclusion of conflicting dependencies in some cases
- May conflict with other networking libraries
- Impact: We handle this in our build configuration

**Threading Model**

- Uses Netty's event loop threads
- Requires understanding of Netty's threading model for advanced use
- Impact: Our module handles threading concerns

### Alternatives Considered

**Mosquitto Client Library**

- Pros: Lightweight, C-based with JNI wrapper
- Cons: Requires NDK, less idiomatic for Android/Kotlin
- Why not chosen: Harder to integrate and maintain

**Moquette**

- Pros: Small, pure Java
- Cons: Primarily a broker implementation, client support is secondary
- Why not chosen: Not designed as a client library

**AWS IoT SDK**

- Pros: AWS integration, WebSocket support
- Cons: AWS-specific, large size, complex setup
- Why not chosen: Too AWS-specific, not general-purpose

### Migration Path

If you need to switch to a different MQTT client library in the future:

1. Only the `HiveMqttClient` class needs to be replaced
2. The `MqttClient` interface remains the same
3. Repository, Service, and Model layers are unaffected
4. No changes needed in the app layer

This modular design ensures flexibility and prevents vendor lock-in.

## Architecture Overview

```
┌─────────────────────────────────────────┐
│           App Layer (UI)                │
│  Activities, ViewModels, Compose        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      MqttRepository                     │
│  Service binding & lifecycle management │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      MqttService (Foreground)           │
│  • Network monitoring                   │
│  • WakeLock management                  │
│  • Reconnection logic                   │
│  • Notification management              │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      MqttClient (Interface)             │
│      HiveMqttClient (Implementation)    │
│  • Connect/Disconnect                   │
│  • Publish/Subscribe                    │
│  • State management                     │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      HiveMQ MQTT Client Library         │
└─────────────────────────────────────────┘
```

### Clean Architecture Structure

This module follows **Clean Architecture** principles with clear separation of concerns:

```
com.mqtt.core/
├── data/                           # Data Layer
│   ├── datasource/                 # Data sources (network, local, etc.)
│   │   ├── MqttClient.kt          # MQTT client interface
│   │   └── HiveMqttClient.kt      # HiveMQ implementation
│   ├── repository/                 # Repository implementations
│   │   └── MqttRepository.kt      # MQTT repository (service binding)
│   └── util/                       # Data layer utilities
│       └── NetworkMonitor.kt      # Network connectivity monitoring
│
├── domain/                         # Domain Layer
│   └── model/                      # Domain models/entities
│       ├── MqttConfig.kt          # MQTT configuration model
│       ├── MqttConnectionState.kt # Connection state sealed class
│       └── MqttMessage.kt         # MQTT message model
│
└── ui/                             # UI/Presentation Layer
    └── service/                    # Android services
        └── MqttService.kt         # Foreground service for MQTT
```

**Layer Responsibilities:**

- **Data Layer**: Handles all data operations and external communication (MQTT broker, network
  monitoring)
- **Domain Layer**: Pure business logic and models with no Android dependencies
- **UI Layer**: Android-specific components (services, activities, fragments)

**Dependency Flow:**

```
UI → Data → Domain
```

Dependencies point inward - the Domain layer has no dependencies on outer layers.

**Benefits:**

- **Separation of Concerns**: Each layer has a single, well-defined responsibility
- **Testability**: Pure domain logic can be tested without Android dependencies
- **Maintainability**: Changes in one layer don't cascade through the entire codebase
- **Flexibility**: Easy to swap implementations (e.g., replace HiveMQ with another MQTT library)
- **Scalability**: Clear structure makes it easier to add new features

## Installation

### 1. Add Module to Your Project

In your `settings.gradle.kts`:

```kotlin
include(":app", ":mqtt")
```

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":mqtt"))
}
```

### 2. Configure Permissions

The module already includes necessary permissions in its manifest:

- `INTERNET` - For network communication
- `ACCESS_NETWORK_STATE` - For network monitoring
- `FOREGROUND_SERVICE` - For persistent connection
- `FOREGROUND_SERVICE_DATA_SYNC` - Android 14+ requirement
- `WAKE_LOCK` - For reliable message delivery
- `POST_NOTIFICATIONS` - Android 13+ for foreground service notification

## Usage

### Basic Usage

```kotlin
// 1. Create repository
val mqttRepository = MqttRepository(context)

// 2. Start service
lifecycleScope.launch {
    mqttRepository.startService()
}

// 3. Create config
val config = MqttConfig(
    brokerUrl = "tcp://broker.hivemq.com:1883",
    clientId = "android_client_123",
    qos = 1
)

// 4. Connect
mqttRepository.connect(config)

// 5. Observe connection state
mqttRepository.getConnectionState()?.collect { state ->
    when (state) {
        is MqttConnectionState.Connected -> {
            // Connected successfully
        }
        is MqttConnectionState.Error -> {
            // Handle error
        }
        // ... other states
    }
}

// 6. Publish message
lifecycleScope.launch {
    val message = MqttMessage(
        topic = "test/topic",
        payload = "Hello MQTT!",
        qos = 1
    )
    mqttRepository.publish(message)
}

// 7. Subscribe to topic
mqttRepository.subscribe("test/topic", qos = 1)?.collect { message ->
    val payload = message.payloadAsString()
    Log.d("MQTT", "Received: $payload")
}

// 8. Disconnect and cleanup
mqttRepository.disconnect()
mqttRepository.stopService()
```

### Battery-Optimized Configuration

```kotlin
val config = MqttConfig.batteryOptimized(
    brokerUrl = "tcp://broker.hivemq.com:1883",
    clientId = "android_client_123",
    username = "user",
    password = "pass"
)
// This uses:
// - 8 minute keep-alive
// - Persistent sessions
// - QoS 1
```

### Low-Latency Configuration

```kotlin
val config = MqttConfig.lowLatency(
    brokerUrl = "tcp://broker.hivemq.com:1883"
)
// This uses:
// - 1 minute keep-alive
// - Clean sessions
// - QoS 1
```

### SSL/TLS Connection

```kotlin
val config = MqttConfig(
    brokerUrl = "ssl://broker.hivemq.com:8883", // Use ssl:// or mqtts://
    username = "user",
    password = "pass"
)
```

## Architecture Overview

```
┌─────────────────────────────────────────┐
│           App Layer (UI)                │
│  Activities, ViewModels, Compose        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      MqttRepository                     │
│  Service binding & lifecycle management │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      MqttService (Foreground)           │
│  • Network monitoring                   │
│  • WakeLock management                  │
│  • Reconnection logic                   │
│  • Notification management              │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      MqttClient (Interface)             │
│      HiveMqttClient (Implementation)    │
│  • Connect/Disconnect                   │
│  • Publish/Subscribe                    │
│  • State management                     │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      HiveMQ MQTT Client Library         │
└─────────────────────────────────────────┘
```

## Battery Optimization Details

### Keep-Alive Intervals

Research shows battery consumption based on keep-alive:

- 60s: ~0.8% per hour on 3G
- 240s (4 min): ~0.16% per hour (Default)
- 480s (8 min): ~0.08% per hour (Battery-optimized mode)

### WakeLock Strategy

- **Partial WakeLock** only during:
    - Connection establishment
    - Reconnection attempts
- **Timeout**: 60 seconds max
- **Automatic release** after operation completes

### Network Switching

- Detects WiFi ↔ Mobile Data switches
- Handles reconnection gracefully
- No reconnection storms (exponential backoff)

## Handling Android Doze Mode

The foreground service ensures the MQTT connection persists even when the device enters Doze mode.
The service uses:

- **Foreground notification**: Keeps service alive
- **START_STICKY**: Automatic restart if killed
- **Network callbacks**: Immediate reconnection on network changes

## Testing

### Public Test Brokers

```kotlin
// HiveMQ Public Broker (No authentication)
val config = MqttConfig(
    brokerUrl = "tcp://broker.hivemq.com:1883"
)

// Test.mosquitto.org (Another popular test broker)
val config = MqttConfig(
  brokerUrl = "tcp://test.mosquitto.org:1883"
)

// With SSL/TLS
val config = MqttConfig(
  brokerUrl = "ssl://broker.hivemq.com:8883"
)
```

### Monitor Battery Usage

```bash
# Check battery stats
adb shell dumpsys batterystats --reset
# Use your app for a while
adb shell dumpsys batterystats > battery.txt
```

## Best Practices

1. **Use QoS 1 as default** - Good balance between reliability and performance
2. **Enable persistent sessions** - Avoids resubscribing after reconnection
3. **Keep message payloads small** - Reduces bandwidth and battery usage
4. **Use topic wildcards wisely** - `home/+/temperature` instead of multiple subscriptions
5. **Compress large payloads** - If you must send large data
6. **Monitor connection state** - Update UI accordingly
7. **Handle permissions properly** - Request notification permission on Android 13+

## Troubleshooting

### Connection Keeps Dropping

- Check keep-alive interval (might be too short/long)
- Verify network stability
- Check firewall/NAT settings
- Try increasing keep-alive: `keepAliveInterval = 480`

### High Battery Drain

- Reduce keep-alive frequency
- Use QoS 0 for non-critical messages
- Check for message loops
- Use Android Studio Energy Profiler or Perfetto for detailed analysis

### Service Gets Killed

- Ensure foreground service is running
- Check manufacturer battery optimization
- Verify START_STICKY is returned
- Consider using WorkManager for critical operations

## License

This module uses:

- **HiveMQ MQTT Client**: Apache License 2.0
- **Kotlin Coroutines**: Apache License 2.0

## References

- [MQTT Specification](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/mqtt-v3.1.1.html)
- [HiveMQ Client Documentation](https://github.com/hivemq/hivemq-mqtt-client)
- [Android Foreground Services](https://developer.android.com/guide/components/foreground-services)
- [Android Battery Optimization](https://developer.android.com/topic/performance/power)
