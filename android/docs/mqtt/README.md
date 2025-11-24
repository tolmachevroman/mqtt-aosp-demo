# MQTT Integration Guide

Complete guide for the MQTT implementation in the Android demo app.

## Overview

This Android app uses **HiveMQ MQTT Client** to connect to MQTT brokers with full SSL/TLS encryption
support. The app is pre-configured to connect to **broker.hivemq.com** - a public test broker
provided by HiveMQ.

### Default Configuration

- **Broker**: `broker.hivemq.com`
- **Port**: 8883 (MQTT over TLS)
- **Protocol**: SSL/TLS encrypted
- **Certificate**: Trusted CA-signed certificate
- **Authentication**: Not required for public broker

## Quick Start

### 1. Run the Android App

**Using the Default Public Broker (Recommended)**:

1. Launch the app
2. Click "Connect" (default: `ssl://broker.hivemq.com:8883`)
3. Once connected, subscribe to `demo/messages`
4. Publish a message to see it appear in the message list

### 2. Using a Local Broker (Optional)

If you want to run your own local broker:

```bash
# Start HiveMQ Community Edition with Docker
docker run -d -p 1883:1883 --name hivemq hivemq/hivemq-ce

# Or start the Node.js test broker
cd server
npm start
```

**For Android Emulator**:

```
Broker URL: tcp://10.0.2.2:1883
```

**For Physical Device**:
```
1. Find your computer's IP: ifconfig | grep "inet "
2. Update in app: tcp://YOUR_IP:1883
3. Ensure same WiFi network
```

## SSL/TLS Configuration

### How It Works

The app uses Android's system trust store to validate SSL certificates automatically:

```kotlin
val trustManagerFactory = TrustManagerFactory.getInstance(
    TrustManagerFactory.getDefaultAlgorithm()
)
// Initialize with null to use the system's default trust store
// This trusts all certificates signed by well-known CAs
trustManagerFactory.init(null as KeyStore?)

clientBuilder.sslConfig()
    .trustManagerFactory(trustManagerFactory)
    .handshakeTimeout(30, TimeUnit.SECONDS)
    .applySslConfig()
```

**Key Features:**

- **No manual certificate configuration needed** - Works with trusted CAs automatically
- **Secure by default** - All communication is encrypted
- **30-second handshake timeout** - Handles slow networks
- **Automatic reconnection** - Exponential backoff (1s to 120s)

### Supported Brokers

The SSL configuration works with any broker using certificates from trusted Certificate Authorities:

| Broker             | URL                           | Port | Certificate Type         |
|--------------------|-------------------------------|------|--------------------------|
| **HiveMQ Public**  | ssl://broker.hivemq.com:8883  | 8883 | Trusted CA (Recommended) |
| test.mosquitto.org | ssl://test.mosquitto.org:8883 | 8883 | Standard encrypted       |
| test.mosquitto.org | ssl://test.mosquitto.org:8886 | 8886 | Let's Encrypt            |

### Common SSL Warnings (Harmless)

You may see these warnings in logcat - **they are completely normal**:

```
W  Accessing hidden method Ljava/lang/Thread;->isVirtual()Z
W  Accessing hidden method Lsun/misc/VM;->maxDirectMemory()J
W  Failed to find a usable hardware address from the network interfaces
```

**Why?** These come from the Netty library (used by HiveMQ client) trying to optimize performance.
They don't affect functionality.

### Custom Certificates (Self-Signed)

For brokers with self-signed certificates or private CAs:

```kotlin
// 1. Load custom certificate
val certificateFactory = CertificateFactory.getInstance("X.509")
val certificate = certificateFactory.generateCertificate(
    assets.open("your-broker-cert.pem")
)

// 2. Create KeyStore with custom certificate
val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
keyStore.load(null, null)
keyStore.setCertificateEntry("broker", certificate)

// 3. Create TrustManagerFactory
val trustManagerFactory = TrustManagerFactory.getInstance(
    TrustManagerFactory.getDefaultAlgorithm()
)
trustManagerFactory.init(keyStore)

// 4. Use in client configuration
clientBuilder.sslConfig()
    .trustManagerFactory(trustManagerFactory)
    .handshakeTimeout(30, TimeUnit.SECONDS)
    .applySslConfig()
```

## Architecture

### Component Overview

```
MqttApplication.kt
    ↓ initializes Koin DI
MqttModule.kt
    ↓ provides dependencies
MqttViewModel.kt
    ↓ uses
MqttRepository.kt
    ↓ manages
MqttService.kt (Foreground Service)
    ↓ contains
HiveMqttClient.kt
    ↓ connects to
MQTT Broker (broker.hivemq.com:8883)
```

### Key Components

**1. HiveMqttClient.kt**

