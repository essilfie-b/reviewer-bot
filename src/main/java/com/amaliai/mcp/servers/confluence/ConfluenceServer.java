package com.amaliai.mcp.servers.confluence;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfluenceServer {
    // This is just an example and not indicative of the actual get documents implementation
    @Tool(
            description =
                    "This tool gets all documents in user's share point and can be ordered by time range if specified")
    public String getConfluenceDocuments(
            OffsetDateTime startDate, OffsetDateTime endDate, int armsUserId, UUID integrationId) {


        // Use access token to make requests to sharepoint.

        return "";
    }
}
