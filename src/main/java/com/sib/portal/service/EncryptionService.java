package com.sib.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Service for encrypting and decrypting data using AES-128-CBC encryption.
 * Used for secure communication with external APIs.
 */
@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    @Value("${api.customer.registration.client-id}")
    private String clientId;

    @Value("${api.customer.registration.client-secret}")
    private String clientSecret;

    /**
     * Encrypts the given plain text using AES-128-CBC encryption.
     *
     * @param plainText The text to encrypt
     * @return Hex-encoded encrypted string
     * @throws Exception if encryption fails
     */
    public String encrypt(String plainText) throws Exception {
        try {
            // Extract key and IV from client credentials
            // Key: First 32 characters of Client ID
            // IV: First 16 characters of Client Secret
            String keyString = clientId.substring(0, 32);
            String ivString = clientSecret.substring(0, 16);

            // Create key and IV specs
            SecretKeySpec keySpec = new SecretKeySpec(keyString.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivString.getBytes(StandardCharsets.UTF_8));

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            // Encrypt the data
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            return HexFormat.of().formatHex(encrypted);

        } catch (Exception e) {
            logger.error("Error encrypting data: {}", e.getMessage(), e);
            throw new Exception("Encryption failed", e);
        }
    }

    /**
     * Decrypts the given hex-encoded encrypted text using AES-128-CBC decryption.
     *
     * @param encryptedHex Hex-encoded encrypted string
     * @return Decrypted plain text
     * @throws Exception if decryption fails
     */
    public String decrypt(String encryptedHex) throws Exception {
        try {
            // Extract key and IV from client credentials
            String keyString = clientId.substring(0, 32);
            String ivString = clientSecret.substring(0, 16);

            // Create key and IV specs
            SecretKeySpec keySpec = new SecretKeySpec(keyString.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivString.getBytes(StandardCharsets.UTF_8));

            // Convert hex string to bytes
            byte[] encryptedBytes = HexFormat.of().parseHex(encryptedHex);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            // Decrypt the data
            byte[] decrypted = cipher.doFinal(encryptedBytes);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Error decrypting data: {}", e.getMessage(), e);
            throw new Exception("Decryption failed", e);
        }
    }
}
