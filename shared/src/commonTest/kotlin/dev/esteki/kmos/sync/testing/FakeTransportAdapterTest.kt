package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.TransportAdapter

class FakeTransportAdapterTest : TransportAdapterContractTest() {
    override fun createAdapter(): TransportAdapter = FakeTransportAdapter()
}
