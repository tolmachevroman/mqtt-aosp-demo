# MQTT Cross-Platform Demo

A comprehensive demonstration of MQTT messaging across multiple platforms: Android, iOS, Web, and Server.

## Overview

This project demonstrates real-time MQTT messaging with clients for Android (Kotlin), iOS (Swift), and Web (React), all communicating through a Node.js MQTT broker.

## Quick Start

### 1. Start the MQTT Broker

```bash
cd server
npm install
npm start
```

The broker will start on:
- MQTT TCP: `localhost:1883`
- MQTT WebSocket: `ws://localhost:8080`
- HTTP API: `http://localhost:3001`

### 2. Run Clients

**Android:**
```bash
cd android
./gradlew installDebug
# Or open in Android Studio and click Run
```

**iOS:**
```bash
cd iOS
open "MQTT Demo.xcodeproj"
# Then press Cmd+R in Xcode
```

**Web:**
```bash
cd web
npm install
npm run dev
# Open browser to http://localhost:5173
```

## Project Structure

```
mqtt-cross-platform-demo/
├── server/                 # MQTT Broker (Node.js + Aedes)
│   ├── index.js           # Server implementation
│   └── package.json
│
├── android/               # Android Client (Kotlin)
│   ├── app/              # Main application
│   └── mqtt/             # Reusable MQTT module
│
├── iOS/                   # iOS Client (Swift)
│   └── MQTT Demo/        # SwiftUI application
│
├── web/                   # Web Client (React)
│   └── src/              # React components
│
└── docs/                  # Documentation
```

## Features

### MQTT Broker (Server)

- Aedes MQTT broker
- TCP connections on port 1883
- WebSocket support on port 8080
- HTTP REST API for testing
- Connection and message logging
- CORS enabled for web clients

### Android Client

**Technology Stack:**
- Kotlin with Jetpack Compose
- HiveMQ MQTT client
- MVVM architecture with Repository pattern
- Koin dependency injection
- Foreground service for background operation

**Features:**
- Real-time messaging
- Topic subscriptions with wildcards
- QoS support (0, 1, 2)
- Connection management
- Message history
- Network monitoring

### iOS Client

**Technology Stack:**
- Swift with SwiftUI
- mqtt-nio MQTT client
- MVVM architecture
- Async/await concurrency
- NIOTransportServices for iOS networking

**Features:**
- Real-time messaging
- Topic subscriptions with wildcards
- QoS support (0, 1, 2)
- Connection management
- Message history
- Clean, native iOS UI

### Web Client

**Technology Stack:**
- React with Vite
- MQTT.js client library
- Modern React hooks
- WebSocket connection

**Features:**
- Browser-based MQTT client
- Real-time messaging
- Topic subscriptions
- Clean, responsive UI

## Configuration

### Broker Connection

**Android Emulator:**
```
Host: 10.0.2.2
Port: 1883
```

**iOS Simulator:**
```
Host: localhost
Port: 1883
```

**Physical Devices:**
```
Host: YOUR_COMPUTER_IP
Port: 1883
```

**Web Browser:**
```
WebSocket: ws://localhost:8080
```

### Default Topics

All clients use `demo/messages` as the default topic for easy cross-platform testing.

## Cross-Platform Testing

1. Start the MQTT broker
2. Launch all clients (Android, iOS, Web)
3. Connect each client to the broker
4. Subscribe all clients to `demo/messages`
5. Send a message from any client
6. Watch it appear instantly on all other clients

## MQTT Features

### Topics

Standard MQTT topic structure:
```
demo/messages
sensors/temperature
home/livingroom/light
```

### Wildcards

**Single-level (+):**
```
sensors/+/temperature
```

