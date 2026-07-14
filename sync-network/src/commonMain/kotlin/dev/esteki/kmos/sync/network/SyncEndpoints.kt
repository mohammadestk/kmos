package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.model.SyncOperation

data class SyncEndpoints(
    val pushUrl: (SyncOperation) -> String = { op ->
        when (op.type) {
            dev.esteki.kmos.sync.core.model.OperationType.Create -> "/sync/push"
            dev.esteki.kmos.sync.core.model.OperationType.Update -> "/sync/push/${op.entityId}"
            dev.esteki.kmos.sync.core.model.OperationType.Delete -> "/sync/push/${op.entityId}"
        }
    },
    val pullUrl: (String?) -> String = { cursor ->
        buildString {
            append("/sync/pull")
            if (cursor != null) {
                append("?cursor=$cursor")
            }
        }
    },
    val singleEntityUrl: (String) -> String = { id -> "/sync/entity/$id" },
)
