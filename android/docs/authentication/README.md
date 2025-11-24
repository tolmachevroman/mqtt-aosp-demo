# MQTT Authentication Setup

## Overview

This app is configured to connect to `broker.hivemq.com` on **port 8883** which provides:

- **TLS/SSL encryption** (secure communication)
- **No authentication required** (open for testing)
- **Trusted certificate** (signed by a well-known CA)

> **Important**: This is the **HiveMQ public test broker**. In production, you should use your
> own MQTT broker with proper user authentication.

## Connection Details

### Broker Configuration

- **URL**: `ssl://broker.hivemq.com:8883`
- **Protocol**: MQTT over TLS/SSL
- **TLS Version**: Supports TLS v1.2 and v1.3
- **Authentication**: None required for public broker
- **Certificate**: Trusted CA-signed certificate (works with Android system trust store)

### HiveMQ Public Broker

HiveMQ provides a free public MQTT broker for testing and development:

- **Host**: `broker.hivemq.com`
- **TCP Port**: 1883 (unencrypted)
- **TLS Port**: 8883 (encrypted - recommended)
- **WebSocket Port**: 8000
- **Secure WebSocket Port**: 8884

> **Note**: The public broker is open to everyone and should only be used for testing. Do not
> transmit sensitive data over the public broker.

### Alternative Test Brokers

If you want to test with different brokers, here are some alternatives:

| Broker             | URL                           | Port | Notes                     |
|--------------------|-------------------------------|------|---------------------------|
| **HiveMQ Public**  | ssl://broker.hivemq.com:8883  | 8883 | **Default - Recommended** |
| test.mosquitto.org | ssl://test.mosquitto.org:8886 | 8886 | Let's Encrypt certificate |
| test.mosquitto.org | ssl://test.mosquitto.org:8883 | 8883 | Standard encrypted port   |
| broker.emqx.io     | ssl://broker.emqx.io:8883     | 8883 | EMQX public broker        |

### test.mosquitto.org Port Reference

The test broker offers multiple ports for different testing scenarios:

| Port     | Encryption | Authentication | Notes                                    |
|----------|------------|----------------|------------------------------------------|
| **1883** | No         | No             | Plain MQTT, fully open                   |
| **1884** | No         | Yes            | Plain MQTT with auth                     |
| **8081** | Yes        | No             | Encrypted with Let's Encrypt (WebSocket) |
| **8883** | Yes        | No             | Encrypted, works with HiveMQ client      |
| **8884** | Yes        | Client Cert    | Requires client certificate              |
| **8885** | Yes        | Yes            | Encrypted with username/password         |
| **8886** | Yes        | No             | Let's Encrypt certificate                |

> **Note**: Ports **8886** and **8081** use Let's Encrypt certificates and are verified using
> system CA certificates. Port **8883** also works reliably with HiveMQ client.

### Authentication Credentials (For Ports Requiring Auth)

If you want to test authenticated ports (like 1884 or 8885), these credentials are available:

