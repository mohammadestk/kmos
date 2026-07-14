# Kmos

### Kotlin Multiplatform Offline-First Sync SDK

**Reliable synchronization. One codebase. Six platforms.**

> **Status: In Progress** — This SDK is actively under development. APIs may change.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-purple?logo=kotlin)](https://kotlinlang.org)
[![KMP](https://img.shields.io/badge/KMP-All%20Targets-blue)](https://www.jetbrains.com/kotlin-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green)](#license)
[![JitPack](https://jitpack.io/v/mohammadestk/Kmos.svg)](https://jitpack.io/#mohammadestk/Kmos)

---

## What is Kmos?

Kmos is a **Kotlin Multiplatform SDK** for building offline-first applications with reliable, correct synchronization. Write your sync logic once in `commonMain` — it runs everywhere.

```
┌──────────────────────────────────────────────────────────────────┐
│                           YOUR APP                               │
├──────────────────────────────────────────────────────────────────┤
│                      SyncRepository<T>                           │
│                       (typed APIs)                               │
├──────────────────────────────────────────────────────────────────┤
│                        Sync Engine                               │
│            ┌─────────────┼─────────────┐                         │
│            ▼             ▼             ▼                         │
│     Operation Queue  Retry Policy  Conflict Resolver             │
│      (idempotent)   (backoff+jitter) (LWW/Custom)                │
├──────────────────────────────────────────────────────────────────┤
│     StorageAdapter                TransportAdapter               │
│        (Room 3)                       (Ktor)                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## Features

| Feature | Description |
|---------|-------------|
| **Offline-First** | Sync runs on foreground, manual trigger, or optional interval |
| **6 Platforms** | Android, iOS, JVM, Desktop, JS, WasmJS — one implementation |
| **Thread-Safe** | Single-writer Channel-driven architecture — no locks needed |
| **Idempotent Retry** | Exponential backoff with jitter, dead-letter path |
| **Conflict Resolution** | Last-Write-Wins default, custom resolver support |
| **Pluggable** | Storage & transport adapters with contract test suites |
| **Room 3** | KMP-native storage reference implementation |
| **Koin DI** | Ready-to-use dependency injection module |

---

## Quick Start

### 1. Add Dependency

```kotlin
// build.gradle.kts
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.mohammadestk.Kmos:syncCore:Tag")
    implementation("com.github.mohammadestk.Kmos:syncStorage:Tag")
    implementation("com.github.mohammadestk.Kmos:syncNetwork:Tag")
    implementation("com.github.mohammadestk.Kmos:syncTrigger:Tag")
}
```

Replace `Tag` with the desired version tag (e.g., `v0.1.0`).

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

## Architecture

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

## Configuration

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

## Development

### Build Commands

```bash
# Android
./gradlew :sample:androidApp:assembleDebug

# Desktop (JVM)
./gradlew :sample:desktopApp:run

# Web (Wasm — modern browsers)
./gradlew :sample:webApp:wasmJsBrowserDevelopmentRun

# Web (JS — older browsers)
./gradlew :sample:webApp:jsBrowserDevelopmentRun

# iOS — open sample/iosApp/ in Xcode
```

### Test Commands

```bash
# Core engine tests
./gradlew :syncCore:jvmTest

# Trigger tests
./gradlew :syncTrigger:jvmTest

# Storage compilation
./gradlew :syncStorage:compileKotlinJvm

# Network compilation
./gradlew :syncNetwork:compileKotlinJvm

# Sample app tests
./gradlew :sample:jvmTest
```

---

## Project Structure

```
kmos/
├── syncCore/                    # Core engine, interfaces, models
├── syncStorage/                 # Room 3 storage adapter
├── syncNetwork/                 # Ktor transport adapter
├── syncTrigger/                 # Lifecycle hooks and trigger management
├── syncTesting/                 # Test utilities (not published)
├── sample/                      # Demo apps
│   ├── shared/                  # Shared UI code
│   ├── androidApp/              # Android app
│   ├── desktopApp/              # Desktop app
│   ├── webApp/                  # Web app
│   └── iosApp/                  # iOS app
├── specs/                       # Design specifications
└── gradle/                      # Build configuration
```

---

## Scope Boundary: No Background Execution

> **This is a deliberate design choice, not a limitation.**

Sync in Kmos only runs while the app process is alive:

| Supported | Not Supported |
|-----------|---------------|
| Foreground sync | Background sync when app is killed |
| Manual trigger | OS-level scheduled sync |
| In-process interval | WorkManager / BGTaskScheduler |

**Why?**

- **WorkManager** (Android) has no equivalent on JVM/Desktop
- **BGTaskScheduler** (iOS) is budget-limited and unreliable by design
- **JVM/Desktop** has no OS-level background scheduler

**Need "sync while closed"?** Use server-push (silent notifications) to trigger foreground sync.

---

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Room 3](https://developer.android.com/jetpack/androidx/releases/room)
- [Ktor](https://ktor.io/)
- [Koin](https://insert-koin.io/)

---

## License

```
Copyright 2025 Mohammad Esteki

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
