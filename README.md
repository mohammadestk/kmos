# ⚡ Kmos

### Kotlin Multiplatform Offline-First Sync SDK

**Reliable synchronization. One codebase. Six platforms.**

> 🚧 **Status: In Progress** — This SDK is actively under development. APIs may change.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-purple?logo=kotlin)](https://kotlinlang.org)
[![KMP](https://img.shields.io/badge/KMP-All%20Targets-blue)](https://www.jetbrains.com/kotlin-multiplatform/)
[![License](https://img.shields.io/badge/License-TBD-green)](#license)

---

## 🎯 What is Kmos?

Kmos is a **Kotlin Multiplatform SDK** for building offline-first applications with reliable, correct synchronization. Write your sync logic once in `commonMain` — it runs everywhere.

```
┌─────────────────────────────────────────────────────────────┐
│                        YOUR APP                             │
├─────────────────────────────────────────────────────────────┤
│                    SyncRepository<T>                        │
│                     (typed APIs)                            │
├─────────────────────────────────────────────────────────────┤
│                     Sync Engine                             │
│         ┌───────────┴───────────┴───────────┐              │
│         ▼               ▼                   ▼              │
│   Operation Queue   Retry Policy    Conflict Resolver      │
│    (idempotent)    (backoff+jitter)    (LWW/Custom)        │
├─────────────────────────────────────────────────────────────┤
│    StorageAdapter          TransportAdapter                 │
│       (Room 3)                 (Ktor)                      │
└─────────────────────────────────────────────────────────────┘
```

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔌 **Offline-First** | Sync runs on foreground, manual trigger, or optional interval |
| 🌍 **6 Platforms** | Android, iOS, JVM, Desktop, JS, WasmJS — one implementation |
| 🔒 **Thread-Safe** | Single-writer Channel-driven architecture — no locks needed |
| 🔄 **Idempotent Retry** | Exponential backoff with jitter, dead-letter path |
| ⚖️ **Conflict Resolution** | Last-Write-Wins default, custom resolver support |
| 🧩 **Pluggable** | Storage & transport adapters with contract test suites |
| 🗄️ **Room 3** | KMP-native storage reference implementation |
| 💉 **Koin DI** | Ready-to-use dependency injection module |

---

## 🚀 Quick Start

### 1. Add Dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.esteki.kmos:shared:VERSION")
}
```

### 2. Initialize SDK

```kotlin
// With Koin (recommended)
startKoin {
    modules(syncModule(databaseName = "myapp.db"))
}

// Or manually
val client = SyncClient.build(scope) {
    storage(RoomStorageAdapter(database))
    transport(KtorTransportAdapter())
    retry(ExponentialBackoffRetryPolicy())
}
```

### 3. Use SyncRepository

```kotlin
// Get a typed repository
val tasks: SyncRepository<Task> = client.repository()

// Observe all tasks
tasks.observeAll().collect { taskList ->
    // Update UI
}

// Create or update a task
tasks.upsert(Task(id = "1", title = "Buy milk"))

// Trigger manual sync
client.trigger()

// Handle failed operations
client.failedOperations().collect { failed ->
    failed.forEach { operation ->
        // Show retry button to user
    }
}
```

---

## 🏗️ Architecture

### Core Components

| Component | Purpose |
|-----------|---------|
| `SyncClient` | Public entry point, builder DSL |
| `SyncEngine` | Single-writer, Channel-driven command processor |
| `OperationQueue` | Persisted queue with idempotency deduplication |
| `RetryPolicy` | Exponential backoff with configurable dead-letter |
| `ConflictResolver` | LWW or custom merge logic |
| `StorageAdapter` | Local persistence interface |
| `TransportAdapter` | Network transport interface |

### Data Flow

```
User Action
    │
    ▼
SyncRepository.upsert()
    │
    ▼
SyncCommand.Enqueue ──► OperationQueue
                              │
                              ▼
                    SyncCommand.TriggerSync
                              │
                              ▼
                    TransportAdapter.push()
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
               Success              Failure
                    │                   │
                    ▼                   ▼
            StorageAdapter.write()  RetryPolicy
            (state = Synced)     (backoff/retry)
```

---

## ⚙️ Configuration

### RetryPolicy

```kotlin
ExponentialBackoffRetryPolicy(
    baseDelay = 1000.milliseconds,    // Initial delay
    maxDelay = 60_000.milliseconds,   // Maximum delay cap
    maxAttempts = 5,                   // Dead-letter threshold
    jitterFactor = 0.3,               // Randomness factor
)
```

### ConflictResolver

```kotlin
// Default: Last-Write-Wins
LastWriteWinsConflictResolver()

// Custom resolver
ConflictResolver<MyEntity> { local, remote ->
    local.copy(
        version = maxOf(local.version, remote.version),
        data = merge(local.data, remote.data)
    )
}
```

### SyncTrigger

```kotlin
DefaultSyncTrigger(
    scope = coroutineScope,
    onTrigger = { client.trigger() },
).apply {
    startInterval(5.minutes)  // Optional periodic sync
}
```

---

## 🛠️ Development

### Build Commands

```bash
# 🤖 Android
./gradlew :androidApp:assembleDebug

# 🖥️ Desktop (JVM)
./gradlew :desktopApp:run

# 🌐 Web (Wasm — modern browsers)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# 🌐 Web (JS — older browsers)
./gradlew :webApp:jsBrowserDevelopmentRun

# 🍎 iOS — open iosApp/ in Xcode
```

### Test Commands

```bash
# 🧪 Android
./gradlew :shared:testAndroidHostTest

# 🧪 Desktop (JVM)
./gradlew :shared:jvmTest

# 🧪 Web (Wasm)
./gradlew :shared:wasmJsTest

# 🧪 Web (JS)
./gradlew :shared:jsTest

# 🧪 iOS simulator
./gradlew :shared:iosSimulatorArm64Test
```

---

## 📁 Project Structure

```
kmos/
├── shared/                    # Core SDK module
│   └── src/
│       ├── commonMain/        # 🎯 All sync logic lives here
│       │   └── dev/esteki/kmos/sync/
│       │       ├── core/      # Engine, interfaces, models
│       │       ├── network/   # Ktor transport
│       │       ├── storage/   # Room 3 adapter
│       │       └── trigger/   # Lifecycle hooks
│       ├── commonTest/        # Contract tests & unit tests
│       ├── androidMain/       # Android actuals
│       ├── iosMain/           # iOS actuals
│       ├── jvmMain/           # JVM/Desktop actuals
│       └── webMain/           # Web actuals
├── androidApp/                # Android demo app
├── desktopApp/                # Desktop demo app
├── webApp/                    # Web demo app
├── iosApp/                    # iOS demo app
└── specs/                     # Design specifications
```

---

## ⚠️ Scope Boundary: No Background Execution

> **This is a deliberate design choice, not a limitation.**

Sync in Kmos only runs while the app process is alive:

| ✅ Supported | ❌ Not Supported |
|-------------|-----------------|
| Foreground sync | Background sync when app is killed |
| Manual trigger | OS-level scheduled sync |
| In-process interval | WorkManager / BGTaskScheduler |

**Why?**

- **WorkManager** (Android) has no equivalent on JVM/Desktop
- **BGTaskScheduler** (iOS) is budget-limited and unreliable by design
- **JVM/Desktop** has no OS-level background scheduler

**Need "sync while closed"?** Use server-push (silent notifications) to trigger foreground sync.

---

## 📚 Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Room 3](https://developer.android.com/jetpack/androidx/releases/room)
- [Ktor](https://ktor.io/)
- [Koin](https://insert-koin.io/)

---

## 📄 License

License TBD.

---

**Built with ❤️ using Kotlin Multiplatform**
