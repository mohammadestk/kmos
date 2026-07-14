package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncEntity

class LastWriteWinsConflictResolver : ConflictResolver<SyncEntity> {

    override fun resolve(local: SyncEntity, remote: SyncEntity): SyncEntity {
        return if (local.updatedAt >= remote.updatedAt) local else remote
    }
}
