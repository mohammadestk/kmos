# KMOS Sync Protocol

This document describes the expected server contract for the KMOS sync SDK's `TransportAdapter`.

## Overview

The protocol is a simple REST-based sync API with:
- **Push**: Client sends local changes to the server
- **Pull**: Client fetches remote changes with cursor-based pagination

All requests must include an `X-Idempotency-Key` header containing the operation's `operationId`. The server MUST deduplicate requests using this key.

## Endpoints

### POST /sync/push (Create)

Creates a new entity on the server.

**Request:**
```json
{
  "entityId": "task-123",
  "operationType": "Create",
  "operationId": "uuid-1",
  "payload": "base64-encoded-data"
}
```

**Headers:**
- `Content-Type: application/json`
- `X-Idempotency-Key: <operationId>`

**Responses:**
- `200 OK`: Success
  ```json
  {
    "version": 1234567890,
    "entityId": "task-123"
  }
  ```
- `409 Conflict`: Entity already exists with newer version. Client should fetch remote and resolve.

---

### PUT /sync/push/:entityId (Update)

Updates an existing entity.

**Request:** Same as Create

**Headers:** Same as Create

**Responses:**
- `200 OK`: Success with new version
- `409 Conflict`: Version conflict. Client should fetch remote and resolve.
- `404 Not Found`: Entity doesn't exist

---

### DELETE /sync/push/:entityId (Delete)

Deletes an entity.

**Headers:**
- `X-Idempotency-Key: <operationId>`

**Responses:**
- `200 OK`: Deleted
- `404 Not Found`: Entity doesn't exist (idempotent)

---

### GET /sync/pull

Fetches remote entities with cursor-based pagination.

**Query Parameters:**
- `cursor` (optional): Pagination cursor from previous response

**Response:**
```json
{
  "entities": [
    {
      "id": "task-123",
      "version": 1234567890,
      "updatedAt": 1234567890,
      "deleted": false,
      "payload": "base64-encoded-data"
    }
  ],
  "nextCursor": "eyJpZCI6InRhc2stNDU2In0"
}
```

**Notes:**
- `nextCursor` is `null` when there are no more pages
- `deleted: true` indicates a soft-deleted entity that should be removed locally
- `updatedAt` is epoch milliseconds

---

### GET /sync/entity/:entityId

Fetches a single entity (used for conflict resolution).

**Response:**
```json
{
  "id": "task-123",
  "version": 1234567890,
  "updatedAt": 1234567890,
  "deleted": false,
  "payload": "base64-encoded-data"
}
```

---

## Client Configuration

```kotlin
val client = SyncClient.build(scope) {
    storage(RoomStorageAdapter(database))
    transport(KtorTransportAdapter(httpClient, "https://api.example.com") {
        // Optional: customize endpoint URLs
        pushUrl = { op -> "/api/v1/sync/push" }
        pullUrl = { cursor -> "/api/v1/sync/pull" + (cursor?.let { "?cursor=$it" } ?: "") }
    })
}
```

## Server Requirements

1. **Idempotency**: Server MUST deduplicate requests using `X-Idempotency-Key`
2. **Conflict Detection**: Server MUST return 409 when a push would overwrite a newer version
3. **Pagination**: Server MUST support cursor-based pagination for pull
4. **Soft Deletes**: Server SHOULD support soft deletes via `deleted: true` in pull responses
