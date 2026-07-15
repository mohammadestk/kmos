package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.model.SyncOperation

data class SyncEndpoints(
    val pushUrl: (SyncOperation) -> String = { op ->
        when (op.type) {
            dev.esteki.kmos.sync.core.model.OperationType.Create -> "/objects"
            dev.esteki.kmos.sync.core.model.OperationType.Update -> "/objects/${op.entityId}"
            dev.esteki.kmos.sync.core.model.OperationType.Delete -> "/objects/${op.entityId}"
        }
    },
    val pullUrl: (String?) -> String = { "/objects" },
    val singleEntityUrl: (String) -> String = { id -> "/objects/$id" },
)
