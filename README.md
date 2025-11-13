# MQTT Android Demo

> **Production-ready MQTT messaging between Android, Web, and Server with battery-optimized
implementation**

A complete demonstration of MQTT messaging with three components:

- **MQTT Broker Server** (Node.js + Aedes)
- **Android App** (Kotlin + Jetpack Compose + HiveMQ Client + Clean Architecture)
- **Web App** (React + MQTT.js)

All clients can send and receive messages in real-time through the MQTT broker!

## Highlights

- **Clean Architecture** - Data/Domain/UI layers with clear separation
- **Battery Optimized** - Research-backed keep-alive intervals (~0.16% drain/hour)
- **Production Ready** - Foreground service, automatic reconnection, network monitoring
- **Modern Android** - Kotlin Coroutines, Flow/StateFlow, Jetpack Compose UI
- **Testable** - Repository pattern with dependency injection (Koin)
- **Cross-Platform** - Android, Web, and any MQTT client can communicate

## Quick Start

**Want to get started immediately?** See the steps below or check component-specific READMEs.

### 1. Start the MQTT Broker

```bash
cd server
npm install
npm start
```

The broker runs on:

- **MQTT TCP**: `localhost:1883`
- **MQTT WebSocket**: `ws://localhost:8080`
- **HTTP API**: `http://localhost:3001`

### 2. Run the Android App

```bash
cd android
./gradlew installDebug
# Or open in Android Studio and click Run 
```

**Broker URLs:**

