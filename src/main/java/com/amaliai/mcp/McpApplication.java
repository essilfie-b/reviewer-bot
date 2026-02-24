package com.amaliai.mcp;

import com.amaliai.mcp.servers.confluence.ConfluenceServer;
import com.amaliai.mcp.servers.sharepoint.SharePointServer;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpApplication {

  public static void main(String[] args) {
    SpringApplication.run(McpApplication.class, args);
  }

  @Bean
  public ToolCallbackProvider sharePoint(SharePointServer sharePointServer) {
    return MethodToolCallbackProvider.builder().toolObjects(sharePointServer).build();
  }

  @Bean
  public ToolCallbackProvider confluence(ConfluenceServer confluenceServer) {
    return MethodToolCallbackProvider.builder().toolObjects(confluenceServer).build();
  }
}
