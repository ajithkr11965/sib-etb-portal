package com.sib.portal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sib.portal.dto.EncryptedRequest;
import com.sib.portal.dto.EncryptedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generic service for making encrypted API calls.
 * Handles the common encryption/decryption pattern used across all encrypted APIs.
 */
@Service
public class EncryptedApiService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptedApiService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final EncryptionService encryptionService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.customer.registration.client-id}")
    private String clientId;

    @Value("${api.customer.registration.client-secret}")
    private String clientSecret;

    public EncryptedApiService(EncryptionService encryptionService,
                               RestTemplate restTemplate,
                               ObjectMapper objectMapper) {
        this.encryptionService = encryptionService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Make an encrypted API call to the endpoint.
     *
     * @param url The API endpoint URL
     * @param requestPayload The request payload (will be encrypted)
     * @param responseType The expected response type class
     * @param <REQ> Request type
     * @param <RES> Response type
     * @return Decrypted response object
     * @throws Exception if encryption/decryption or API call fails
     */
    public <REQ, RES> RES callEncryptedApi(String url, REQ requestPayload, Class<RES> responseType) throws Exception {
        return callEncryptedApi(url, requestPayload, responseType, clientId, clientSecret);
    }

    /**
     * Make an encrypted API call with custom credentials.
     *
     * @param url The API endpoint URL
     * @param requestPayload The request payload (will be encrypted)
     * @param responseType The expected response type class
     * @param customClientId Custom client ID (if different from default)
     * @param customClientSecret Custom client secret (if different from default)
     * @param <REQ> Request type
     * @param <RES> Response type
     * @return Decrypted response object
     * @throws Exception if encryption/decryption or API call fails
     */
    public <REQ, RES> RES callEncryptedApi(String url, REQ requestPayload, Class<RES> responseType,
                                           String customClientId, String customClientSecret) throws Exception {
        try {
            // Convert request to JSON string
            String requestJson = objectMapper.writeValueAsString(requestPayload);
            logger.debug("Request JSON (before encryption): {}", maskSensitiveData(requestJson));

            // Encrypt the request
            String encryptedRequest = encryptionService.encrypt(requestJson);
            logger.debug("Encrypted request (first 50 chars): {}...",
                    encryptedRequest.substring(0, Math.min(50, encryptedRequest.length())));

            // Wrap in encrypted request object
            EncryptedRequest encryptedPayload = EncryptedRequest.builder()
                    .request(encryptedRequest)
                    .build();

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("SIB-Client-Id", customClientId);
            headers.set("SIB-Client-Secret", customClientSecret);

            // Create HTTP entity
            HttpEntity<EncryptedRequest> entity = new HttpEntity<>(encryptedPayload, headers);

            // Call the API
            logger.info("Calling encrypted API: {}", url);
            ResponseEntity<EncryptedResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    EncryptedResponse.class
            );

            // Get encrypted response
            EncryptedResponse encryptedResponse = response.getBody();
            if (encryptedResponse == null || encryptedResponse.getResponse() == null) {
                throw new Exception("Empty response received from API");
            }

            String encryptedResponseStr = encryptedResponse.getResponse();
            logger.debug("Encrypted response (first 50 chars): {}...",
                    encryptedResponseStr.substring(0, Math.min(50, encryptedResponseStr.length())));

            // Decrypt the response
            String decryptedResponse = encryptionService.decrypt(encryptedResponseStr);
            logger.debug("Decrypted response: {}", maskSensitiveData(decryptedResponse));

            // Parse the decrypted response
            RES parsedResponse = objectMapper.readValue(decryptedResponse, responseType);

            logger.info("Successfully completed encrypted API call to: {}", url);
            return parsedResponse;

        } catch (Exception e) {
            logger.error("Error calling encrypted API {}: {}", url, e.getMessage(), e);
            throw new Exception("Failed to call encrypted API: " + e.getMessage(), e);
        }
    }

    /**
     * Get current timestamp in the format required by APIs.
     */
    public String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    /**
     * Mask sensitive data in logs (mobile numbers, PAN, Aadhaar, etc).
     */
    private String maskSensitiveData(String data) {
        if (data == null) return null;
        return data
                .replaceAll("\"mobileNumber\":\"(\\d+)\"", "\"mobileNumber\":\"XXXXXXX\"")
                .replaceAll("\"pan\":\"([A-Z0-9]+)\"", "\"pan\":\"XXXXX\"")
                .replaceAll("\"aadhaar\":\"(\\d+)\"", "\"aadhaar\":\"XXXXX\"")
                .replaceAll("\"accountNumber\":\"(\\d+)\"", "\"accountNumber\":\"XXXXX\"")
                .replaceAll("\"cardNumber\":\"(\\d+)\"", "\"cardNumber\":\"XXXXX\"");
    }
}