- Low-level MQTT client wrapper
- Handles SSL/TLS configuration
- Manages connection lifecycle
- Implements publish/subscribe

**2. MqttService.kt**

- Android Foreground Service
- Maintains persistent connection
- Monitors network changes
- Manages wake locks for reliability

**3. MqttRepository.kt**

- Business logic layer
- Bridges UI and service
- Manages service lifecycle

**4. MqttViewModel.kt**

- UI state management
- User interaction handling
- Message list management

## Troubleshooting

### Connection Stays in "Connecting..." State

**1. Check Network Connectivity**

```kotlin
// The app monitors network automatically
// Look for: "Network available: true" in logs
```

**2. Verify Broker URL Format**

```kotlin
// Correct formats:
"ssl://broker.hivemq.com:8883"
"tcp://10.0.2.2:1883"

// Wrong formats:
"broker.hivemq.com:8883"          // Missing protocol
"https://broker.hivemq.com:8883"  // Wrong protocol
```

**3. Check Connection Timeout**

The default is 30 seconds. To increase:

```kotlin
val config = MqttConfig(
    brokerUrl = "ssl://broker.hivemq.com:8883",
    clientId = "...",
    connectionTimeout = 60 // Increase to 60 seconds
)
```

**4. Look for These Log Messages**

**Success indicators:**

```
SSL configured with Android system trust store
MQTT client built successfully
Initiating connection...
Connection successful in whenComplete
Connected to broker
```

**Error indicators:**

```
Connection timed out after X ms
Connection failed in whenComplete
SSLHandshakeException: ...
```

### SSL/TLS Certificate Errors

**Common issues and solutions:**

| Error                   | Cause                         | Solution                                           |
|-------------------------|-------------------------------|----------------------------------------------------|
| `SSLHandshakeException` | Certificate validation failed | Check broker URL, verify certificate is trusted    |
| `Certificate expired`   | Device time incorrect         | Update device system time (Settings → Date & time) |
| `Unknown CA`            | Self-signed certificate       | Use custom certificate configuration               |
| `Hostname mismatch`     | URL doesn't match certificate | Verify broker URL spelling                         |

**Detailed error logging:**

The client logs full exception chains:

```
Cause [0]: SSLHandshakeException: ...
Cause [1]: CertificateException: ...
Cause [2]: ...
```

### Network Issues

**For Emulator:**

- Use `10.0.2.2` instead of `localhost`
- Ensure emulator has internet access

**For Physical Device:**

- Confirm WiFi is connected
- Check firewall isn't blocking port 8883
- Verify VPN isn't interfering

### Testing Your Connection

**Using MQTT CLI:**

```bash
# Install mqtt-cli
# macOS: brew install hivemq/mqtt-cli/mqtt-cli
# Linux: See https://hivemq.github.io/mqtt-cli/

# Test connection
mqtt sub -h broker.hivemq.com -p 8883 -s -t "test/#" -v

# The -s flag enables SSL with default config
```

**Using Web Client:**

