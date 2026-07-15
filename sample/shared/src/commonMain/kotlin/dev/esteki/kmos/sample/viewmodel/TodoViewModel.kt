package dev.esteki.kmos.sample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.esteki.kmos.sample.model.TodoItem
import dev.esteki.kmos.sample.model.TodoRepository
import dev.esteki.kmos.sync.core.SyncClient
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(
    private val syncClient: SyncClient,
    private val todoRepository: TodoRepository,
) : ViewModel() {

    val items: StateFlow<List<TodoItem>> = todoRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _syncProgress = MutableStateFlow<SyncProgress>(SyncProgress.Idle)
    val syncProgress: StateFlow<SyncProgress> = _syncProgress

    val failedOperations: StateFlow<List<SyncEntity>> = syncClient.failedOperations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncClient.start()
        viewModelScope.launch {
            syncClient.progress.collect { _syncProgress.value = it }
        }
    }

    fun addTodo(title: String) {
        viewModelScope.launch {
            todoRepository.add(title)
        }
    }

    fun toggleComplete(item: TodoItem) {
        viewModelScope.launch {
            todoRepository.toggleComplete(item)
        }
    }

    fun deleteTodo(item: TodoItem) {
        viewModelScope.launch {
            todoRepository.delete(item)
        }
    }

    fun syncNow() {
        syncClient.trigger()
    }

    fun retryFailed(entity: SyncEntity) {
        syncClient.retry(entity)
    }

    fun discardFailed(entity: SyncEntity) {
        syncClient.discard(entity)
    }

    override fun onCleared() {
        super.onCleared()
        syncClient.stop()
    }
}
