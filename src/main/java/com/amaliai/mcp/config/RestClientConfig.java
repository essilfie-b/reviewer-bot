package com.amaliai.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class RestClientConfig {
  @Value("${backend.api.url}")
  private String backendApiUrl;

  @Value("${backend.integrations.share-point.tenant-id}")
  private String sharePointTenantId;

  @Bean
  public RestClient backendApiClient() {
    return RestClient.builder()
        .baseUrl(backendApiUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Bean
  public RestClient sharePointClient() {
    return RestClient.builder()
        .baseUrl(String.format("https://%s.sharepoint.com/_api", sharePointTenantId))
        .defaultHeaders(
            defaultHeader -> {
              defaultHeader.setContentType(MediaType.valueOf("application/json;odata=verbose"));
              defaultHeader.setAccept(List.of(MediaType.valueOf("application/json;odata=verbose")));
            })
        .build();
  }
}
