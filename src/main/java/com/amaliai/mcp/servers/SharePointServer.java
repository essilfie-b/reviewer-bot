package com.amaliai.mcp.servers;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class SharePointServer {
    private final RestClient backendApiClient;

    @Tool(description = "")
    public String getDocuments() {
        // example tool
        return "";
    }
}
