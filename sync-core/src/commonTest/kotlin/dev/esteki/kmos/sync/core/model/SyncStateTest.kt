package dev.esteki.kmos.sync.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncStateTest {

    @Test
    fun allStatesExist() {
        val states = SyncState.entries
        assertEquals(5, states.size)
        assertEquals(SyncState.LocalOnly, SyncState.valueOf("LocalOnly"))
        assertEquals(SyncState.PendingUpload, SyncState.valueOf("PendingUpload"))
        assertEquals(SyncState.Synced, SyncState.valueOf("Synced"))
        assertEquals(SyncState.Conflict, SyncState.valueOf("Conflict"))
        assertEquals(SyncState.Failed, SyncState.valueOf("Failed"))
    }
}
