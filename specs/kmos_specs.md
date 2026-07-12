# Kotlin Multiplatform Offline Sync SDK — Spec

## Vision

A Kotlin Multiplatform SDK for offline-first apps with reliable, correct
synchronization, implemented entirely in commonMain — no platform-specific
integration beyond what Room 3's bundled SQLite driver already requires.
This is a deliberate design constraint, not a limitation: it forces every
hard problem (conflict resolution, idempotent retry, adapter contracts) to
be solved once, in shared code, rather than reimplemented and re-tested
per platform.

## Design Principle

Every mechanism in this spec must be concrete enough to implement without
further design decisions. If a bullet can't be turned into an interface or
an algorithm today, it doesn't belong in scope yet — it goes in
"Deferred / Future Work."

## Goals (v1)

- Offline-first by default
- Fully common-code core: Android, iOS, JVM, Desktop from a single
  implementation, no expect/actual platform glue beyond Room 3 bootstrap
- Pluggable storage and transport, enforced by contract tests
- Correct retry semantics (idempotent, bounded, observable, driven by
  failure — not by platform connectivity APIs)
- Coroutine and Flow based APIs
- High testability (target coverage is a *result* of testing conflict
  and retry logic exhaustively, not a goal in itself)

## Non-goals (v1)

- Authentication provider
- UI framework
- Delta/changefeed sync (requires a server-side contract — see below)
- Field-level / CRDT merge conflict resolution
- At-rest encryption
- **Guaranteed background execution when the app process is not
  running.** This is a deliberate scope boundary, not an oversight: OS
  background schedulers (WorkManager, BGTaskScheduler) are per-platform,
  unreliable on iOS by design, and nonexistent on JVM/Desktop. Building
  around them would reintroduce exactly the per-platform effort this SDK
  is designed to avoid, in exchange for a guarantee iOS can't actually
  give you anyway. Sync runs on app foreground, on explicit trigger, and
  on an optional in-process interval while the app is alive.

## Explicit Contradiction Resolved: Delta Sync vs. "No Backend"

The original spec listed delta sync as a requirement while declaring
backend implementation out of scope. Delta sync requires a cursor/changefeed
protocol both sides agree on — it cannot be built without *some* backend
contract. Resolution: v1 ships batch sync with cursor-based pagination
against a documented (but unimplemented) minimal protocol. Delta sync is
deferred until the protocol is worth formalizing.

## Supported Platforms (v1)

- Android
- iOS
- JVM
- Desktop

All four from one commonMain implementation. If it compiles and passes
the shared contract test suite on a target, that target is supported —
no per-platform proving phase, no per-platform glue code to write.

## High-level Architecture

```text
App
 │
 Public API (typed repositories)
 │
 SyncRepository<T>
 │
 Sync Engine (single-writer, Channel-driven command queue)
 ├── Operation Queue (persisted, idempotency-keyed)
 ├── Retry Policy (exponential backoff + jitter, dead-letter,
 │                 triggered by failure — no connectivity API needed)
 ├── Conflict Resolver (LWW | Custom callback)
 └── Sync Trigger (app-foreground hook, manual trigger,
                    optional in-process interval — all commonMain)
 │
 StorageAdapter (contract-tested)
 │
 TransportAdapter (contract-tested)
 │
 Backend (protocol documented, not implemented)
```

**Concurrency model**: the sync engine is single-writer. All mutations
enter through a `Channel<SyncCommand>`; there is exactly one coroutine
consuming it per `SyncClient` instance. This sidesteps cross-platform
differences in shared-mutable-state semantics (JVM vs. Kotlin/Native)
instead of relying on locks that behave differently per platform.

**Why no Network Monitor node**: watching for connectivity changes via
platform APIs (`ConnectivityManager`, `NWPathMonitor`) is itself a
per-platform integration, and a flaky signal even where it exists (a
device can report "connected" to a Wi-Fi network with no internet route).
Instead, the engine just attempts the operation; a network-class failure
triggers the same backoff/retry path any other transient failure does.
This is simpler, fully common-code, and no less correct in practice.

