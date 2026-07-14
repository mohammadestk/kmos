# AGENTS.md

## What This Is

Kotlin Multiplatform Offline Sync SDK. The spec (`specs/kmos_specs.md`) is the source of truth for all design decisions — read it before implementing anything.

## Build & Run

```bash
# Android
./gradlew :androidApp:assembleDebug

# Desktop (JVM)
./gradlew :desktopApp:run

# Web (Wasm — faster, modern browsers)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Web (JS — older browsers)
./gradlew :webApp:jsBrowserDevelopmentRun

# iOS — open sample/iosApp/ in Xcode, run from there
```

## Test Commands

Run all tests across modules:

```bash
./gradlew :sync-core:jvmTest               # Core engine tests
./gradlew :sync-trigger:jvmTest            # Trigger tests
./gradlew :sync-storage:compileKotlinJvm   # Storage compilation
./gradlew :sync-network:compileKotlinJvm   # Network compilation
./gradlew :sample:jvmTest                  # Sample app tests
```

iOS tests require an Xcode-managed simulator. If `iosSimulatorArm64Test` fails with a simulator not found error, create one via `xcrun simctl create`.

## Architecture (non-obvious)

- **All sync engine code lives in `commonMain`** of each module. No expect/actual platform glue beyond Room 3's bundled SQLite driver. This is a hard constraint — the whole point is solving every hard problem once in shared code.
- **Single-writer concurrency**. All mutations enter a `Channel<SyncCommand>` consumed by one coroutine per `SyncClient` instance. No locks — sidesteps JVM vs. Kotlin/Native shared-mutable-state differences.
- **No network monitor node**. Failures drive retry directly. A device reporting "connected" to Wi-Fi with no internet is not something platform APIs reliably detect anyway.
- **No background execution**. WorkManager, BGTaskScheduler, and OS schedulers are deliberately excluded. Sync runs on foreground, manual trigger, and optional in-process interval while the process is alive.
- **Every push requires an `operationId`**. Idempotency key is mandatory, not optional. Without it, retry-after-ambiguous-failure produces duplicate writes.

## Modules

```
SDK Modules (core library):
sync-core        interfaces, models, engine, operation queue, retry policy
sync-storage     StorageAdapter + Room reference impl, DatabaseFactory expect/actuals
sync-network     TransportAdapter + Ktor reference impl
sync-trigger     lifecycle hooks, manual trigger, optional in-process interval
sync-testing     fake adapters, contract test base classes, deterministic clocks

Sample App:
sample/         demo app module (App.kt, Platform.kt, SyncClientBuilder.kt, SyncModule.kt)
sample/androidApp/    Android app shell
sample/desktopApp/    Desktop app shell
sample/webApp/        Web app shell
sample/iosApp/        iOS app (Xcode project)
```

### Module Dependencies

```
sync-core          (no SDK deps)
sync-trigger       → sync-core
sync-testing       → sync-core
sync-storage       → sync-core (+ Room3, KSP)
sync-network       → sync-core (+ Ktor)
sample             → sync-core, sync-storage, sync-network, sync-trigger (+ Compose, Koin)
sample/androidApp  → sample
sample/desktopApp  → sample
sample/webApp      → sample
```

Conflict resolution: LWW (default, uses `updatedAt`) or app-supplied `ConflictResolver<T>` callback.

## Source Set Layout

Each SDK module follows this structure:
```
<module>/src/
  commonMain/    Shared code (interfaces, implementations)
  commonTest/    Tests (if any)
  androidMain/   Android actuals (Room bootstrap only for sync-storage)
  iosMain/       iOS actuals
  jvmMain/       JVM/Desktop actuals
  jsMain/        JS/Web actuals
  wasmJsMain/    Wasm/Web actuals
  webMain/       Web shared code (if any)
```

## Toolchain

- Gradle 9.4.1, Kotlin 2.4.0, Compose Multiplatform 1.11.1, AGP 9.2.1
- JDK 21 (Amazon Corretto — see `gradle-daemon-jvm.properties`)
- Configuration cache + build caching enabled
- Version catalog: `gradle/libs.versions.toml`
- Typesafe project accessors: use `projects.syncCore`, not `":sync-core"`

## Conventions

- Package: `dev.esteki.kmos`
- Conventional commits (feat:, fix:, refactor:, etc.)
- DI: Koin
- Serialization: kotlinx.serialization
- Transport: Ktor
- No linting/formatting tools configured yet (`kotlin.code.style=official` only)
- No convention plugins, no buildSrc

## Don't

- Don't put sync engine logic in platform source sets — it belongs in `commonMain`
- Don't add WorkManager or BGTaskScheduler — it's a deliberate exclusion, not an oversight
- Don't add ProGuard/R8 — minification is disabled
- Don't skip the `operationId` field in `SyncOperation` — idempotency is non-negotiable
- Don't claim features from the spec are "implemented" without passing the contract test suite
