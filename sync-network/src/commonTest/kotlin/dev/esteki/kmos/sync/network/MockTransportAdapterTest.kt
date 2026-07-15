package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.testing.MockTransportAdapter
import dev.esteki.kmos.sync.testing.TransportAdapterContractTest

class MockTransportAdapterTest : TransportAdapterContractTest() {
    override fun createAdapter(): TransportAdapter = MockTransportAdapter()
}
