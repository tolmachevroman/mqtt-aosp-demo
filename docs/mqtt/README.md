# MQTT Integration Guide

Complete guide for the MQTT implementation in the Android demo app.

## Overview

This Android app uses **HiveMQ MQTT Client** to connect to an MQTT broker and exchange messages with
other clients (web, mobile, IoT devices, etc.).

## Quick Start

### 1. Start the MQTT Broker

```bash
cd server
npm start
```

The broker runs on:

- **MQTT TCP**: `localhost:1883`
- **MQTT WebSocket**: `ws://localhost:8080`
- **HTTP API**: `http://localhost:3001`

### 2. Configure & Run Android App

**For Emulator** (default):

```
Broker URL: tcp://10.0.2.2:1883
```

**For Physical Device**:

```
1. Find your computer's IP: ifconfig | grep "inet "
2. Update in app: tcp://YOUR_IP:1883
3. Ensure same WiFi network
```

### 3. Test Communication

1. Launch app → Click "Connect"
2. Subscribe to `test/topic`
3. Send a message
4. Launch web app or another client
5. Messages sync in real-time!

## Architecture

### MQTT Components

```
MqttApplication.kt
    ↓ initializes
MqttManager.kt (Singleton)
    ↓ injected into
MqttViewModel.kt
    ↓ provides state to
MainActivity.kt (Compose UI)
```

### MqttManager - MQTT Client Wrapper

**Location**: `android/app/src/main/java/.../MqttManager.kt`

**Purpose**: Wraps HiveMQ client, provides simple API for MQTT operations

**Features**:

- ✅ Connect/disconnect to broker
- ✅ Subscribe to topics (with QoS support)
- ✅ Publish messages
- ✅ Callback-based event handling
- ✅ Automatic reconnection
- ✅ Async/non-blocking operations

**Key Methods**:

```kotlin
class MqttManager(context: Context) {
    // Connect to broker
    fun connect(serverUri: String, username: String = "", password: String = "")
    
    // Subscribe to topic
    fun subscribe(topic: String, qos: Int = 1)
    
    // Publish message
    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false)
    
    // Unsubscribe from topic
    fun unsubscribe(topic: String)
    
    // Disconnect from broker
    fun disconnect()
    
    // Check connection status
    fun isConnected(): Boolean
}
```

**Callbacks**:

```kotlin
mqttManager.onConnectionSuccess = { /* Connected! */ }
mqttManager.onConnectionLost = { reason -> /* Handle disconnect */ }
mqttManager.onMessageArrived = { topic, message -> /* New message */ }
mqttManager.onError = { error -> /* Handle error */ }
```

### MqttViewModel - State Management

**Location**: `android/app/src/main/java/.../MqttViewModel.kt`

**Purpose**: Manages MQTT state and business logic

**State Properties**:

```kotlin
val isConnected: MutableState<Boolean>
val brokerUrl: MutableState<String>
val subscribedTopic: MutableState<String>
val publishTopic: MutableState<String>
val messageToSend: MutableState<String>
val messages: SnapshotStateList<MqttMessageItem>
val statusMessage: MutableState<String>
```

**Actions**:

- `connect()` - Connect to broker
- `disconnect()` - Disconnect from broker
- `subscribe()` - Subscribe to topic
- `publishMessage()` - Send message
- `clearMessages()` - Clear message history

### UI - Jetpack Compose

**Location**: `android/app/src/main/java/.../MainActivity.kt`

Four main sections:

1. **Connection** - Broker URL, connect/disconnect, status
2. **Subscription** - Topic input, subscribe button
3. **Publishing** - Topic, message input, send button
4. **Messages** - Scrollable list of sent/received messages

## Why HiveMQ MQTT Client?

We chose **HiveMQ** over Eclipse Paho because:

| Feature | HiveMQ | Eclipse Paho |
|---------|--------|--------------|
| Maintenance | ✅ Active | ⚠️ Less frequent |
| API Style | ✅ Modern async | ❌ Callback-based |
| Android Service | ✅ Not needed | ❌ Required |
| Kotlin Support | ✅ Excellent | ⚠️ Java-first |
| Auto-reconnect | ✅ Built-in | ⚠️ Manual setup |
| Complexity | ✅ Simple | ❌ More complex |

## MQTT Broker Configuration

### Server Details

The demo includes a Node.js MQTT broker (Aedes):

**File**: `server/index.js`

**Ports**:

- `1883` - MQTT TCP (for Android/native clients)
- `8080` - MQTT WebSocket (for web clients)
- `3001` - HTTP REST API (for testing)

**Features**:

- Event logging (connections, subscriptions, messages)
- CORS enabled
- HTTP publish endpoint for testing

### Connection from Android

**Emulator**:

```kotlin
brokerUrl = "tcp://10.0.2.2:1883"  // Maps to host's localhost
```

**Physical Device**:

```kotlin
brokerUrl = "tcp://192.168.1.100:1883"  // Your computer's IP
```

**With Authentication** (if enabled):

```kotlin
mqttManager.connect(
    serverUri = brokerUrl,
    username = "your_username",
    password = "your_password"
)
```

## MQTT Topics

### Topic Patterns

**Single Level**:

```
home/livingroom/temperature
sensors/humidity
notifications/user123
```

**Wildcards**:

```
home/+/temperature        # + matches one level
home/#                    # # matches multiple levels
sensors/+/status
devices/+/+/data
```

