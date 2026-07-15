package dev.esteki.kmos.sample.model

import dev.esteki.kmos.sync.core.SyncClient
import dev.esteki.kmos.sync.core.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

class TodoRepository(syncClient: SyncClient) {

    private val repository: SyncRepository<TodoItem> = syncClient.repository(TodoItemMapper)

    fun observeAll(): Flow<List<TodoItem>> = repository.observeAll()

    suspend fun observe(id: String): TodoItem? = repository.read(id)

    suspend fun add(title: String): TodoItem {
        val item = TodoItem(
            id = Uuid.random().toString(),
            title = title,
        )
        repository.upsert(item)
        return item
    }

    suspend fun toggleComplete(item: TodoItem) {
        val updated = item.copy(completed = !item.completed)
        repository.upsert(updated)
    }

    suspend fun update(item: TodoItem) {
        repository.upsert(item)
    }

    suspend fun delete(item: TodoItem) {
        repository.delete(item.id)
    }
}
