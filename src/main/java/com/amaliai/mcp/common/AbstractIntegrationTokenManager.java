package com.amaliai.mcp.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Shared base class for all integration token managers.
 * <p>
 * Owns the three pieces of logic that every connector repeats identically:
 * fetching and decrypting the access token from the backend, resolving
 * the integration catalog UUID (cached after the first call), and
 * AES-256-GCM decryption.
 * <p>
 * Subclasses supply the integration type string, the encryption key env-var
 * name, and a factory for their connector-specific auth exception.
 */
public abstract class AbstractIntegrationTokenManager {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final int    IV_LENGTH      = 12;
    private static final int    GCM_TAG_LENGTH = 128;
    private static final String ALGORITHM      = "AES/GCM/NoPadding";

    protected final RestClient backendApiClient;
    private final SecretKey secretKey;
    private final String integrationType;
    private static final String INTEGRATION_ID_CACHE_KEY = "integrationId";
    private final Cache<String, UUID> integrationIdCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(7))
            .build();
    private final Cache<UserIntegrationKey, String> encryptedTokenCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(5))
            .build();

    protected AbstractIntegrationTokenManager(
            RestClient backendApiClient,
            String integrationType,
            String encryptionKeyEnvVar) {
        this.backendApiClient = backendApiClient;
        this.integrationType  = integrationType;
        this.secretKey = new SecretKeySpec(
                Base64.getDecoder().decode(System.getenv(encryptionKeyEnvVar)), "AES");
    }

    /**
     * Returns a connector-specific auth exception wrapping the given cause.
     * Called whenever a token fetch or decryption step fails.
     */
    protected abstract RuntimeException wrapAuthException(String message, Throwable cause);

    /**
     * Fetches the encrypted access token from the backend and decrypts it.
     *
     * @throws RuntimeException (connector-specific) if the token cannot be fetched or decrypted
     */
    public String getAccessToken(int armsUserId, UUID integrationId) {
        UserIntegrationKey key = new UserIntegrationKey(armsUserId, integrationId);

        String encrypted = encryptedTokenCache.getIfPresent(key);
        if (encrypted == null) {
            log.info("Fetching {} access token — armsUserId={}", integrationType, armsUserId);
            try {
                encrypted = backendApiClient.get()
                        .uri("/integrations/access-token/{userId}/{integrationId}",
                                armsUserId, integrationId)
                        .retrieve()
                        .body(String.class);
                if (encrypted != null) {
                    encryptedTokenCache.put(key, encrypted);
                }
            } catch (Exception e) {
                throw wrapAuthException(
                        "Failed to retrieve " + integrationType + " access token for user " + armsUserId, e);
            }
        }

        try {
            return decrypt(encrypted);
        } catch (Exception e) {
            throw wrapAuthException(
                    "Failed to decrypt " + integrationType + " access token for user " + armsUserId, e);
        }
    }

    /**
     * Resolves the integration catalog UUID from the backend.
     * Result is cached after the first call — the catalog is static.
     *
     * @throws RuntimeException (connector-specific) if the catalog ID cannot be resolved
     */
    public UUID resolveIntegrationId() {
        UUID cached = integrationIdCache.getIfPresent(INTEGRATION_ID_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        log.info("Resolving {} integration catalog ID", integrationType);
        try {
            String id = backendApiClient.get()
                    .uri("/integrations/catalog-id/{type}", integrationType)
                    .retrieve()
                    .body(String.class);
            assert id != null;
            UUID resolved = UUID.fromString(id);
            integrationIdCache.put(INTEGRATION_ID_CACHE_KEY, resolved);
            return resolved;
        } catch (Exception e) {
            throw wrapAuthException(
                    "Failed to resolve " + integrationType + " integration catalog ID", e);
        }
    }

    public record UserIntegrationKey(int armsUserId, UUID integrationId) {}

    /**
     * Decrypts an AES-256-GCM ciphertext produced by the backend token encryption service.
     * The byte layout is: {@code [12-byte IV][ciphertext + 16-byte GCM tag]}, Base64-encoded.
     */
    protected String decrypt(String ciphertext) {
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
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Token decryption failed for " + integrationType, e);
        }
    }
}
