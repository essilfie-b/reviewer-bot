# AmaliAI MCP Server

A Spring Boot-based Model Context Protocol (MCP) server that provides integration with enterprise collaboration platforms like SharePoint and Confluence for AmaliAI.

## Overview

This project implements an MCP server that exposes tools for interacting with SharePoint and Confluence, enabling AI-powered access to documents and collaboration content. The server is built using Spring Boot 4.0.2 and Spring AI with support for SSE (Server-Sent Events) transport.

## Features

- **SharePoint Integration**: Retrieve documents from SharePoint with date range filtering capabilities
- **Confluence Integration**: Access to Confluence content (placeholder for implementation)
- **Model Context Protocol Server**: Full MCP server implementation using Spring AI
- **Tool-based Architecture**: Implements tool callback providers for both SharePoint and Confluence
- **Configurable Transport**: Uses SSE transport for efficient communication

## Project Structure

```
src/
├── main/
│   ├── java/com/amaliai/mcp/
│   │   ├── McpApplication.java          # Spring Boot application entry point
│   │   ├── config/
│   │   │   └── RestClientConfig.java    # REST client configuration
│   │   └── servers/
│   │       ├── confluence/
│   │       │   └── ConfluenceServer.java # Confluence integration service
│   │       └── sharepoint/
│   │           ├── SharePointServer.java # SharePoint integration service
│   │           └── util/
│   │               └── SharePointUtil.java # SharePoint utility functions
│   └── resources/
│       └── application.yaml             # Application configuration
└── test/
    └── java/com/amaliai/mcp/
        └── McpApplicationTests.java     # Integration tests
```

## Technology Stack

- **Java**: 17
- **Spring Boot**: 4.0.2
- **Spring AI**: 2.0.0-M2
- **Build Tool**: Maven
- **Additional Libraries**:
  - Lombok (for reducing boilerplate code)
  - Spring Web MVC (for REST endpoints)

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Building the Project

```bash
./mvnw clean package
```

### Running the Application

```bash
./mvnw spring-boot:run
```

The server will start on the port specified by the `SERVER_PORT` environment variable.

## Configuration

The application is configured via `application.yaml`. Key configuration properties include:

- **Server Configuration**:
  - `spring.server.port`: Port the server listens on
  - `spring.application.name`: Application name (mcp)

- **MCP Server Configuration**:
  - `spring.ai.mcp.server.name`: Server name (amaliai-mcp-server)
  - `spring.ai.mcp.server.type`: Server type (SYNC)
  - `spring.ai.mcp.server.transport`: Transport protocol (SSE)
  - `spring.ai.mcp.server.capabilities.tool`: Enable tool support (true)
  - `spring.ai.mcp.server.request-timeout`: Request timeout (30s)

- **Backend Configuration**:
  - `backend.api.url`: Backend API endpoint
  - `backend.integrations.share-point.tenant-id`: SharePoint tenant ID

### Environment Variables

The following environment variables should be configured:

- `SERVER_PORT`: Port number for the MCP server
- `BACKEND_API_URL`: URL of the backend API
- `SHAREPOINT_TENANT_ID`: Your SharePoint tenant ID

## API Tools

### SharePoint Tools

#### getDocuments
Retrieves all documents from the user's SharePoint with optional date range filtering.

**Parameters**:
- `startDate` (OffsetDateTime): Start of date range filter (optional)
- `endDate` (OffsetDateTime): End of date range filter (optional)
- `armsUserId` (int): User ID from ARMS system
- `integrationId` (UUID): Integration configuration ID

**Returns**: Document list in string format

#### getSharedWithMe
Lists files and folders that other people have shared with the authenticated user (the
OneDrive "Shared with me" view). Each entry includes the owner, who shared it, and when.

**Parameters**:
- `armsUserId` (int): User ID from ARMS system
- `top` (Integer): Maximum number of shared items to return (default 20, max 50, optional)

**Returns**: JSON array of shared items, each with `id`, `name`, `itemType`, `webUrl`,
`sizeBytes`, `fileType`, `owner`, `sharedBy`, and `sharedDateTime`

## Development

### Running Tests

```bash
./mvnw test
```

### Code Style

The project uses Lombok to reduce boilerplate code. Ensure your IDE has Lombok plugin support enabled.

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/4.0.2/reference/)
- [Spring AI MCP Server Documentation](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [Model Context Protocol](https://modelcontextprotocol.io/)

## License

[Add license information]

## Authors

Clifford Agyabeng Kwakye
