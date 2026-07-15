package dev.esteki.kmos.sample.model

import dev.esteki.kmos.sync.core.SyncMapper
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

@Serializable
data class TodoItem(
    val id: String,
    val title: String,
    val completed: Boolean = false,
    val createdAt: Long = 0L,
)

private val json = Json { ignoreUnknownKeys = true }

object TodoItemMapper : SyncMapper<TodoItem> {
    override fun toSyncEntity(value: TodoItem): SyncEntity {
        return SyncEntity(
            id = value.id,
            version = 0L,
            updatedAt = Instant.fromEpochMilliseconds(value.createdAt),
            deleted = false,
            syncState = SyncState.PendingUpload,
            payload = json.encodeToString(value).toByteArray(),
        )
    }

    override fun fromSyncEntity(entity: SyncEntity): TodoItem {
        return json.decodeFromString<TodoItem>(entity.payload.decodeToString())
    }
}
