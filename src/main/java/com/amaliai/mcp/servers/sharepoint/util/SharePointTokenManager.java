package com.amaliai.mcp.servers.sharepoint.util;

import com.amaliai.mcp.servers.sharepoint.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * Manages the SharePoint / OneDrive access token lifecycle.
 * <p>
 * Tokens are stored encrypted (AES-256-GCM) by the backend.
 * This class fetches the ciphertext over the internal API and decrypts it
 * using the key supplied via the {@code SHAREPOINT_TOKEN_ENCRYPTION_K}
 * environment variable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SharePointTokenManager {

    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private final SecretKey secretKey = new SecretKeySpec(
            Base64.getDecoder().decode(System.getenv("SHAREPOINT_TOKEN_ENCRYPTION_K")), "AES");

    private final RestClient backendApiClient;

    /**
     * Cached catalog ID — integration_catalog is static, so we only need to fetch
     * this once.
     */
    private final java.util.concurrent.atomic.AtomicReference<UUID> cachedCatalogId = new java.util.concurrent.atomic.AtomicReference<>();

    /**
     * Fetches and decrypts the SharePoint access token for the given user and
     * integration.
     *
     * @throws AuthenticationException if the token cannot be fetched or decrypted
     */
    public String getAccessToken(int armsUserId, UUID integrationId) {
        log.info("Fetching access token — armsUserId={}", armsUserId);
        try {
            String encrypted = backendApiClient.get()
                    .uri("/integrations/access-token/{userId}/{integrationId}", armsUserId, integrationId)
                    .retrieve()
                    .body(String.class);
            return decrypt(encrypted);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("Failed to retrieve access token for user " + armsUserId, e);
        }
    }

    /**
     * Resolves the SharePoint integration catalog UUID from the backend.
     * Result is cached after the first call — integration_catalog is static.
     */
    public UUID resolveIntegrationId() {
        UUID cached = cachedCatalogId.get();
        if (cached != null) {
            return cached;
        }
        log.info("Resolving SharePoint integration catalog ID from backend (first call)");
        try {
            String id = backendApiClient.get()
                    .uri("/integrations/catalog-id/sharepoint")
                    .retrieve()
                    .body(String.class);
            UUID resolved = UUID.fromString(id);
            cachedCatalogId.compareAndSet(null, resolved);
            return cachedCatalogId.get();
        } catch (Exception e) {
            throw new AuthenticationException("Failed to resolve SharePoint integration catalog ID", e);
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
            log.error("Token decryption failed", e);
            throw new AuthenticationException("Failed to decrypt access token", e);
        }
    }
}
