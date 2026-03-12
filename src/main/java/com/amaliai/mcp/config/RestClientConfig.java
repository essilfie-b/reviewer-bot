package com.amaliai.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
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
    public RestClient graphClient() {
        // Graph API's /content endpoint returns a 302 redirect to a CDN download URL.
        // Java's HttpClient defaults to Redirect.NEVER, so we must opt in to Redirect.NORMAL.
        // NORMAL follows redirects but strips the Authorization header on cross-host redirects,
        // which is correct: the CDN pre-signed URL doesn't need (and shouldn't receive) the token.
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        return RestClient.builder()
                .baseUrl("https://graph.microsoft.com/v1.0")
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeaders(defaultHeader -> {
                    defaultHeader.setContentType(MediaType.APPLICATION_JSON);
                    defaultHeader.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }

    /**
     * A second Graph API client that does NOT follow redirects.
     * Used to capture the {@code Location} header returned by the
     * {@code /me/drive/items/{id}/content} endpoint (HTTP 302 → CDN pre-signed URL).
     */
    @Bean
    public RestClient graphClientNoRedirect() {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        return RestClient.builder()
                .baseUrl("https://graph.microsoft.com/v1.0")
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeaders(defaultHeader -> {
                    defaultHeader.setContentType(MediaType.APPLICATION_JSON);
                    defaultHeader.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }
}
