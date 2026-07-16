package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncOperation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse

/**
 * Defines the wire format for sync API communication.
 *
 * Implement this interface to define how your backend expects requests
 * to be formatted and how responses should be parsed.
 *
 * The [KtorTransportAdapter] uses this protocol to serialize requests
 * and deserialize responses, keeping the HTTP transport layer generic.
 *
 * Example implementation:
 * ```kotlin
 * class MyApiProtocol : SyncApiProtocol {
 *     override fun pushUrl(op: SyncOperation): String = when (op.type) {
 *         OperationType.Create -> "/api/v1/sync"
 *         OperationType.Update -> "/api/v1/sync/${op.entityId}"
 *         OperationType.Delete -> "/api/v1/sync/${op.entityId}"
 *     }
 *
 *     override fun pullUrl(cursor: String?): String = buildString {
 *         append("/api/v1/sync")
 *         if (cursor != null) append("?cursor=$cursor")
 *     }
 *
 *     override suspend fun buildPushRequest(op: SyncOperation): HttpRequestBuilder.() -> Unit = {
 *         setBody(MyPushRequest(op.entityId, op.type.name, op.payload.decodeToString()))
 *     }
 *
 *     override suspend fun parsePushResponse(response: HttpResponse): PushResult {
 *         // Parse based on status code and response body
 *     }
 *
 *     override suspend fun parsePullResponse(response: HttpResponse): PullResult {
 *         // Parse response body into entities and cursor
 *     }
 * }
 * ```
 */
interface SyncApiProtocol {
    /**
     * Returns the URL path for a push operation.
     *
     * @param op The sync operation being pushed
     * @return The URL path (e.g., "/api/sync/push")
     */
    fun pushUrl(op: SyncOperation): String

    /**
     * Returns the URL path for a pull operation.
     *
     * @param cursor Optional cursor for pagination
     * @return The URL path (e.g., "/api/sync/pull?cursor=abc")
     */
    fun pullUrl(cursor: String?): String

    /**
     * Builds the HTTP request for a push operation.
     *
     * Use this to set the request body, headers, or any other request configuration.
     *
     * @param op The sync operation being pushed
     * @return A lambda that configures the HttpRequestBuilder
     */
    suspend fun buildPushRequest(op: SyncOperation): HttpRequestBuilder.() -> Unit

    /**
     * Parses the HTTP response from a push operation.
     *
     * Implement this to extract the version from successful responses,
     * detect conflicts (e.g., 409 status), and handle errors.
     *
     * @param response The HTTP response from the server
     * @return PushResult indicating success, conflict, or error
     */
    suspend fun parsePushResponse(response: HttpResponse): PushResult

    /**
     * Parses the HTTP response from a pull operation.
     *
     * Implement this to extract entities and pagination cursor from the response.
     *
     * @param response The HTTP response from the server
     * @return PullResult containing entities and optional next cursor
     */
    suspend fun parsePullResponse(response: HttpResponse): PullResult
}