## Modules

### sync-core
Interfaces, models, sync engine, operation queue, retry policy.

### sync-storage
`StorageAdapter` interface + contract test suite. Reference impl:
Room/SQLDelight adapter.

### sync-network
`TransportAdapter` interface + contract test suite. Reference impl:
Ktor-based HTTP/WebSocket adapter.

### sync-trigger
App-lifecycle hook (foreground/resume), manual `trigger()`, and an
optional in-process periodic loop while the process is alive. Fully
commonMain — no WorkManager, no BGTaskScheduler, no OS scheduler
integration. Does not guarantee sync while the app is killed or
long-backgrounded; this is a documented scope boundary (see Non-goals).

### sync-testing
Fake `StorageAdapter`/`TransportAdapter` implementations, contract test
base classes, deterministic clock/network-condition injection for testing
retry and conflict paths.

*(sync-encryption, sync-compose: deferred — see Future Work)*

## Core Interfaces

```kotlin
interface SyncRepository<T> {
    fun observe(id: String): Flow<T?>
    fun observeAll(): Flow<List<T>>
    suspend fun upsert(value: T)
    suspend fun delete(id: String)
}

interface StorageAdapter {
    suspend fun read(id: String): SyncEntity?
    suspend fun write(entity: SyncEntity)
    suspend fun queryPending(): List<SyncEntity>
}

interface TransportAdapter {
    suspend fun push(op: SyncOperation): PushResult
    suspend fun pull(cursor: String?): PullResult
}

interface ConflictResolver<T> {
    fun resolve(local: T, remote: T): T
}

interface RetryPolicy {
    fun nextDelay(attempt: Int): Duration
    fun shouldDeadLetter(attempt: Int, lastError: SyncError): Boolean
}
```

## Data Model

```kotlin
data class SyncEntity(
    val id: String,
    val version: Long,
    val updatedAt: Instant,
    val deleted: Boolean,
    val syncState: SyncState,
    val payload: ByteArray,        // kotlinx.serialization-encoded
)

data class SyncOperation(
    val operationId: String,       // idempotency key — required for
                                    // every push, not optional
    val entityId: String,
    val type: OperationType,       // Create | Update | Delete
    val attempt: Int,
    val payload: ByteArray,
)
```

**Idempotency**: every push carries an `operationId`. The (unimplemented,
documented) backend contract requires the server to dedupe on this key.
Without it, retry-after-ambiguous-failure produces duplicate writes — this
is non-negotiable for v1, not a nice-to-have.

## Sync States

- LocalOnly
- PendingUpload
- Synced
- Conflict
- Failed *(with an explicit unstick path — see Retry below)*

## Sync Flow

1. User updates data → persisted locally, `syncState = PendingUpload`.
2. Operation enqueued with a fresh `operationId`.
3. Sync Trigger fires (foreground, manual, or interval) → engine drains
   queue and attempts each pending operation.
4. Push via TransportAdapter.
5. On success: `syncState = Synced`.
6. On version conflict: `syncState = Conflict` → ConflictResolver invoked
   (LWW or app-supplied custom callback for v1).
7. On network-class or other transient failure: RetryPolicy decides
   backoff or dead-letter (`syncState = Failed`) — no separate
   connectivity check needed, the failure itself is the signal.
8. **Failed entities are not a black hole**: exposed via
   `SyncClient.failedOperations(): Flow<List<SyncOperation>>` with an
   explicit `retry()` / `discard()` API for the app to call.

## Conflict Strategy (v1)

- **Last Write Wins** (default, uses `updatedAt`)
- **Custom** — app supplies a `ConflictResolver<T>` callback

*Version-based rejection, field-level merge, and CRDT-based merge are
deferred — see Future Work. They require either schema-aware diffing or
vector clocks, neither of which the current scalar `version` field
supports. Do not claim "Merge" as a v1 feature.*

## Retry Policy

