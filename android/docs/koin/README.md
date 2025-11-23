# Koin Dependency Injection Guide

Complete guide for Koin dependency injection in the MQTT Android Demo app.

## What is Koin?

Koin is a lightweight dependency injection framework for Kotlin. It's:

- **Simple**: Easy to set up and use
- **Kotlin-first**: Written in pure Kotlin
- **No code generation**: Uses DSL and reflection
- **Perfect for Android**: First-class support for Android components

## Why Use Dependency Injection?

### Benefits

1. **Testability**: Easy to mock dependencies in tests
2. **Loose Coupling**: Components don't create their dependencies
3. **Flexibility**: Easy to swap implementations
4. **Single Responsibility**: Classes focus on their core logic
5. **Maintainability**: Changes to dependencies don't require modifying consumers

### Without DI (Before)

```kotlin
class MqttViewModel(application: Application) : AndroidViewModel(application) {
    // ViewModel creates its own dependency
    private val mqttManager = MqttManager(application)
}
```

**Problems:**

- Hard to test (can't mock MqttManager)
- Tight coupling to MqttManager implementation
- ViewModel has to know how to create MqttManager

### With DI (After)

```kotlin
class MqttViewModel(
    private val mqttManager: MqttManager
) : ViewModel() {
    // Dependency is injected
}
```

**Benefits:**

- Easy to test (inject mock MqttManager)
- Loose coupling
- ViewModel doesn't care how MqttManager is created

## Architecture

```
┌──────────────────────────────┐
│         MqttApplication     │
│  (Initializes Koin on app   │
│   startup)                  │
└──────────────────────────────┘
...existing code...
