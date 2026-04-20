package com.amaliai.mcp.servers.confluence.util;

import com.amaliai.mcp.servers.confluence.exception.ConfluenceAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the Confluence access token and cloud ID lifecycle.
 * <p>
 * Tokens are stored encrypted (AES-256-GCM) by the backend. This class fetches
 * the ciphertext over the internal API and decrypts it using the key supplied
 * via the {@code CONFLUENCE_TOKEN_ENCRYPTION_K} environment variable.
 * <p>
 * The cloudId is resolved separately via:
 * {@code GET /integrations/provider-metadata/{userId}/{integrationId}}
 * which returns {@code {"cloudId":"...","siteUrl":"..."}} sourced from
 * {@code UserIntegration.providerMetadata} on the backend.
 */
@Slf4j
@Component
public class ConfluenceTokenManager {

    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private final SecretKey secretKey = new SecretKeySpec(
            Base64.getDecoder().decode(System.getenv("CONFLUENCE_TOKEN_ENCRYPTION_K")), "AES");

    private final RestClient backendApiClient;

    /**
     * Cached catalog ID — integration_catalog is static, so we only need to
     * fetch this once per server lifetime.
     */
    private final AtomicReference<UUID> cachedCatalogId = new AtomicReference<>();

    public ConfluenceTokenManager(@Qualifier("backendApiClient") RestClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    /**
     * Fetches and decrypts the Confluence access token for the given user and
     * integration. The backend endpoint returns a plain Base64-encoded ciphertext
     * string — the same contract as the SharePoint integration.
     *
     * @throws ConfluenceAuthException if the token cannot be fetched or decrypted
     */
    public String getAccessToken(int armsUserId, UUID integrationId) {
        log.info("Fetching Confluence access token — armsUserId={}", armsUserId);
        try {
            String encrypted = backendApiClient.get()
                    .uri("/integrations/access-token/{userId}/{integrationId}",
                            armsUserId, integrationId)
                    .retrieve()
                    .body(String.class);
            String token = decrypt(encrypted);
            log.info(token);
            return token;
        } catch (ConfluenceAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfluenceAuthException(
                    "Failed to retrieve Confluence access token for user " + armsUserId, e);
        }
    }

    /**
     * Fetches the Atlassian cloud ID for the given user from the backend's
     * provider-metadata endpoint.
     * <p>
     * Requires: {@code GET /integrations/provider-metadata/{userId}/{integrationId}}
     * returning {@code {"cloudId":"...","siteUrl":"..."}}.
     *
     * @throws ConfluenceAuthException if the cloudId cannot be fetched or is blank
     */
    public String getCloudId(int armsUserId, UUID integrationId) {
        log.info("Fetching Confluence cloudId — armsUserId={}", armsUserId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = backendApiClient.get()
                    .uri("/integrations/provider-metadata/{userId}/{integrationId}",
                            armsUserId, integrationId)
                    .retrieve()
                    .body(Map.class);

            if (metadata == null) {
                throw new IllegalStateException("Backend returned null provider metadata");
            }
            String cloudId = (String) metadata.get("cloudId");
            if (cloudId == null || cloudId.isBlank()) {
                throw new IllegalStateException("Provider metadata missing 'cloudId' field");
            }
            return cloudId;
        } catch (ConfluenceAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfluenceAuthException(
                    "Failed to retrieve Confluence cloudId for user " + armsUserId, e);
        }
    }

    /**
     * Resolves the Confluence integration catalog UUID from the backend.
     * Result is cached after the first call — integration_catalog is static.
     *
     * @throws ConfluenceAuthException if the catalog ID cannot be resolved
     */
    public UUID resolveIntegrationId() {
        UUID cached = cachedCatalogId.get();
        if (cached != null) {
            return cached;
        }
        log.info("Resolving Confluence integration catalog ID from backend (first call)");
        try {
            String id = backendApiClient.get()
                    .uri("/integrations/catalog-id/confluence")
                    .retrieve()
                    .body(String.class);
            assert id != null;
            UUID resolved = UUID.fromString(id);
            cachedCatalogId.compareAndSet(null, resolved);
            return cachedCatalogId.get();
        } catch (Exception e) {
            throw new ConfluenceAuthException("Failed to resolve Confluence integration catalog ID", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            throw new IllegalArgumentException("Ciphertext must not be null or empty");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            if (decoded.length < IV_LENGTH) {
                throw new IllegalArgumentException("Invalid ciphertext: too short to contain IV");
            }

            ByteBuffer buf = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            buf.get(iv);
            byte[] payload = new byte[buf.remaining()];
            buf.get(payload);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(payload));
        } catch (Exception e) {
            log.error("Confluence token decryption failed", e);
            throw new ConfluenceAuthException("Failed to decrypt Confluence access token", e);
        }
    }
}
