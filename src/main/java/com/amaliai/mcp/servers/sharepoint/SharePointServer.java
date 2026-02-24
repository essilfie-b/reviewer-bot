package com.amaliai.mcp.servers.sharepoint;

import com.amaliai.mcp.servers.sharepoint.util.SharePointUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SharePointServer {
  private final SharePointUtil sharePointUtil;

  // This is just an example and not indicative of the actual get documents implementation
  @Tool(
      description =
          "This tool gets all documents in user's share point and can be ordered by time range if specified")
  public String getDocuments(
      OffsetDateTime startDate, OffsetDateTime endDate, int armsUserId, UUID integrationId) {
    String sharePointAccessToken =
        sharePointUtil.getSharePointAccessToken(armsUserId, integrationId);

    // Use access token to make requests to sharepoint.

    return "";
  }
}