### Default Topics in App

- **Default subscribe**: `test/topic`
- **Default publish**: `test/topic`

### Best Practices

1. **Hierarchical**: Use `/` for structure (`device/room/sensor`)
2. **Descriptive**: Clear naming (`sensors/temp` not `s/t`)
3. **Consistent**: Same pattern across app
4. **Avoid spaces**: Use `_` or `-` instead
5. **Forward slashes only**: Don't start with `/`

## Quality of Service (QoS)

### QoS Levels

| Level | Guarantee | Use Case |
|-------|-----------|----------|
| QoS 0 | At most once | Fire and forget |
| QoS 1 | At least once | Default, reliable |
| QoS 2 | Exactly once | Critical data |

**Current app uses**: QoS 1 (at least once delivery)

**To change**:

```kotlin
mqttManager.subscribe(topic = "test/topic", qos = 2)
mqttManager.publish(topic = "test/topic", message = "Hello", qos = 2)
```

## Advanced Features

### Retained Messages

Messages stored on broker and delivered to new subscribers:

```kotlin
mqttManager.publish(
    topic = "status/device",
    message = "online",
    retained = true
)
```

### Last Will and Testament (LWT)

Message sent when client disconnects unexpectedly:

```kotlin
// Modify MqttManager.connect() to add:
val options = MqttConnectOptions().apply {
    // ... existing options
    setWill(
        "status/android",
        "offline".toByteArray(),
        1,  // QoS
        true  // retained
    )
}
```

### Clean Session

```kotlin
// In MqttManager.connect():
isCleanSession = true   // Don't persist session
isCleanSession = false  // Persist subscriptions
```

## Testing & Debugging

### Test with Multiple Clients

1. **Android App** → Subscribe to `test/topic`
2. **Web App** → Subscribe to `test/topic`
3. **Terminal** → `mosquitto_pub -h localhost -t test/topic -m "Test"`
4. All clients receive the message!

### Using mosquitto_pub/sub

```bash
# Subscribe
mosquitto_sub -h localhost -t test/topic

# Publish
mosquitto_pub -h localhost -t test/topic -m "Hello World"

# Subscribe with wildcard
mosquitto_sub -h localhost -t "sensors/#"
```

### HTTP API Testing

```bash
# Publish via REST API
curl -X POST http://localhost:3001/publish \
  -H "Content-Type: application/json" \
  -d '{"topic": "test/topic", "message": "Hello from curl!"}'
```

### Check Server Status

```bash
curl http://localhost:3001/status
```

## Troubleshooting

### Connection Issues

**Problem**: Can't connect from emulator

```
Solution: Use tcp://10.0.2.2:1883 (NOT localhost)
```

**Problem**: Can't connect from physical device

```
Solution:
1. Check same WiFi network
2. Use computer's IP (not 10.0.2.2)
3. Disable firewall temporarily
4. Check port 1883 is open
```

**Problem**: Connection timeout

```
Solution:
1. Verify server is running
2. Check broker URL is correct
3. Check network connectivity
4. Look at server logs for errors
```

### Message Issues

**Problem**: Messages not arriving

```
Solution:
1. Verify subscription is active
2. Check topic names match exactly
3. Ensure QoS is appropriate
4. Check server logs
```

**Problem**: Duplicate messages

```
Solution:
1. Check for multiple subscriptions
2. Verify QoS settings
3. Look for reconnection loops
```

### Server Issues

**Problem**: Port already in use

```bash
# Find process
lsof -i :1883

# Kill it
kill -9 <PID>
```

**Problem**: WebSocket not working

```
Check port 8080 is open
Verify CORS is enabled
Check browser console
```

## Performance Tips

1. **Use appropriate QoS**: QoS 0 for non-critical data
2. **Batch messages**: Combine multiple updates
3. **Optimize topics**: Avoid deep hierarchies
4. **Clean up**: Unsubscribe from unused topics
5. **Keepalive**: Balance between battery and responsiveness

## Security Considerations

⚠️ **This is a demo**. For production:

1. **Enable TLS/SSL**:
   ```kotlin
   serverUri = "ssl://broker.example.com:8883"
   ```

2. **Use Authentication**:
   ```kotlin
   connect(serverUri, username, password)
   ```

3. **Certificate Pinning**: Validate server certificates

4. **Input Validation**: Sanitize all messages

5. **Access Control**: Broker-level ACLs

## Resources

### Documentation

- [HiveMQ Client Docs](https://github.com/hivemq/hivemq-mqtt-client)
- [MQTT Protocol](https://mqtt.org/)
- [MQTT Essentials](https://www.hivemq.com/mqtt-essentials/)

### Tools

- [MQTT Explorer](http://mqtt-explorer.com/) - Desktop MQTT client
- [mosquitto](https://mosquitto.org/) - CLI tools
- [MQTT.fx](https://mqttfx.jensd.de/) - Java-based client

## Next Steps

Want to extend the MQTT functionality?

- [ ] Add TLS/SSL support
- [ ] Implement authentication UI
- [ ] Support multiple topic subscriptions
- [ ] Add QoS selection in UI
- [ ] Implement retained messages toggle
- [ ] Add Last Will and Testament
- [ ] Create connection profiles
- [ ] Add message persistence
- [ ] Implement offline queuing

See the main README for the complete feature list and roadmap!
