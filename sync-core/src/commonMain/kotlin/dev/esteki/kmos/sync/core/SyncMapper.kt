package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncEntity

interface SyncMapper<T> {
    fun toSyncEntity(value: T): SyncEntity
    fun fromSyncEntity(entity: SyncEntity): T
}