**Multi-level (#):**
```
home/#
```

### Quality of Service (QoS)

| Level | Guarantee | Use Case |
|-------|-----------|----------|
| 0 | At most once | Non-critical data |
| 1 | At least once | Default, reliable |
| 2 | Exactly once | Critical data |

## Architecture

### Android

```
UI (Compose) → ViewModel → Repository → DataSource (HiveMQ)
                                    ↓
                              Foreground Service
```

### iOS

```
Views (SwiftUI) → ViewModel → MQTTManager (mqtt-nio)
```

### Web

```
React Components → MQTT.js Client
```

### Server

```
Express HTTP API
         ↓
Aedes MQTT Broker
    ↓        ↓
  TCP    WebSocket
```

## Development

### Prerequisites

**General:**
- Node.js 16+ (for server and web)
- Git

**Android:**
- Android Studio Arctic Fox or later
- JDK 17+
- Android SDK 24+

**iOS:**
- macOS
- Xcode 15.0+
- iOS 17.0+ SDK

**Web:**
- Modern web browser
- npm or yarn

### Building from Source

**Android:**
```bash
cd android
./gradlew clean build
```

**iOS:**
```bash
cd iOS
# Open in Xcode and build (Cmd+B)
```

**Web:**
```bash
cd web
npm install
npm run build
```

**Server:**
```bash
cd server
npm install
# No build step needed
```

## Testing

### Manual Testing

1. Start broker: `cd server && npm start`
2. Launch clients on different platforms
3. Connect all clients to broker
4. Subscribe to common topic
5. Send messages between clients
6. Verify real-time delivery

### Using Command Line Tools

**Publish:**
```bash
mosquitto_pub -h localhost -t demo/messages -m "Test message"
```

**Subscribe:**
```bash
mosquitto_sub -h localhost -t demo/messages
```

**HTTP API:**
```bash
curl -X POST http://localhost:3001/publish \
  -H "Content-Type: application/json" \
  -d '{"topic": "demo/messages", "message": "Hello"}'
```

## Troubleshooting

### Broker Won't Start

- Check if ports 1883, 8080, or 3001 are already in use
- Run `npm install` in server directory
- Check Node.js version (16+ required)

### Client Can't Connect

**Emulator/Simulator:**
- Android: Use `10.0.2.2` for localhost
- iOS: Use `localhost`
- Check broker is running

**Physical Devices:**
- Find computer's IP address
- Use that IP in client configuration
- Ensure device and computer on same WiFi
- Check firewall settings

### Messages Not Appearing

- Verify all clients subscribed to same topic
- Check topic names match exactly (case-sensitive)
- Confirm QoS levels are appropriate
- Check broker logs for errors

## Documentation

- **Android**: See `android/README.md` and `docs/mqtt/README.md`
- **iOS**: See `iOS/README.md`
- **Server**: See `server/README.md`
- **Web**: See `web/README.md`

## Performance

| Metric | Value |
|--------|-------|
| Message Latency | < 100ms (local network) |
| Max Clients | 1000+ (depends on hardware) |
| Message Throughput | 1000+ msg/sec |
| Memory (per client) | ~20-50MB |
| Battery Impact | Minimal with proper configuration |

## Security Notes

**Warning:** This is a demo project for development and testing.

For production use:
1. Enable TLS/SSL encryption
2. Implement authentication (username/password)
3. Use access control lists (ACLs)
4. Validate all inputs
5. Use secure WebSocket (wss://)
6. Implement rate limiting
7. Monitor and log security events

## License

This is a demo project for educational purposes.

## Resources

- [MQTT Protocol](https://mqtt.org/)
- [Aedes Broker](https://github.com/moscajs/aedes)
- [HiveMQ Client](https://github.com/hivemq/hivemq-mqtt-client)
- [mqtt-nio](https://github.com/swift-server-community/mqtt-nio)
- [MQTT.js](https://github.com/mqttjs/MQTT.js)

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review platform-specific documentation
3. Check broker logs for errors
4. Verify network configuration

## Contributing

This is a demonstration project. Feel free to fork and modify for your own use cases.