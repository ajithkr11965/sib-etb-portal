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

        // Mock response for testing
        if (mockEnabled) {
            return buildMockRegistrationResponse(mobileNumber);
        }

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
     * Build mock registration response for testing.
     * Returns multiple profiles if mobile ends with '999', otherwise single profile.
     */
    private CustomerRegistrationResponse buildMockRegistrationResponse(String mobileNumber) {
        String uuid = BaseApiRequest.generateRequestUUID();
        boolean multipleProfiles = mobileNumber.endsWith("999");

        logger.info("[MOCK] Generating {} profile(s) for testing",
                multipleProfiles ? "multiple" : "single");

        // Build Response Header
        CustomerRegistrationResponse.ResponseHeader responseHeader =
                CustomerRegistrationResponse.ResponseHeader.builder()
                        .timestamp(java.time.LocalDateTime.now().format(
                                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                        .apiName("CustomerRegistration")
                        .apiVersion("1.0")
                        .interfaceName("REST")
                        .build();

        // Build Status
        CustomerRegistrationResponse.Status status =
                CustomerRegistrationResponse.Status.builder()
                        .code("200")
                        .desc("Success")
                        .build();

        // Build Customer Details
        java.util.List<CustomerRegistrationResponse.CustomerDetails> custDetailsList =
                new java.util.ArrayList<>();

        if (multipleProfiles) {
            // Profile 1 - Parent
            custDetailsList.add(buildMockCustomerDetails(
                    "A55835680",
                    "GOPIKRISHNAN T M",
                    "14-08-1992",
                    "XXXX1234",
                    "ABCDE1234F",
                    "2"
            ));

            // Profile 2 - Child
            custDetailsList.add(buildMockCustomerDetails(
                    "A55835681",
                    "GOPIKRISHNAN JUNIOR",
                    "01-05-2015",
                    "XXXX5678",
                    "",
                    "1"
            ));

            // Profile 3 - Spouse
            custDetailsList.add(buildMockCustomerDetails(
                    "A55835682",
                    "GOPIKRISHNA T M",
                    "20-03-1995",
                    "XXXX9012",
                    "FGHIJ5678K",
                    "1"
            ));
        } else {
            // Single profile
            custDetailsList.add(buildMockCustomerDetails(
                    "A55835680",
                    "GOPIKRISHNAN T M",
                    "14-08-1992",
                    "XXXX1234",
                    "ABCDE1234F",
                    "2"
            ));
        }

        // Build Response Body
        CustomerRegistrationResponse.ResponseBody responseBody =
                CustomerRegistrationResponse.ResponseBody.builder()
                        .uuid(uuid)
                        .registerType("NEW")
                        .custDetails(custDetailsList)
                        .build();

        // Build complete Response
        CustomerRegistrationResponse.Response response =
                CustomerRegistrationResponse.Response.builder()
                        .header(responseHeader)
                        .status(status)
                        .body(responseBody)
                        .build();

        return CustomerRegistrationResponse.builder()
                .response(response)
                .build();
    }

    /**
     * Build a single mock customer details object.
     */
    private CustomerRegistrationResponse.CustomerDetails buildMockCustomerDetails(
            String cifId, String custName, String dob, String aadhaar, String pan, String acctCnt) {

        // Build mock account details
        java.util.List<CustomerRegistrationResponse.AccountDetails> accountDetailsList =
                new java.util.ArrayList<>();

        // Add primary account
        accountDetailsList.add(CustomerRegistrationResponse.AccountDetails.builder()
                .foracid("12345678901234")
                .schemCode("SB01")
                .modeofOper("SINGLE")
                .acctName(custName)
                .tranFlag("Y")
                .schemType("SB")
                .schemDesc("Savings Account")
                .branchCode("0001")
                .branchName("Main Branch")
                .ifscCode("SIBL0000001")
                .build());

        // Add second account if count is 2
        if ("2".equals(acctCnt)) {
            accountDetailsList.add(CustomerRegistrationResponse.AccountDetails.builder()
                    .foracid("12345678901235")
                    .schemCode("CA01")
                    .modeofOper("SINGLE")
                    .acctName(custName)
                    .tranFlag("Y")
                    .schemType("CA")
                    .schemDesc("Current Account")
                    .branchCode("0001")
                    .branchName("Main Branch")
                    .ifscCode("SIBL0000001")
                    .build());
        }

        return CustomerRegistrationResponse.CustomerDetails.builder()
                .cifID(cifId)
                .constCode("001")
                .custDOB(dob)
                .custName(custName)
                .aadhaar(aadhaar)
                .pan(pan)
                .operativeAcctCnt(acctCnt)
                .accountDetails(accountDetailsList)
                .build();
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