- Exponential backoff with jitter, capped at a configurable max delay.
- Configurable max-attempt threshold → transitions to `Failed`
  (dead-letter), not silent infinite retry.
- Retry is always idempotency-key-safe (see Data Model).

## Requirements

### Functional (v1)
- CRUD support
- Batch sync with cursor-based pagination
- Idempotent retry
- Cancellation
- Progress events
- Explicit failed-operation recovery API

### Functional (deferred)
- Delta/changefeed sync
- Field-level or CRDT merge conflict resolution
- At-rest encryption

### Non-functional
- Single-writer concurrency model (see Architecture)
- Fully commonMain: Android, iOS, JVM, Desktop from one implementation,
  no platform-specific glue beyond Room 3 bootstrap
- Contract test suite required for every adapter implementation
  (storage and transport) — no adapter ships without passing it, and
  the same suite runs unmodified on every target
- Modular, DI-friendly

## Public API Example

```kotlin
val client = SyncClient {
    storage(RoomAdapter())
    transport(KtorAdapter())
    conflictResolver(LastWriteWins)
    syncOnForeground(true)        // commonMain lifecycle hook
    syncInterval(5.minutes)       // optional, only while process is alive
}

val tasks: SyncRepository<Task> = client.repository()

tasks.observeAll().collect { /* render */ }
tasks.upsert(updatedTask)

client.trigger()  // manual sync, e.g. pull-to-refresh

client.failedOperations().collect { failed ->
    // surface to user, offer retry
}
```

## Scope Boundary: No Background Execution (read this before filing an issue)

Sync in this SDK only runs while the app process is alive: on foreground,
on manual trigger, or on an optional in-process interval. It does **not**
wake the app to sync when killed or long-backgrounded.

This is intentional, not a gap to be filled later:

- WorkManager (Android) is genuinely reliable, but has no equivalent on
  JVM/Desktop and only a degraded analog on iOS.
- BGTaskScheduler (iOS) is budget-limited and OS-scheduled at Apple's
  discretion — "background sync" on iOS is already best-effort even in
  SDKs that integrate it, so depending on it buys less guarantee than it
  appears to.
- JVM/Desktop has no OS-level background task scheduler at all.

Building per-platform background integration would reintroduce the exact
per-target effort this SDK exists to avoid, in exchange for a guarantee
that's inconsistent across targets anyway. Apps that need "sync while
closed" should pair this SDK with server-push (silent push notification
triggering a foreground-ish sync) — that's an application-level decision
outside this SDK's scope, not something the SDK should fake internally.

## Roadmap

### v0.1
- Core engine (single-writer, Channel-driven)
- Operation queue with idempotency keys
- Retry policy with dead-letter path, failure-driven (no connectivity API)
- Storage + Transport adapter interfaces with contract test suite
- sync-trigger (foreground hook + manual trigger)
- Sample app running on all four targets from shared code

### v0.5
- LWW + custom conflict resolver
- Contract test suite green on Android, iOS, JVM, Desktop simultaneously
  (a target failing the shared suite is a bug, not a "not yet supported")
- Optional in-process sync interval

### v1.0
- Stable public API
- Documentation, including the background-execution scope boundary
  front and center in the README
- Benchmarks (throughput under queued-offline-then-reconnect scenarios,
  not just unit test coverage)
- Two adapter implementations per interface (proves the abstraction
  isn't fitted to one implementation)

## Future Work (explicitly deferred, not forgotten)

- Delta/changefeed sync (requires formalizing the backend protocol)
- Field-level or CRDT-based merge conflict resolution
- At-rest encryption (payload-level encryption via a KMP crypto library
  is the fully-common-code path — avoid SQLCipher-style whole-DB
  encryption, which reintroduces per-platform build complexity)
- Compose Multiplatform helpers (sync-compose) — genuinely common code
  across all four targets, low risk, just not core-path priority
- Server-push-triggered sync as an application-level pattern layered on
  top of this SDK, documented but not implemented by it
