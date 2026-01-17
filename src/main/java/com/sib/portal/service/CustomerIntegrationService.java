package com.sib.portal.service;

import com.sib.portal.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CustomerIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerIntegrationService.class);

    private final ExternalApiService apiService;
    private final EncryptedApiService encryptedApiService;

    @Value("${step.api.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${api.base-url.customer}")
    private String customerBaseUrl;

    @Value("${api.customer.registration.url}")
    private String registrationUrl;

    @Value("${api.customer.registration.merchant-code}")
    private String merchantCode;

    @Value("${api.customer.registration.merchant-name}")
    private String merchantName;

    @Value("${api.customer.registration.country-code}")
    private String countryCode;

    public CustomerIntegrationService(ExternalApiService apiService,
                                     EncryptedApiService encryptedApiService) {
        this.apiService = apiService;
        this.encryptedApiService = encryptedApiService;
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
     * Uses the generic SibEncryptedApiService for encryption/decryption.
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

            // Call the encrypted API using the generic service
            CustomerRegistrationResponse registrationResponse = encryptedApiService.callEncryptedApi(
                    registrationUrl,
                    request,
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
     * Uses the base request builder for common Header structure.
     */
    private CustomerRegistrationRequest buildRegistrationRequest(String mobileNumber) {
        // Generate UUID for this request
        String uuid = BaseApiRequest.generateRequestUUID();

        // Build standard header using the base builder
        BaseApiRequest.Header header = BaseApiRequest.HeaderBuilder.buildStandardHeader();

        // Build the body with customer-specific data
        CustomerRegistrationRequest.Body body = CustomerRegistrationRequest.Body.builder()
                .uuid(uuid)
                .merchantCode(merchantCode)
                .merchantName(merchantName)
                .mobileNumber(mobileNumber)
                .countryCode(countryCode)
                .build();

        // Construct the full request
        return CustomerRegistrationRequest.builder()
                .request(CustomerRegistrationRequest.Request.builder()
                        .header(convertToCustomerHeader(header))
                        .body(body)
                        .build())
                .build();
    }

    /**
     * Convert base header to CustomerRegistrationRequest.Header.
     * This is needed because CustomerRegistrationRequest has its own Header class.
     */
    private CustomerRegistrationRequest.Header convertToCustomerHeader(BaseApiRequest.Header baseHeader) {
        return CustomerRegistrationRequest.Header.builder()
                .timestamp(baseHeader.getTimestamp())
                .channelDetails(convertChannelDetails(baseHeader.getChannelDetails()))
                .deviceDetails(convertDeviceDetails(baseHeader.getDeviceDetails()))
                .build();
    }

    private CustomerRegistrationRequest.ChannelDetails convertChannelDetails(BaseApiRequest.ChannelDetails base) {
        return CustomerRegistrationRequest.ChannelDetails.builder()
                .channelID(base.getChannelID())
                .channelType(base.getChannelType())
                .channelSubClass(base.getChannelSubClass())
                .branchCode(base.getBranchCode())
                .channelCusHdr(CustomerRegistrationRequest.ChannelCusHdr.builder()
                        .channelProtocol(base.getChannelCusHdr().getChannelProtocol())
                        .build())
                .build();
    }

    private CustomerRegistrationRequest.DeviceDetails convertDeviceDetails(BaseApiRequest.DeviceDetails base) {
        return CustomerRegistrationRequest.DeviceDetails.builder()
                .deviceID(base.getDeviceID())
                .imeiNumber(base.getImeiNumber())
                .clientIP(base.getClientIP())
                .os(base.getOS())
                .browserType(base.getBrowserType())
                .mobileNumber(base.getMobileNumber())
                .geoLocation(CustomerRegistrationRequest.GeoLocation.builder()
                        .latitude(base.getGeoLocation().getLatitude())
                        .longitude(base.getGeoLocation().getLongitude())
                        .build())
                .build();
    }
}
