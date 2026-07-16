package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncState
import dev.esteki.kmos.sync.testing.TransportAdapterContractTest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class KtorTransportAdapterTest : TransportAdapterContractTest() {

    private val testProtocol = TestSyncApiProtocol()

    override fun createAdapter(): TransportAdapter {
        val mockEngine = MockEngine { request ->
            val method = request.method.value
            val path = request.url.encodedPath
            val jsonHeaders = headersOf(ContentType.Application.Json.toString(), "application/json")

            when {
                method == "GET" && path == "/objects" -> {
                    respond(
                        content = """{"entities":[{"id":"1","version":1,"deleted":false,"payload":{"key":"value"}}],"nextCursor":null}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }

                method == "POST" && path == "/objects" -> {
                    respond(
                        content = """{"version":1000}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }

                method == "PUT" && path.startsWith("/objects/") -> {
                    respond(
                        content = """{"version":2000}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }

                method == "DELETE" && path.startsWith("/objects/") -> {
                    respond(
                        content = """{"version":3000}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }

                else -> {
                    respond(
                        content = """{"error":"not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = jsonHeaders,
                    )
                }
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
        return KtorTransportAdapter(httpClient, "https://test.example.com", testProtocol)
    }

    @Test
    fun testPushReturnsSuccess() = runTest { pushReturnsSuccess() }

    @Test
    fun testPushReturnsConflict() = runTest { pushReturnsConflict() }

    @Test
    fun testPullReturnsEntities() = runTest {
        val adapter = createAdapter()
        val result = adapter.pull(null)
        assertEquals(1, result.entities.size)
    }

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

private class TestSyncApiProtocol : SyncApiProtocol {
    override fun pushUrl(op: SyncOperation): String = when (op.type) {
        OperationType.Create -> "/objects"
        OperationType.Update -> "/objects/${op.entityId}"
        OperationType.Delete -> "/objects/${op.entityId}"
    }

    override fun pullUrl(cursor: String?): String = "/objects"

    override suspend fun buildPushRequest(op: SyncOperation): HttpRequestBuilder.() -> Unit = {}

    override suspend fun parsePushResponse(response: HttpResponse): PushResult {
        return try {
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val version = Json.decodeFromString<VersionResponse>(body).version
                PushResult.Success(version = version)
            } else {
                PushResult.Error(SyncError.ServerError(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            PushResult.Error(SyncError.Unknown(e))
        }
    }

    override suspend fun parsePullResponse(response: HttpResponse): PullResult {
        return try {
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val pullResponse = Json.decodeFromString<PullResponse>(body)
                PullResult(
                    entities = pullResponse.entities.map { it.toSyncEntity() },
                    nextCursor = pullResponse.nextCursor,
                )
            } else {
                PullResult(entities = emptyList(), nextCursor = null)
            }
        } catch (e: Exception) {
            PullResult(entities = emptyList(), nextCursor = null)
        }
    }

    private fun RemoteEntity.toSyncEntity() = SyncEntity(
        id = id,
        version = version,
        updatedAt = Clock.System.now(),
        deleted = deleted,
        syncState = SyncState.Synced,
        payload = Json.encodeToString(JsonElement.serializer(), payload).encodeToByteArray(),
    )
}

@Serializable
private data class VersionResponse(val version: Long)

@Serializable
private data class PullResponse(
    val entities: List<RemoteEntity>,
    val nextCursor: String? = null,
)

@Serializable
private data class RemoteEntity(
    val id: String,
    val version: Long,
    val deleted: Boolean = false,
    val payload: JsonElement,
)