- **Emulator**: `tcp://10.0.2.2:1883` (maps to host's localhost)
- **Physical Device**: `tcp://YOUR_COMPUTER_IP:1883`

### 3. Run the Web App (Optional)

```bash
cd web
npm install
npm run dev
```

Open browser to `http://localhost:5173`

## Project Structure

```
mqtt-aosp-demo/
├── server/                       # MQTT Broker (Node.js + Aedes)
│   ├── index.js                 # Server with MQTT, WebSocket, HTTP
│   └── package.json
│
├── android/                      # Android App (Clean Architecture)
│   ├── app/                     # Demo application
│   │   └── src/main/java/com/push/notifications/via/mqtt/
│   │       ├── MainActivity.kt          # Jetpack Compose UI
│   │       ├── MqttViewModel.kt         # UI state management
│   │       ├── MqttApplication.kt       # App initialization (Koin)
│   │       └── di/AppModule.kt          # Dependency injection
│   │
│   └── mqtt/                    # Reusable MQTT Module 
│       └── src/main/java/com/mqtt/core/
│           ├── data/            # Data Layer
│           │   ├── datasource/  # MQTT client (HiveMQ)
│           │   ├── repository/  # Repository pattern
│           │   └── util/        # Network monitoring
│           ├── domain/          # Domain Layer (Pure Kotlin)
│           │   └── model/       # Business models
│           └── ui/              # UI Layer (Android-specific)
│               └── service/     # Foreground service
│
├── web/                         # Web App (React + Vite)
│   └── src/
│
├── docs/                        # Documentation
│   ├── mqtt/                    # MQTT implementation guide
│   └── koin/                    # Koin dependency injection guide
│
└── README.md                    # This file
```

## Features

### Android App

**Modern Architecture:**

- **Clean Architecture** - Data, Domain, and UI layers
- **Repository Pattern** - Clean API abstraction
- **Foreground Service** - Survives backgrounding and Doze mode
- **Dependency Injection** - Koin for testability

**Battery Optimization:**

- **Adaptive Keep-Alive** - 4-8 minute intervals (research-backed)
- **Smart WakeLock** - Only during critical operations (60s timeout)
- **Network-Aware** - Automatic reconnection on network switches
- **Exponential Backoff** - Prevents reconnection storms
- **Battery Drain** - ~0.16% per hour with default settings

**UI/UX Features:**

- **Material 3 Design** with Jetpack Compose
- **Real-time Messaging** - Reactive Flow-based updates
- **Connection Status** - Visual indicator with state
- **Message History** - Sent/received with timestamps
- **QoS Support** - All MQTT QoS levels (0, 1, 2)
- **Topic Management** - Subscribe and publish to topics

**Reliability:**

- **Automatic Reconnection** - Smart retry with exponential backoff
- **Persistent Sessions** - Avoids resubscribing
- **START_STICKY** - Automatic restart if killed
- **Network Monitoring** - Handles WiFi Mobile Data switches

### MQTT Broker (Server)

- **TCP MQTT** on port 1883 (for native clients)
- **WebSocket MQTT** on port 8080 (for web clients)
- **HTTP REST API** on port 3001
- **Event Logging** - Connections, subscriptions, messages
- **CORS Enabled** - For web clients
- **Low Latency** - < 100ms on local network

### Web App

- **Browser-based MQTT client**
- **Subscribe and publish** to topics
- **Real-time messaging** with Android and other clients
- **Modern React UI** with Tailwind CSS

## Technology Stack

### Android

| Component        | Technology               | Version        |
|------------------|--------------------------|----------------|
| **Language**     | Kotlin                   | 2.2.21         |
| **UI Framework** | Jetpack Compose          | Material 3     |
| **MQTT Client**  | HiveMQ MQTT Client       | 1.3.10         |
| **DI**           | Koin                     | 4.1.1          |
| **Async**        | Kotlin Coroutines + Flow | -              |
| **Architecture** | Clean Architecture       | Data/Domain/UI |
| **Min SDK**      | Android 7.0 (API 24)     | -              |
| **Target SDK**   | Android 15 (API 36)      | -              |

**Key Libraries:**

- `HiveMQ MQTT Client` - Modern, reactive MQTT 3.1.1 & 5.0 support
- `Jetpack Compose` - Declarative UI with Material 3
- `Koin` - Lightweight dependency injection
- `Kotlin Coroutines` - Structured concurrency
- `StateFlow/Flow` - Reactive state management

### Server

- **Runtime**: Node.js
- **MQTT Broker**: Aedes (lightweight, fast)
- **Web Server**: Express
- **WebSocket**: ws library

### Web

- **Framework**: React 18
- **Build Tool**: Vite
- **MQTT Client**: MQTT.js
- **Styling**: Tailwind CSS

## Android Module Architecture

The Android MQTT module follows **Clean Architecture** principles:

```
┌─────────────────────┐
│   App (ViewModel)   │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  MqttRepository     │  Data Layer (service binding)
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│   MqttService       │  UI Layer (foreground service)
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  HiveMqttClient     │  Data Layer (MQTT implementation)
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  Domain Models      │  Domain Layer (pure Kotlin)
└─────────────────────┘
```

**Layer Responsibilities:**

- **Data**: MQTT communication, network monitoring, repository
- **Domain**: Business models (MqttConfig, MqttMessage, MqttConnectionState)
- **UI**: Android service, foreground service lifecycle

**Benefits:**

- **Testability**: Domain layer has zero Android dependencies
- **Maintainability**: Clear boundaries between layers
- **Flexibility**: Easy to swap MQTT implementations
- **Reusability**: MQTT module can be used in any Android project

## Battery Consumption

Based on research and testing:

| Keep-Alive Interval | Battery Drain (3G) | Use Case                |
|---------------------|--------------------|-------------------------|
| 60s                 | ~0.8% per hour     | Real-time messaging     |
| 240s (4 min)        | **~0.16%/hour**    | **Balanced (Default)**  |
| 480s (8 min)        | ~0.08% per hour    | Maximum battery savings |

**Configuration Presets:**

```kotlin
// Battery-Optimized (8-minute keep-alive)
MqttConfig.batteryOptimized(brokerUrl)

// Low-Latency (1-minute keep-alive)
MqttConfig.lowLatency(brokerUrl)

// Custom
MqttConfig(
    brokerUrl = "tcp://broker.example.com:1883",
    keepAliveInterval = 240,  // 4 minutes
    cleanSession = false,     // Persistent sessions
    qos = 1                   // At least once delivery
)
```

## Documentation

### Main Documentation

- **[android/README.md](android/README.md)** - Complete Android app & module documentation
- **[android/mqtt/README.md](android/mqtt/README.md)** - MQTT module API reference

### Guides

- **[docs/mqtt/README.md](docs/mqtt/README.md)** - MQTT implementation guide
- **[docs/koin/README.md](docs/koin/README.md)** - Koin dependency injection guide

## Use Cases

This project demonstrates:

- **IoT Communication** - Sensor data collection and control
- **Real-time Messaging** - Chat and notifications
- **Cross-Platform** - Android, Web, and other MQTT clients
- **Production Patterns** - Clean architecture, DI, state management
- **Battery-Efficient Background Tasks** - Foreground service patterns
- **Network Resilience** - Automatic reconnection and error handling

## Configuration

### Android Emulator

Use `tcp://10.0.2.2:1883` (maps to host's localhost)

### Physical Android Device

1. Find your computer's IP:
   ```bash
   # macOS/Linux
   ifconfig | grep "inet "
   
   # Windows
   ipconfig
   ```

2. Update broker URL in app: `tcp://YOUR_IP:1883`

3. Ensure both devices are on the same WiFi network

### Web Browser

Connects via WebSocket: `ws://localhost:8080`

## Testing

### Basic Test Flow

1. **Start the broker** → See "Ready to receive messages!"
2. **Launch Android app** → Tap "Connect" → See green indicator
3. **Send message from Android** → Appears in message list
4. **Open web app** → Subscribe to same topic
5. **Send from web** → Message appears in Android app instantly
6. **Send from Android** → Message appears in web app instantly

### Test Topics

Default topic: `demo/messages`

Try these patterns:

```
sensors/temperature          # Simple topic
home/livingroom/light       # Multi-level
notifications/+             # Single-level wildcard
devices/#                   # Multi-level wildcard
```

### MQTT QoS Levels

The demo supports all QoS levels:

- **QoS 0** (At most once) - Fire and forget
- **QoS 1** (At least once) - Guaranteed delivery (default)
- **QoS 2** (Exactly once) - Guaranteed, no duplicates

## Troubleshooting

### Android Can't Connect

**Problem**: "Connection failed" or "Disconnected"

**Solutions:**
- **Emulator**: Use `10.0.2.2` not `localhost`
- **Physical Device**:
    - Verify same WiFi network
    - Use computer's IP address (not 127.0.0.1)
    - Check firewall allows port 1883
- **Server**: Ensure broker is running (`npm start` in server folder)

### Build Errors

```bash
cd android
./gradlew clean
./gradlew build
```

### Server Port Already in Use

```bash
# Find process using port
lsof -i :1883          # macOS/Linux
netstat -ano | findstr :1883  # Windows

# Kill process and restart server
```

### Messages Not Appearing

- Check topic names match (case-sensitive)
- Verify QoS level is appropriate
- Check connection status (green indicator)
- Look at Logcat for errors

## Advanced Usage

### Custom Configuration

```kotlin
val config = MqttConfig(
    brokerUrl = "ssl://broker.example.com:8883",
    username = "user",
    password = "pass",
    keepAliveInterval = 240,  // 4 minutes
    cleanSession = false,     // Persistent session
    qos = 1,                  // At least once
    automaticReconnect = true
)
mqttRepository.connect(config)
```

### Subscribe with Wildcards

```kotlin
// Single-level wildcard
mqttRepository.subscribe("sensors/+/temperature", qos = 1)

// Multi-level wildcard
mqttRepository.subscribe("home/#", qos = 1)
```

### Persistent Sessions

```kotlin
// Avoid resubscribing on reconnect
MqttConfig(
    cleanSession = false,  // Saves battery and time
    // Messages are queued by broker while disconnected
)
```

### SSL/TLS Connection

```kotlin
MqttConfig(
    brokerUrl = "ssl://broker.example.com:8883",  // Use ssl:// or mqtts://
    username = "user",
    password = "pass"
)
```

## Performance Metrics

| Metric                | Value                             |
|-----------------------|-----------------------------------|
| **Message Latency**   | < 100ms (local network)           |
| **APK Size**          | ~15MB (debug), ~8MB (release)     |
| **Memory Usage**      | ~50-80MB (Android app running)    |
| **Battery Impact**    | ~0.16% per hour (default config)  |
| **Reconnection Time** | < 5 seconds (exponential backoff) |
| **Network Overhead**  | ~4 bytes per keep-alive ping      |

## Mobile MQTT Challenges Solved

| Challenge               | Solution in This Project                                            |
|-------------------------|---------------------------------------------------------------------|
| **Battery Drain**       | Optimized keep-alive (4-8 min), smart WakeLock, exponential backoff |
| **Background Killing**  | Foreground service with START_STICKY and notification               |
| **Network Switching**   | NetworkMonitor with automatic reconnection on WiFi Mobile           |
| **Doze Mode**           | Foreground service exemption from Doze restrictions                 |
| **Reconnection Storms** | Exponential backoff (1s → 2s → 4s → ... → 120s max)                 |
| **Message Loss**        | QoS 1/2 support with persistent sessions                            |
| **Thread Management**   | Kotlin Coroutines with proper scope management                      |
| **Memory Leaks**        | Lifecycle-aware components and proper cleanup                       |

## Security Notes

This is a demo project. For production deployment:

### Required Security Measures:

1. **Enable TLS/SSL** encryption
   ```kotlin
   MqttConfig(brokerUrl = "ssl://broker.example.com:8883")
   ```

2. **Implement Authentication**
   ```kotlin
   MqttConfig(
       brokerUrl = "ssl://...",
       username = "secure_user",
       password = "strong_password"
   )
   ```

3. **Use ACLs** (Access Control Lists) on the broker

4. **Validate Messages** - Sanitize all incoming data

5. **Implement Rate Limiting** - Prevent message flooding

6. **Use Certificate Pinning** - For mobile apps

7. **Secure WebSocket** - Use `wss://` instead of `ws://`

## Contributing

This is a demo/reference project. Feel free to:

1. Use the MQTT module in your own projects
2. Report issues or suggest improvements
3. Fork and extend for your use cases
4. Share your experiences and feedback

## License

- **Project Code**: MIT License (modify as needed)
- **HiveMQ MQTT Client**: Apache License 2.0
- **Kotlin Coroutines**: Apache License 2.0

See [LICENSE](LICENSE) file for details.

## Learning Resources

### MQTT Protocol

- [MQTT 3.1.1 Specification](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/mqtt-v3.1.1.html)
- [MQTT.org](https://mqtt.org/) - Official documentation
- [HiveMQ MQTT Essentials](https://www.hivemq.com/mqtt-essentials/) - Tutorial series

### Android Development
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - Async programming
- [Android Architecture Guide](https://developer.android.com/topic/architecture) - Best practices

### MQTT Clients

- [HiveMQ MQTT Client](https://github.com/hivemq/hivemq-mqtt-client) - Java/Android client
- [MQTT.js](https://github.com/mqttjs/MQTT.js) - JavaScript/Node.js client

### Server
- [Aedes](https://github.com/moscajs/aedes) - MQTT broker for Node.js
- [Express](https://expressjs.com/) - Web framework

## Why This Project Stands Out

### 1. Production-Ready Architecture

- **Clean Architecture** with clear layer separation
- **Repository pattern** for data abstraction
- **Dependency injection** for testability
- **Proper error handling** and state management

### 2. Battery Optimization

- **Research-backed keep-alive intervals**
- **Smart WakeLock management** (only when needed)
- **Network-aware reconnection strategy**
- **Foreground service** for reliability

### 3. Modern Tech Stack

- **Latest Kotlin (2.2.21)** and Compose
- **HiveMQ client** (better than legacy alternatives)
- **Kotlin Coroutines** and Flow
- **Material 3 design**

### 4. Complete Solution

- **Working MQTT broker included**
- **Android and Web clients**
- **Cross-platform communication demo**
- **Multiple configuration examples**

### 5. Comprehensive Documentation

- **Detailed READMEs** for each component
- **Architecture explanations**
- **Battery optimization guide**
- **Troubleshooting sections**
- **Code is well-commented**

### 6. Real-World Patterns

- **Handles network switches gracefully**
- **Survives Doze mode and app backgrounding**
- **Automatic reconnection with backoff**
- **Persistent sessions for efficiency**

## Development

### Build Android APK

```bash
cd android
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Run Android Tests

```bash
cd android
./gradlew test
./gradlew connectedAndroidTest
```

### Analyze Battery Usage

```bash
# Reset battery stats
adb shell dumpsys batterystats --reset

# Use the app for a few hours

# Dump stats
adb shell dumpsys batterystats > battery_stats.txt

# Use Android Studio Energy Profiler
# Tools → Profiler → Energy
```

### Clean Build

```bash
cd android
./gradlew clean build
```

## Status

| Component     | Status   | Notes                                       |
|---------------|----------|---------------------------------------------|
| MQTT Broker   | Complete | Aedes on Node.js                            |
| Android App   | Complete | Clean Architecture, battery-optimized       |
| MQTT Module   | Complete | Reusable, production-ready                  |
| Web App       | Complete | React + MQTT.js                             |
| Documentation | Complete | Comprehensive guides                        |
| Tests         | Verified | Build successful, tested on emulator/device |

## Quick Links

- [Android App Documentation](android/README.md)
- [MQTT Module API Reference](android/mqtt/README.md)
- [MQTT Implementation Guide](docs/mqtt/README.md)
- [Dependency Injection Guide](docs/koin/README.md)

## Getting Started (Step-by-Step)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd mqtt-aosp-demo
   ```

2. **Start the MQTT broker**
   ```bash
   cd server
   npm install
   npm start
   ```

3. **Open Android project**
   ```bash
   cd android
   # Open in Android Studio
   ```

4. **Configure broker URL**
    - Emulator: Use `tcp://10.0.2.2:1883`
    - Device: Use `tcp://YOUR_COMPUTER_IP:1883`

5. **Run the app**
    - Click Run in Android Studio
    - Or: `./gradlew installDebug`

6. **Test messaging**
    - Connect to broker
    - Send a message
    - See it appear in message list

7. **Optional: Run web app**
   ```bash
   cd web
   npm install
   npm run dev
   ```

---
**Ready to dive in?** Start with the [Android README](android/README.md) for detailed module
documentation!

**Questions or issues?** Check the troubleshooting sections or examine the code—it's
well-documented!

**Happy messaging!**
