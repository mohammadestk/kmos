package dev.esteki.kmos.model

import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class TodoItem(
    val id: String,
    val title: String,
    val completed: Boolean = false,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
)

private val json = Json { ignoreUnknownKeys = true }

fun TodoItem.toSyncEntity(syncState: SyncState = SyncState.PendingUpload): SyncEntity {
    return SyncEntity(
        id = id,
        version = 0L,
        updatedAt = Instant.fromEpochMilliseconds(createdAt),
        deleted = false,
        syncState = syncState,
        payload = json.encodeToString(this).toByteArray(),
    )
}

fun SyncEntity.toTodoItem(): TodoItem {
    return json.decodeFromString<TodoItem>(payload.decodeToString())
}
