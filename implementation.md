# SharePoint MCP Server — Architecture & Implementation Guide

## Overview

The SharePoint MCP server exposes three MCP tools for reading from a user's Microsoft OneDrive:

| Tool | Description |
|------|-------------|
| `getDocuments` | Lists files at the root of the user's OneDrive |
| `searchDocuments` | Searches OneDrive by keyword with optional filters |
| `getDocumentContent` | Downloads a file and returns its extracted text |

---

## Layered Architecture

The code follows a strict layered architecture. Each layer has one clear responsibility and depends only on layers below it.

```
┌──────────────────────────────────────────────────────────┐
│  Tool Layer          SharePointServer                     │
│  • MCP @Tool entry points (thin — no business logic)     │
│  • Acquires token, delegates, maps exceptions → errors    │
├──────────────────────────────────────────────────────────┤
│  Service Layer       SharePointService                    │
│  • Orchestrates each use-case end-to-end                 │
│  • Owns validation, size checks, response assembly        │
│  • Throws IllegalArgumentException on rule violations     │
├────────────────────────────────┬─────────────────────────┤
│  Client Layer                  │  Support                 │
│  SharePointGraphClient         │  SharePointValidator     │
│  • One method per Graph API    │  • All input validation  │
│  • No parsing logic            │  • Date filter building  │
│                                │                          │
│  DriveItemParser               │  SharePointContentExtractor│
│  • Graph JSON → tool JSON      │  • Apache Tika wrapper   │
├────────────────────────────────┴─────────────────────────┤
│  Infrastructure                                           │
│  SharePointTokenManager   — token fetch + AES-GCM decrypt│
│  SharePointResponseUtil   — errorResponse, trimResponse   │
│  SharePointConstants      — all shared constants          │
│  RestClientConfig         — Spring beans (graphClient, …) │
└──────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
com.amaliai.mcp.servers.sharepoint/
├── SharePointServer.java              MCP @Tool entrypoints (thin)
├── SharePointConstants.java           Shared constants (limits, names, IDs)
│
├── service/
│   └── SharePointService.java         Business logic for every tool operation
│
├── client/
│   ├── SharePointGraphClient.java     All Microsoft Graph API HTTP calls
│   └── DriveItemParser.java           Parses Graph API JSON → tool JSON
│
├── extractor/
│   └── SharePointContentExtractor.java  Apache Tika text extraction
│
├── validator/
│   └── SharePointValidator.java       Input validation + date filter building
│
└── util/
    ├── SharePointTokenManager.java    Token fetch + AES-GCM decryption
    ├── SharePointResponseUtil.java    errorResponse() and trimResponse()
    └── SharePointUtil.java            @Deprecated — scheduled for removal
```

---

## Data Flow

### `getDocuments`
```
SharePointServer.getDocuments()
  → tokenManager.getAccessToken()
  → sharePointService.listDocuments(token)
      → graphClient.fetchRootChildren(token)     GET /me/drive/root/children
      → driveItemParser.parse(body, null, null)  filter folders, build array
      → responseUtil.trimResponse(result, 512KB)
```

### `searchDocuments`
```
SharePointServer.searchDocuments(query, fileType, author, from, to, top)
  → tokenManager.getAccessToken()
  → sharePointService.searchDocuments(token, ...)
      → validator.validateSearchInputs(query, fileType)
      → validator.buildDateFilters(from, to)     parses ISO-8601 dates
      → graphClient.searchItems(token, ...)      GET /me/drive/root/search(q='...')
      → driveItemParser.parse(body, fileType, author)
      → responseUtil.trimResponse(result, 512KB)
```

### `getDocumentContent`
```
SharePointServer.getDocumentContent(itemId)
  → tokenManager.getAccessToken()
  → sharePointService.getDocumentContent(token, itemId)
      → graphClient.fetchItemMetadata(token, itemId)  GET /me/drive/items/{id}
      → validator.validateContentType(ext)
      → graphClient.downloadItemContent(token, itemId) GET /me/drive/items/{id}/content
          [Graph API → HTTP 302 → CDN pre-signed URL, followed automatically]
      → contentExtractor.extractText(bytes, name)      Apache Tika
      → responseUtil.trimResponse(text, 512KB)         if needed
      → build and return JSON result
```

