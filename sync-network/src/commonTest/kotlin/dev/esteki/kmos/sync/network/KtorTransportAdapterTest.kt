package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.testing.TransportAdapterContractTest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test

class KtorTransportAdapterTest : TransportAdapterContractTest() {
    override fun createAdapter(): TransportAdapter {
        val mockEngine = MockEngine { request ->
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(ContentType.Application.Json.toString(), "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
        return KtorTransportAdapter(httpClient, "https://test.example.com")
    }

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
