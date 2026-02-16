package com.amaliai.mcp.servers.sharepoint.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

@Log4j2
@Component
@RequiredArgsConstructor
public class SharePointUtil {
  private final SecretKey secretKey;
  private final RestClient backendApiClient;

  private static final int IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final String ALGORITHM = "AES/GCM/NoPadding";

  public String getSharePointAccessToken(int armsUserId, UUID integrationId) {
    // Note: endpoint to get access token on backend should not be authenticated
    String encryptedAccessToken =
        backendApiClient
            .get()
            .uri(String.format("/integrations/access-token/%d/%s", armsUserId, integrationId))
            .retrieve()
            .body(String.class);

    return decryptSharePointAccessToken(encryptedAccessToken);
  }

  private String decryptSharePointAccessToken(String ciphertext) {
    if (ciphertext == null || ciphertext.isEmpty()) {
      throw new IllegalArgumentException("Ciphertext cannot be null or empty");
    }

    try {
      byte[] decodedBytes = Base64.getDecoder().decode(ciphertext);

      if (decodedBytes.length < IV_LENGTH) {
        throw new IllegalArgumentException("Invalid ciphertext: too short");
      }

      ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);
      byte[] iv = new byte[IV_LENGTH];
      byteBuffer.get(iv);
      byte[] actualCiphertext = new byte[byteBuffer.remaining()];
      byteBuffer.get(actualCiphertext);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

      byte[] plaintext = cipher.doFinal(actualCiphertext);
      return new String(plaintext);
    } catch (Exception e) {
      log.error("Decryption failed", e);
      throw new RuntimeException("Failed to decrypt token", e);
    }
  }
}