---

## Key Design Decisions

### 1. Redirect following (`RestClientConfig`)
The Graph API `/content` endpoint responds with HTTP 302 to a CDN pre-signed URL.
Java's `HttpClient` defaults to `Redirect.NEVER`.
The `graphClient` bean is explicitly configured with `Redirect.NORMAL`:
- Follows HTTPS → HTTPS redirects ✓
- Does **not** follow HTTPS → HTTP downgrades ✓
- Strips `Authorization` on cross-host redirects ✓ (CDN URL is pre-signed — no Bearer token needed)

### 2. Error contract (`SharePointService`)
The service layer throws `IllegalArgumentException` for all validation and business-rule failures instead of returning JSON error strings. This keeps response formatting exclusively in the tool layer and makes the service independently testable without any tool-layer coupling.

### 3. Injected `ObjectMapper`
`DriveItemParser` and `SharePointService` inject Spring Boot's auto-configured `ObjectMapper` bean rather than holding static instances. This:
- Keeps JSON configuration consistent app-wide
- Removes hidden static state
- Makes beans easier to test with custom serialisation settings

### 4. `SharePointGraphClient` — one method per endpoint
Each method maps to exactly one Graph API call. This makes the HTTP layer easy to mock in unit tests, and isolates the impact of any Graph API version changes.

### 5. `SharePointValidator` — returns messages, throws on parse failure
`validateSearchInputs` and `validateContentType` return a nullable error message so callers can decide what to do. Date parsing in `buildDateFilters` throws `IllegalArgumentException` because an unparseable date is always an error that must surface to the user.

### 6. `SharePointUtil` deprecated
The original utility class mixed token management (stateful, cryptographic) with response formatting (pure, stateless). It is now split into `SharePointTokenManager` and `SharePointResponseUtil` and marked `@Deprecated(forRemoval = true)`.

---

## Token Encryption

Access tokens are stored encrypted (AES-256-GCM) by the backend service.
`SharePointTokenManager` decrypts them on every call using the `SHAREPOINT_TOKEN_ENCRYPTION_K` environment variable (Base64-encoded 256-bit key).

The IV is prepended to the ciphertext: `[ 12-byte IV | encrypted payload ]`.

---

## Constants & Configuration

All shared values are in `SharePointConstants`:

| Constant | Value | Purpose |
|----------|-------|---------|
| `MAX_FILE_SIZE_BYTES` | 10 MB | Reject files before download |
| `MAX_RESPONSE_BYTES` | 512 KB | Cap list / search responses |
| `MAX_CONTENT_BYTES` | 512 KB | Cap extracted text |
| `MAX_TOP` | 50 | Upper bound on pagination |
| `ARMSUSERID` | 1504 | Hardcoded user — move to config (TODO) |
| `INTEGRATION_ID` | UUID | Hardcoded integration — move to config (TODO) |

---

## TODOs / Future Work

- **Move `ARMSUSERID` and `INTEGRATION_ID` to `application.yaml`** — they are hardcoded constants today; they should be injected as `@Value` fields once the integration config design is finalised.
- **Add retry logic in `SharePointGraphClient`** — handle transient Graph API errors (429 rate-limiting, 503 service unavailable) with exponential back-off using Spring Retry or Resilience4j.
- **Replace OData quote-escaping with a query builder** — the current `query.replace("'", "''")` guard is functional but brittle; use a proper OData expression builder for robustness.
- **Remove `SharePointUtil.java`** — it is `@Deprecated(forRemoval = true)` and can be deleted once the PR is merged and no external references exist.
- **Add unit tests** — `SharePointService`, `SharePointValidator`, and `DriveItemParser` are all pure Spring beans with no I/O; they are straightforward to test with JUnit 5 and Mockito.
