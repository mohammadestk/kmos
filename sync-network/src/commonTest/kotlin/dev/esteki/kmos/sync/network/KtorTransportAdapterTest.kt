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
import kotlinx.serialization.json.Json

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
}
