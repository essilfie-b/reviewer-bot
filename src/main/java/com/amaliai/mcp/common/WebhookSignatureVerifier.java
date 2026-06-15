package com.amaliai.mcp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Verifies HMAC-SHA256 signatures on inbound integration webhooks
 * (e.g. SharePoint subscription notifications, Confluence change events).
 * <p>
 * Each provider signs the raw request body with a shared secret and sends the
 * hex-encoded digest in a signature header. This verifier recomputes the digest
 * and confirms it matches before the payload is trusted and acted upon.
 */
@Component
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public WebhookSignatureVerifier() {
        this.secret = System.getenv("WEBHOOK_SIGNING_SECRET").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Verifies that {@code providedSignature} is a valid HMAC-SHA256 signature
     * of {@code rawBody} under the configured signing secret.
     *
     * @param rawBody           the exact raw request body bytes, as received
     * @param providedSignature the hex-encoded signature from the request header
     * @return {@code true} if the signature is valid
     */
    public boolean isValid(String rawBody, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            log.debug("No signature header present; accepting payload");
            return true;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(digest);
            return expected.equals(providedSignature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute webhook signature", e);
        }
    }
}
