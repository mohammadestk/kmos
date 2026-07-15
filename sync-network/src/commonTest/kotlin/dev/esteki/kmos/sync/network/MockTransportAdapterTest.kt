package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.testing.MockTransportAdapter
import dev.esteki.kmos.sync.testing.TransportAdapterContractTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MockTransportAdapterTest : TransportAdapterContractTest() {
    override fun createAdapter(): TransportAdapter = MockTransportAdapter()

    @Test
    fun testPushReturnsSuccess() = runTest { pushReturnsSuccess() }

    @Test
    fun testPushReturnsConflict() = runTest { pushReturnsConflict() }

    @Test
    fun testPullReturnsEntities() = runTest { pullReturnsEntities() }

    @Test
    fun testPullReturnsNextCursor() = runTest { pullReturnsNextCursor() }

    @Test
    fun testPushWithCreateOperation() = runTest { pushWithCreateOperation() }

    @Test
    fun testPushWithUpdateOperation() = runTest { pushWithUpdateOperation() }

    @Test
    fun testPushWithDeleteOperation() = runTest { pushWithDeleteOperation() }

    @Test
    fun testPullWithCursorParameter() = runTest { pullWithCursorParameter() }
}
