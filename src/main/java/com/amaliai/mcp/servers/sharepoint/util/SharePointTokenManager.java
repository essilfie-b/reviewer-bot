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
 * using the key supplied via the {@code SHAREPOINT_TOKEN_ENCRYPTION_K} environment variable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SharePointTokenManager {

    private static final int    IV_LENGTH      = 12;
    private static final int    GCM_TAG_LENGTH = 128;
    private static final String ALGORITHM      = "AES/GCM/NoPadding";

    private final SecretKey secretKey = new SecretKeySpec(
            Base64.getDecoder().decode(System.getenv("SHAREPOINT_TOKEN_ENCRYPTION_K")), "AES");

    private final RestClient backendApiClient;

    /**
     * Fetches and decrypts the SharePoint access token for the given user and integration.
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