| Username | Password    | Access Level | Description                   |
|----------|-------------|--------------|-------------------------------|
| `rw`     | `readwrite` | Read/Write   | Full access to all topics (#) |
| `ro`     | `readonly`  | Read Only    | Can only subscribe to topics  |
| `wo`     | `writeonly` | Write Only   | Can only publish to topics    |

> **Note**: These are **not** real user credentials! They're shared access keys provided by the
> test broker for demonstration purposes.

## SSL/TLS Configuration

### How It Works

The HiveMQ MQTT client uses Android's system trust store to validate SSL certificates. This means:

1. **No manual certificate configuration needed** for well-known CAs
2. **Trusted certificates work automatically** (used by broker.hivemq.com)
3. **Secure by default** - certificates are validated
4. **30-second handshake timeout** to handle slow networks

### Implementation Details

The client configures SSL with:

```kotlin
val trustManagerFactory = TrustManagerFactory.getInstance(
    TrustManagerFactory.getDefaultAlgorithm()
)
trustManagerFactory.init(null) // Use system trust store

clientBuilder.sslConfig()
    .trustManagerFactory(trustManagerFactory)
    .handshakeTimeout(30, TimeUnit.SECONDS)
    .applySslConfig()
```

This configuration:

- Works with broker.hivemq.com:8883
- Validates certificates properly
- Uses Android's built-in trusted CAs
- No need for custom certificates

### Common SSL Warnings

You may see these warnings in logcat - they are **harmless**:

```
W  Accessing hidden method Ljava/lang/Thread;->isVirtual()Z
W  Accessing hidden method Lsun/misc/VM;->maxDirectMemory()J
W  Failed to find a usable hardware address from the network interfaces
```

These come from the HiveMQ client's Netty library and don't affect functionality.

## Client ID Generation

Each app instance uses a unique, persistent client ID:

- **Format**: `android_mqtt_<8-char-uuid>`
- **Generation**: UUID-based, randomly generated
- **Persistence**: Stored in Jetpack DataStore Preferences for consistency across sessions
- **Location**: `ClientIdHelper.kt`

### Example Client IDs

```
android_mqtt_a3b4c5d6
android_mqtt_f7e8d9c0
android_mqtt_1234abcd
```

### Why UUID?

- **Privacy**: No device-specific identifiers exposed
- **Uniqueness**: Prevents client ID collisions
- **Persistence**: Same ID across app restarts
- **Regeneration**: Can be reset for testing via `ClientIdHelper.resetClientId(context)`

## Security Features

### TLS/SSL Encryption

- All data transmitted over the connection is encrypted
- The HiveMQ MQTT client automatically handles SSL when using `ssl://` protocol
- Certificate validation is performed using system CA certificates

### Authentication

- Username and password fields are available in the UI
- Credentials are sent securely over the encrypted connection
- Empty credentials are handled gracefully (won't be sent if fields are empty)

## Configuration in Code

### MqttViewModel.kt

The ViewModel uses Koin's dependency injection for context:

```kotlin
class MqttViewModel(
  private val mqttRepository: MqttRepository
) : ViewModel(), KoinComponent {

  // Context is injected via Koin
  private val context: Context by inject()

  // Default connection settings (HiveMQ public broker)
  val brokerUrl = mutableStateOf("ssl://broker.hivemq.com:8883")
  val username = mutableStateOf("") // Not required for public broker
  val password = mutableStateOf("") // Not required for public broker

  // Connection with authentication
  fun connect() {
    val config = MqttConfig(
      brokerUrl = brokerUrl.value,
      clientId = ClientIdHelper.getClientId(context),
      username = username.value.takeIf { it.isNotEmpty() },
      password = password.value.takeIf { it.isNotEmpty() },
      keepAliveInterval = 240,
      qos = 1
    )
    mqttRepository.connect(config)
  }
}
```

### ClientIdHelper.kt

```kotlin
// Get or generate a persistent client ID (suspend function)
val clientId = ClientIdHelper.getClientId(context)

// Reset client ID (generates new one) (suspend function)
val newClientId = ClientIdHelper.resetClientId(context)

// Note: Both functions are suspend and must be called from a coroutine
viewModelScope.launch {
  val clientId = ClientIdHelper.getClientId(context)
  // Use the client ID...
}
```

> **Note**: ClientIdHelper uses Jetpack DataStore Preferences, which provides type-safe,
> asynchronous data storage. All operations are suspend functions and must be called from a coroutine
> scope.

## Testing Different Configurations

### Testing Encrypted Connection (Port 8883)

1. Use default: `ssl://broker.hivemq.com:8883`
2. Leave username/password empty
3. Connect successfully
4. Both publish and subscribe work

### Testing Authenticated Connection (Port 1884)

1. Change to: `tcp://test.mosquitto.org:1884`
2. Set credentials: `rw` / `readwrite`
3. Connect successfully
4. Both publish and subscribe work

### Testing Read-Only Access (Port 1884)

1. Change to: `tcp://test.mosquitto.org:1884`
2. Change credentials to: `ro` / `readonly`
3. Connect successfully
4. Subscribe to topics (works)
5. Try to publish (should fail or have no effect)

## Troubleshooting

### Connection Fails

- **Check**: Broker URL is correct (`ssl://` for encrypted ports)
- **Check**: Port number (8883 for encrypted, no auth)
- **Check**: Internet connectivity
- **Check**: Username and password if using authenticated ports

### Connection Timeout

- **Symptom**: Stuck in "Connecting..." state
- **Possible causes**:
  - Network connectivity issues
  - Firewall blocking the port
  - Certificate validation issues (use port 8883 instead of 8884)

### Certificate Errors

- **Error**: "SSL handshake failed" or certificate validation errors
  - **Solution**: Use port 8883 which has better certificate compatibility
  - **Note**: broker.hivemq.com uses valid certificates for port 8883

### Authentication Errors

- **Error**: "Not authorized"
  - **Solution**: Verify credentials match one of the valid sets for authenticated ports
  - **Note**: Public broker doesn't require authentication

## Alternative Brokers

If you want to use your own MQTT broker:

1. **Update broker URL** in `MqttViewModel.kt`
2. **Configure credentials** for your broker
3. **Consider**: Custom certificate configuration if using self-signed certs

### For Custom Certificates

The `HiveMqttClient` uses `.sslWithDefaultConfig()` which trusts system CAs. For custom
certificates, you would need to modify `HiveMqttClient.kt` to:

```kotlin
clientBuilder.sslConfig()
    .trustManagerFactory(customTrustManager)
    .applySslConfig()
```

## References

- [HiveMQ Public Broker Documentation](https://www.hivemq.com/docs/hivemq/mqtt-broker/)
- [HiveMQ MQTT Client Documentation](https://hivemq.github.io/hivemq-mqtt-client/)
- [MQTT Security Best Practices](https://www.hivemq.com/mqtt-security-fundamentals/)

