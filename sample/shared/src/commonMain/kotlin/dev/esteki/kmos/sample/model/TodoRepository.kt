package dev.esteki.kmos.sample.model

import dev.esteki.kmos.sync.core.SyncClient
import dev.esteki.kmos.sync.core.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

class TodoRepository(private val syncClient: SyncClient) {

    private val _items = MutableStateFlow<List<TodoItem>>(emptyList())

    private val repository: SyncRepository<TodoItem> = syncClient.repository(
        observe = { id ->
            _items.map { list -> list.find { it.id == id } }
        },
        observeAll = {
            _items
        },
        upsert = { item ->
            _items.update { current ->
                val index = current.indexOfFirst { it.id == item.id }
                if (index >= 0) {
                    current.toMutableList().apply { set(index, item) }
                } else {
                    current + item
                }
            }
        },
        delete = { id ->
            _items.update { current -> current.filter { it.id != id } }
        },
    )

    fun observeAll(): Flow<List<TodoItem>> = _items

    fun observe(id: String): Flow<TodoItem?> = _items.map { list -> list.find { it.id == id } }

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
