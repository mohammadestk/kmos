# Kmos

Kotlin Multiplatform offline-first sync SDK. Single-writer Channel-driven engine with idempotent retry, conflict resolution, and pluggable storage/transport — all in commonMain.

## Features

- **Offline-first by default** — sync runs on foreground, manual trigger, or optional in-process interval
- **Single implementation** — Android, iOS, JVM, Desktop, JS, WasmJS from one codebase
- **Single-writer concurrency** — Channel-driven command queue, no locks, no platform-specific threading
- **Idempotent retry** — exponential backoff with jitter, dead-letter path, failure-driven (no connectivity API needed)
- **Conflict resolution** — Last-Write-Wins default, custom `ConflictResolver<T>` callback
- **Pluggable adapters** — `StorageAdapter` and `TransportAdapter` interfaces with contract test suites
- **Room 3 storage** — reference implementation backed by Room 3 (KMP-native)
- **Koin DI** — ready-to-use dependency injection module

## Supported Platforms

| Platform | Target |
|----------|--------|
| Android | Library (minSdk 24) |
| iOS | iosArm64, iosSimulatorArm64 |
| Desktop | JVM |
| Web | JS, WasmJS |

## Quick Start

### Setup

```kotlin
// Initialize with Koin
startKoin {
    modules(syncModule(databaseName = "myapp.db"))
}

// Or use the builder directly
val client = SyncClient.build(scope) {
    storage(RoomStorageAdapter(database))
    transport(KtorTransportAdapter())
    retry(ExponentialBackoffRetryPolicy())
}
```

### Using SyncRepository

```kotlin
val tasks: SyncRepository<Task> = client.repository()

// Observe data
tasks.observeAll().collect { taskList ->
    // render UI
}

// Update data
tasks.upsert(updatedTask)

// Manual sync
client.trigger()

// Monitor failed operations
client.failedOperations().collect { failed ->
    // show retry UI
}
```

## Architecture

```
App
 │
 Public API (typed repositories)
 │
 SyncRepository<T>
 │
 Sync Engine (single-writer, Channel-driven command queue)
 ├── Operation Queue (persisted, idempotency-keyed)
 ├── Retry Policy (exponential backoff + jitter, dead-letter)
 ├── Conflict Resolver (LWW | Custom callback)
 └── Sync Trigger (foreground, manual, interval)
 │
 StorageAdapter (Room 3)
 │
 TransportAdapter (Ktor)
```

### Key Design Decisions

- **Single-writer concurrency** — all mutations enter a `Channel<SyncCommand>`, consumed by one coroutine. No locks.
- **No network monitor** — failures drive retry directly. A device reporting "connected" with no internet is not something platform APIs reliably detect.
- **No background execution** — WorkManager, BGTaskScheduler, and OS schedulers are deliberately excluded. See [Scope Boundary](#scope-boundary-no-background-execution) below.

## Configuration

### RetryPolicy

```kotlin
ExponentialBackoffRetryPolicy(
    baseDelay = 1000.milliseconds,
    maxDelay = 60_000.milliseconds,
    maxAttempts = 5,
    jitterFactor = 0.3,
)
```

### ConflictResolver

```kotlin
// Default: Last-Write-Wins
LastWriteWinsConflictResolver()

// Custom resolver
ConflictResolver<MyEntity> { local, remote ->
    // your merge logic
}
```

## Building

```bash
# Android
./gradlew :androidApp:assembleDebug

# Desktop (JVM)
./gradlew :desktopApp:run

# Web (Wasm)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Web (JS)
./gradlew :webApp:jsBrowserDevelopmentRun

# iOS — open iosApp/ in Xcode
```

## Testing

```bash
# Android
./gradlew :shared:testAndroidHostTest

# Desktop (JVM)
./gradlew :shared:jvmTest

# Web (Wasm)
./gradlew :shared:wasmJsTest

# Web (JS)
./gradlew :shared:jsTest

# iOS simulator
./gradlew :shared:iosSimulatorArm64Test
```

## Project Structure

```
shared/src/
  commonMain/    SDK core (interfaces + engine)
  commonTest/    contract tests, unit tests
  androidMain/   Android actuals (Room bootstrap)
  iosMain/       iOS actuals
  jvmMain/       JVM/Desktop actuals
  jsMain/        JS/Web actuals
  wasmJsMain/    Wasm/Web actuals
```

## Scope Boundary: No Background Execution

Sync in this SDK only runs while the app process is alive: on foreground, on manual trigger, or on an optional in-process interval. It does **not** wake the app to sync when killed or long-backgrounded.

This is intentional:
- **WorkManager** (Android) has no equivalent on JVM/Desktop
- **BGTaskScheduler** (iOS) is budget-limited and OS-scheduled at Apple's discretion
- **JVM/Desktop** has no OS-level background task scheduler

Apps that need "sync while closed" should pair this SDK with server-push (silent push notification triggering a foreground sync).

## License

License TBD.
