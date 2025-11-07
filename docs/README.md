# Documentation Index

Complete documentation for the MQTT Android Demo project.

## Main Guides

### [MQTT Integration Guide](mqtt/README.md)

Complete guide for MQTT implementation in the Android app.

**Topics covered:**

- Quick start and configuration
- Architecture and components
- MqttManager API reference
- Topics, QoS, and advanced features
- Testing and debugging
- Security considerations
- Troubleshooting

### [Koin Dependency Injection Guide](koin/README.md)

Complete guide for Koin DI implementation.

**Topics covered:**

- What is Koin and why use it?
- Architecture overview
- Implementation details
- Testing with Koin
- DSL keywords and patterns
- Best practices

## 🚀 Quick Links

### Getting Started

- [Quick Start Guide](../QUICK_START.md) - Get running in 5 minutes
- [Main README](../README.md) - Project overview

### Implementation

- [MQTT Guide](mqtt/README.md) - MQTT implementation
- [Koin Guide](koin/README.md) - Dependency injection
- [Setup Timeline](../MQTT_ANDROID_SETUP.md) - Implementation history

### Improvements

- [Recent Improvements](../IMPROVEMENTS_SUMMARY.md) - Latest changes

## 📱 Android App Structure

```
android/app/src/main/java/.../
├── MqttApplication.kt     # App initialization (Koin setup)
├── MainActivity.kt        # Jetpack Compose UI
├── MqttViewModel.kt       # State management
├── MqttManager.kt         # MQTT client wrapper
└── di/
    └── AppModule.kt       # Koin DI module
```

## Key Technologies

| Technology | Purpose | Documentation |
|-----------|---------|---------------|
| HiveMQ | MQTT Client | [MQTT Guide](mqtt/README.md) |
| Koin | Dependency Injection | [Koin Guide](koin/README.md) |
| Jetpack Compose | UI Framework | [Android Developers](https://developer.android.com/jetpack/compose) |
| ViewModel | State Management | [Architecture Guide](https://developer.android.com/topic/libraries/architecture/viewmodel) |
| Material 3 | Design System | [Material Design](https://m3.material.io/) |

## 🎯 Learning Path

### Beginner

1. Start with [Quick Start](../QUICK_START.md)
2. Run the app and test basic features
3. Read [Main README](../README.md) for overview

### Intermediate

1. Study [MQTT Guide](mqtt/README.md)
2. Understand MqttManager implementation
3. Learn about topics and QoS

### Advanced

1. Deep dive into [Koin Guide](koin/README.md)
2. Understand DI architecture
3. Explore testing patterns
4. Study [Implementation Timeline](../MQTT_ANDROID_SETUP.md)

## 🛠️ Common Tasks

### Connect to MQTT Broker

See: [MQTT Guide - Quick Start](mqtt/README.md#quick-start)

### Add New Dependency

See: [Koin Guide - Migration Path](koin/README.md#migration-path)

### Test MQTT Messages

See: [MQTT Guide - Testing](mqtt/README.md#testing--debugging)

### Mock Dependencies for Testing

See: [Koin Guide - Testing](koin/README.md#testing-with-koin)

## 📖 External Resources

### MQTT Protocol

- [MQTT.org](https://mqtt.org/) - Official specification
- [HiveMQ MQTT Essentials](https://www.hivemq.com/mqtt-essentials/) - Tutorial series
- [HiveMQ Client Docs](https://github.com/hivemq/hivemq-mqtt-client) - Library documentation

### Koin

- [Koin Official Docs](https://insert-koin.io/) - Complete guide
- [Koin for Compose](https://insert-koin.io/docs/reference/koin-compose/compose) - Compose
  integration
- [Koin Android Guide](https://insert-koin.io/docs/reference/koin-android/start) - Android specifics

### Android Development

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI toolkit
- [Android Architecture](https://developer.android.com/topic/architecture) - Best practices
- [Material 3](https://m3.material.io/) - Design system

---

**Happy coding! 📱💬**
