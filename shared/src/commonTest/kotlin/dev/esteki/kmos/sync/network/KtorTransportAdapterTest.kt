package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.testing.TransportAdapterContractTest

class KtorTransportAdapterTest : TransportAdapterContractTest() {
    override fun createAdapter(): TransportAdapter = KtorTransportAdapter()
}
