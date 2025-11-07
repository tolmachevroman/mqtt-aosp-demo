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
┌─────────────────────────────────────────┐
│         MqttApplication                 │
│  (Initializes Koin on app startup)     │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│         di/AppModule                    │
│  (Defines what to inject and how)      │
│                                         │
│  • MqttManager (singleton)              │
│  • MqttViewModel (factory)              │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│         MainActivity                    │
│  (Composes UI)                          │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│      MqttDemoScreen (Composable)        │
│  viewModel = koinViewModel()            │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│         MqttViewModel                   │
│  (Receives injected MqttManager)        │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│         MqttManager                     │
│  (Singleton instance shared app-wide)   │
└─────────────────────────────────────────┘
```

## Implementation Details

### 1. Application Class

**File**: `MqttApplication.kt`

```kotlin
class MqttApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Start Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MqttApplication)
            modules(appModule)
        }
    }
}
```

**What it does:**

- Initializes Koin when the app starts
- Provides Android context to Koin
- Loads the dependency modules

**AndroidManifest.xml:**

```xml
<application
    android:name=".MqttApplication">
    <!-- app content -->
</application>
```

### 2. Koin Module

**File**: `di/AppModule.kt`

```kotlin
val appModule = module {
    // Provide MqttManager as a singleton
    single { MqttManager(androidContext()) }
    
    // Provide MqttViewModel
    viewModel { MqttViewModel(get()) }
}
```

**What it defines:**

#### `single { MqttManager(androidContext()) }`

- Creates **one instance** of MqttManager for the entire app lifetime
- Uses `androidContext()` to get the Application context
- Perfect for MQTT connection (should be shared)

#### `viewModel { MqttViewModel(get()) }`

- Creates a new ViewModel instance when requested
- `get()` tells Koin to inject MqttManager
- Koin knows to inject the singleton MqttManager instance

### 3. ViewModel with Injection

**File**: `MqttViewModel.kt`

```kotlin
class MqttViewModel(
    private val mqttManager: MqttManager
) : ViewModel() {
    // Use the injected mqttManager
}
```

**Key changes:**

- Now extends `ViewModel` instead of `AndroidViewModel`
- Receives `MqttManager` via constructor
- No longer creates its own instance

### 4. Composable with Koin

**File**: `MainActivity.kt`

```kotlin
@Composable
fun MqttDemoScreen(viewModel: MqttViewModel = koinViewModel()) {
    // Use viewModel
}
```

**What happens:**

1. `koinViewModel()` asks Koin for a MqttViewModel
2. Koin sees MqttViewModel needs MqttManager
3. Koin provides the singleton MqttManager instance
4. Koin creates MqttViewModel with injected MqttManager
5. Returns the ViewModel to the Composable

## Dependency Graph

```
MqttDemoScreen
    ↓ requests
koinViewModel<MqttViewModel>()
    ↓ Koin resolves
MqttViewModel(mqttManager: MqttManager)
    ↓ Koin provides
MqttManager (singleton instance)
```

## Testing with Koin

### Unit Testing ViewModel

```kotlin
class MqttViewModelTest {
    @Test
    fun `should connect when connect is called`() {
        // Create a mock MqttManager
        val mockManager = mockk<MqttManager>()
        
        // Inject the mock
        val viewModel = MqttViewModel(mockManager)
        
        // Test
        viewModel.connect()
        
        // Verify
        verify { mockManager.connect(any()) }
    }
}
```

**Benefits:**

- Can test ViewModel in isolation
- Don't need real MQTT connection
- Fast and reliable tests

### Integration Testing with Koin

```kotlin
class MqttIntegrationTest {
    @Before
    fun setup() {
        startKoin {
            modules(module {
                single { MockMqttManager() }
                viewModel { MqttViewModel(get()) }
            })
        }
    }
    
    @Test
    fun `should handle MQTT flow`() {
        val viewModel: MqttViewModel by inject()
        // Test with injected dependencies
    }
}
```

## Koin DSL Keywords

### `single`

Creates a **singleton** instance (one instance for entire app).

```kotlin
single { MqttManager(androidContext()) }
```

Use for:

- Network clients
- Database instances
- Shared repositories
- Services that should be shared

### `factory`

Creates a **new instance** every time it's requested.

```kotlin
factory { SomeRepository() }
```

Use for:

- Objects that shouldn't be shared
- Lightweight objects
- Objects with state that varies

### `viewModel`

Special factory for ViewModels with lifecycle awareness.

```kotlin
viewModel { MqttViewModel(get()) }
```

Use for:

- All ViewModels
- Automatic lifecycle management
- Compose integration

### `get()`

Retrieves a dependency from Koin.

```kotlin
viewModel { MqttViewModel(get()) }  // get() resolves to MqttManager
```

### `androidContext()`

Gets the Android Application context.

```kotlin
single { MqttManager(androidContext()) }
```

## Advantages in This Project

### 1. Singleton MqttManager

- **One MQTT connection** shared across the app
- Prevents multiple connections to broker
- Efficient resource usage

### 2. Testable ViewModel

- Can inject mock MqttManager
- Test business logic without network
- Fast, reliable tests

### 3. Clean Code

- ViewModel focuses on state management
- Doesn't know how to create dependencies
- Single Responsibility Principle

### 4. Easy to Extend

Want to add analytics? Just:

```kotlin
val appModule = module {
    single { MqttManager(androidContext()) }
    single { AnalyticsService() }
    viewModel { MqttViewModel(get(), get()) }
}
```

ViewModel constructor:

```kotlin
class MqttViewModel(
    private val mqttManager: MqttManager,
    private val analytics: AnalyticsService
) : ViewModel()
```

## Migration Path

If you want to add more dependencies:

### 1. Define in Module

```kotlin
val appModule = module {
    single { MqttManager(androidContext()) }
    single { PreferencesManager(androidContext()) }
    viewModel { MqttViewModel(get(), get()) }
}
```

### 2. Update Consumer

```kotlin
class MqttViewModel(
    private val mqttManager: MqttManager,
    private val preferences: PreferencesManager
) : ViewModel()
```

### 3. That's It!

Koin handles the rest automatically.

## Common Patterns

### Providing Interface Implementations

```kotlin
single<IMqttManager> { MqttManager(androidContext()) }
```

### Named Dependencies

```kotlin
single(named("production")) { MqttManager(androidContext()) }
single(named("test")) { MockMqttManager() }

viewModel { MqttViewModel(get(named("production"))) }
```

### Module Organization

```kotlin
val networkModule = module {
    single { MqttManager(androidContext()) }
}

val viewModelModule = module {
    viewModel { MqttViewModel(get()) }
}

startKoin {
    modules(networkModule, viewModelModule)
}
```

## Best Practices

1. **Use `single` for stateful services** (like MqttManager)
2. **Use `viewModel` for ViewModels** (lifecycle-aware)
3. **Keep modules focused** (network, data, UI, etc.)
4. **Don't over-inject** (only inject what you need)
5. **Test with dependency injection** (easy to mock)

## Resources

- [Koin Official Documentation](https://insert-koin.io/)
- [Koin for Compose](https://insert-koin.io/docs/reference/koin-compose/compose)
- [Koin Android Guide](https://insert-koin.io/docs/reference/koin-android/start)

## Summary

Koin makes the MQTT Android Demo:

- ✅ **More testable** - Easy to mock dependencies
- ✅ **Better structured** - Clear separation of concerns
- ✅ **Easier to maintain** - Changes isolated to modules
- ✅ **More professional** - Production-ready architecture
- ✅ **Simpler to extend** - Just add to the module

The investment in DI pays off as the app grows!