- Visit [HiveMQ WebSocket Client](http://www.mqtt-dashboard.com/)
- Connect to `broker.hivemq.com:8884` (WebSocket with SSL)
- Subscribe to same topics as your Android app

## Configuration

### Changing the Broker

Edit `MqttViewModel.kt`:

```kotlin
class MqttViewModel(
    private val mqttRepository: MqttRepository
) : ViewModel(), KoinComponent {
    
    // Change these values:
    val brokerUrl = mutableStateOf("ssl://your-broker.com:8883")
    val username = mutableStateOf("your-username")  // If required
    val password = mutableStateOf("your-password")  // If required
}
```

### Connection Parameters

Customize in `MqttConfig`:

```kotlin
val config = MqttConfig(
    brokerUrl = "ssl://broker.hivemq.com:8883",
    clientId = "android_client_123",
    username = "optional",
    password = "optional",
    keepAliveInterval = 240,  // seconds (4 minutes)
    connectionTimeout = 30,   // seconds
    cleanSession = true,
    qos = 1                  // 0, 1, or 2
)
```

### Battery Optimization Modes

**Battery Optimized:**

```kotlin
val config = MqttConfig.batteryOptimized(
    brokerUrl = "ssl://broker.hivemq.com:8883"
)
// Uses: 8-minute keep-alive, persistent sessions, QoS 1
```

**Low Latency:**

```kotlin
val config = MqttConfig.lowLatency(
    brokerUrl = "ssl://broker.hivemq.com:8883"
)
// Uses: 1-minute keep-alive, clean sessions, QoS 1
```

## Production Deployment Checklist

Before going to production:

### Security

- [ ] Use your own MQTT broker (not public test brokers)
- [ ] Enable authentication (username/password or certificates)
- [ ] Use SSL/TLS encryption (always!)
- [ ] Implement proper authorization (topic-level access control)
- [ ] Use client certificates for device authentication (recommended)

### Reliability

- [ ] Test connection on poor networks
- [ ] Verify automatic reconnection works
- [ ] Test behavior when network is lost/restored
- [ ] Implement message queuing for offline scenarios
- [ ] Set appropriate keep-alive intervals

### Performance

- [ ] Choose appropriate QoS levels (0, 1, or 2)
- [ ] Optimize payload sizes
- [ ] Use efficient topic structures
- [ ] Test with expected message volume

### Monitoring

- [ ] Add analytics for connection events
- [ ] Monitor message delivery rates
- [ ] Track connection uptime
- [ ] Log errors and exceptions

### Testing

- [ ] Test on various Android versions (API 24+)
- [ ] Test on different network types (WiFi, 4G, 5G)
- [ ] Test with multiple concurrent connections
- [ ] Load test your broker

## MQTT Features Used

### Connection Management

- **Clean Session**: Configurable persistent/clean sessions
- **Keep Alive**: Configurable heartbeat interval (default: 240s)
- **Automatic Reconnection**: Exponential backoff (1s to 120s)
- **Last Will & Testament**: Supported via config

### Publishing

- **QoS Levels**: 0 (at most once), 1 (at least once), 2 (exactly once)
- **Retained Messages**: Configurable per publish
- **Topic Validation**: Prevents publishing to invalid topics

### Subscribing

- **Topic Filters**: Full MQTT wildcard support (`+`, `#`)
- **QoS Levels**: Configurable per subscription
- **Multiple Subscriptions**: Support for subscribing to multiple topics

### Security

- **SSL/TLS**: Full support with system trust store
- **Username/Password**: Authentication support
- **Client Certificates**: SSL/TLS mutual authentication
- **Custom Trust Stores**: For self-signed certificates

## Advanced Topics

### Client ID Generation

Each app instance uses a unique, persistent client ID:

```kotlin
// Format: android_mqtt_<8-char-uuid>
// Examples:
// - android_mqtt_a3b4c5d6
// - android_mqtt_f7e8d9c0

// Get persistent client ID
val clientId = ClientIdHelper.getClientId(context)

// Reset client ID (generates new one)
val newClientId = ClientIdHelper.resetClientId(context)
```

**Benefits:**

- **Privacy**: No device-specific identifiers exposed
- **Uniqueness**: Prevents client ID collisions
- **Persistence**: Same ID across app restarts
- **Storage**: Jetpack DataStore Preferences

### Network Monitoring

The app automatically monitors network changes:

```kotlin
class MqttService {
    private lateinit var networkMonitor: NetworkMonitor
    
    private fun observeNetworkChanges() {
        networkMonitor.observeNetworkChanges()
            .onEach { isAvailable ->
                if (isAvailable) {
                    // Attempt reconnection
                } else {
                    // Handle offline state
                }
            }
            .launchIn(serviceScope)
    }
}
```

### Wake Lock Management

For reliable connections during sleep:

```kotlin
class MqttService {
    // Acquire wake lock during connection
    private fun acquireWakeLock() {
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MqttService:WakeLock"
        ).apply {
            acquire(60_000L) // 1 minute timeout
        }
    }
}
```

## Resources

### Documentation

- [HiveMQ MQTT Client](https://hivemq.github.io/hivemq-mqtt-client/)
- [MQTT 3.1.1 Specification](https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html)
- [MQTT 5.0 Specification](https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html)

### Tutorials

- [MQTT Essentials](https://www.hivemq.com/mqtt-essentials/)
- [MQTT Security Fundamentals](https://www.hivemq.com/mqtt-security-fundamentals/)

### Tools

- [MQTT CLI](https://hivemq.github.io/mqtt-cli/)
- [HiveMQ WebSocket Client](http://www.mqtt-dashboard.com/)
- [MQTT Explorer](http://mqtt-explorer.com/)

### Brokers

- [HiveMQ Cloud](https://www.hivemq.com/mqtt-cloud-broker/) - Free tier available
- [HiveMQ Community Edition](https://github.com/hivemq/hivemq-community-edition) - Open source
- [Eclipse Mosquitto](https://mosquitto.org/) - Popular open source broker

## Need Help?

If you're experiencing issues:

1. **Check logcat** - Filter by package name for MQTT-related logs
2. **Verify network** - Ensure broker is accessible
3. **Test with CLI** - Use mqtt-cli to verify broker connectivity
4. **Review logs** - Look for detailed exception stack traces
5. **Check firewall** - Ensure ports aren't blocked

**Key log tags to filter:**

- `HiveMqttClient` - Connection, SSL, publish/subscribe
- `MqttService` - Service lifecycle, network changes
- `MqttRepository` - Repository operations
