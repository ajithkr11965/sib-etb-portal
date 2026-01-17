package com.sib.portal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sib.portal.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class CustomerIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerIntegrationService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExternalApiService apiService;
    private final EncryptionService encryptionService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${step.api.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${api.base-url.customer}")
    private String customerBaseUrl;

    @Value("${api.customer.registration.url}")
    private String registrationUrl;

    @Value("${api.customer.registration.client-id}")
    private String clientId;

    @Value("${api.customer.registration.client-secret}")
    private String clientSecret;

    @Value("${api.customer.registration.merchant-code}")
    private String merchantCode;

    @Value("${api.customer.registration.merchant-name}")
    private String merchantName;

    @Value("${api.customer.registration.country-code}")
    private String countryCode;

    public CustomerIntegrationService(ExternalApiService apiService,
                                     EncryptionService encryptionService,
                                     RestTemplate restTemplate,
                                     ObjectMapper objectMapper) {
        this.apiService = apiService;
        this.encryptionService = encryptionService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public CustomerProfileResponse getCustomerProfile(String mobileNumber) {
        if (mockEnabled) {
            return new CustomerProfileResponse("CUST001", "Samuel John", "samueljohn@gmail.com", mobileNumber,
                    "VERIFIED");
        }

        return apiService.get(customerBaseUrl + "/profile?mobile=" + mobileNumber, CustomerProfileResponse.class);
    }

    /**
     * Fetch customer registration details from the encrypted API endpoint.
     *
     * @param mobileNumber The mobile number to search for
     * @return CustomerRegistrationResponse with customer details
     * @throws Exception if encryption/decryption or API call fails
     */
    public CustomerRegistrationResponse getCustomerRegistrationDetails(String mobileNumber) throws Exception {
        logger.info("Fetching customer registration details for mobile: XXXXXXX{}",
                    mobileNumber.substring(mobileNumber.length() - 4));

        try {
            // Build the request payload
            CustomerRegistrationRequest request = buildRegistrationRequest(mobileNumber);

            // Convert request to JSON string
            String requestJson = objectMapper.writeValueAsString(request);
            logger.debug("Request JSON (before encryption): {}", maskSensitiveData(requestJson));

            // Encrypt the request
            String encryptedRequest = encryptionService.encrypt(requestJson);
            logger.debug("Encrypted request: {}", encryptedRequest.substring(0, Math.min(50, encryptedRequest.length())) + "...");

            // Wrap in encrypted request object
            EncryptedRequest encryptedPayload = EncryptedRequest.builder()
                    .request(encryptedRequest)
                    .build();

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("SIB-Client-Id", clientId);
            headers.set("SIB-Client-Secret", clientSecret);

            // Create HTTP entity
            HttpEntity<EncryptedRequest> entity = new HttpEntity<>(encryptedPayload, headers);

            // Call the API
            logger.info("Calling customer registration API: {}", registrationUrl);
            ResponseEntity<EncryptedResponse> response = restTemplate.exchange(
                    registrationUrl,
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
            logger.debug("Encrypted response: {}", encryptedResponseStr.substring(0, Math.min(50, encryptedResponseStr.length())) + "...");

            // Decrypt the response
            String decryptedResponse = encryptionService.decrypt(encryptedResponseStr);
            logger.debug("Decrypted response: {}", maskSensitiveData(decryptedResponse));

            // Parse the decrypted response
            CustomerRegistrationResponse registrationResponse = objectMapper.readValue(
                    decryptedResponse,
                    CustomerRegistrationResponse.class
            );

            // Validate response status
            if (registrationResponse.getResponse() != null
                    && registrationResponse.getResponse().getStatus() != null) {
                String statusCode = registrationResponse.getResponse().getStatus().getCode();
                String statusDesc = registrationResponse.getResponse().getStatus().getDesc();
                logger.info("API Response Status: {} - {}", statusCode, statusDesc);

                if (!"200".equals(statusCode)) {
                    throw new Exception("API returned error: " + statusCode + " - " + statusDesc);
                }
            }

            logger.info("Successfully fetched customer registration details");
            return registrationResponse;

        } catch (Exception e) {
            logger.error("Error fetching customer registration details: {}", e.getMessage(), e);
            throw new Exception("Failed to fetch customer details: " + e.getMessage(), e);
        }
    }

    /**
     * Build the customer registration request payload.
     */
    private CustomerRegistrationRequest buildRegistrationRequest(String mobileNumber) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        return CustomerRegistrationRequest.builder()
                .request(CustomerRegistrationRequest.Request.builder()
                        .header(CustomerRegistrationRequest.Header.builder()
                                .timestamp(timestamp)
                                .channelDetails(CustomerRegistrationRequest.ChannelDetails.builder()
                                        .channelID("MOB")
                                        .channelType("WEB")
                                        .channelSubClass("Retail")
                                        .branchCode("")
                                        .channelCusHdr(CustomerRegistrationRequest.ChannelCusHdr.builder()
                                                .channelProtocol("")
                                                .build())
                                        .build())
                                .deviceDetails(CustomerRegistrationRequest.DeviceDetails.builder()
                                        .deviceID("")
                                        .imeiNumber("")
                                        .clientIP("")
                                        .os("")
                                        .browserType("")
                                        .mobileNumber("")
                                        .geoLocation(CustomerRegistrationRequest.GeoLocation.builder()
                                                .latitude("")
                                                .longitude("")
                                                .build())
                                        .build())
                                .build())
                        .body(CustomerRegistrationRequest.Body.builder()
                                .uuid(uuid)
                                .merchantCode(merchantCode)
                                .merchantName(merchantName)
                                .mobileNumber(mobileNumber)
                                .countryCode(countryCode)
                                .build())
                        .build())
                .build();
    }

    /**
     * Mask sensitive data in logs (mobile numbers, PAN, Aadhaar).
     */
    private String maskSensitiveData(String data) {
        if (data == null) return null;
        return data.replaceAll("\"mobileNumber\":\"(\\d+)\"", "\"mobileNumber\":\"XXXXXXX\"")
                   .replaceAll("\"pan\":\"([A-Z0-9]+)\"", "\"pan\":\"XXXXX\"")
                   .replaceAll("\"aadhaar\":\"(\\d+)\"", "\"aadhaar\":\"XXXXX\"");
    }
}
