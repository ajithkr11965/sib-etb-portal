package com.sib.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthIntegrationService {

    private final ExternalApiService apiService;
    private static final Logger logger = LoggerFactory.getLogger(AuthIntegrationService.class);

    @Value("${step.api.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${api.base-url.auth}")
    private String authBaseUrl;

    public AuthIntegrationService(ExternalApiService apiService) {
        this.apiService = apiService;
    }

    public String generateOtp(String mobileNumber) {
        if (mockEnabled) {
            String mockOtp = "111111"; // Magic OTP
            logger.info("Mock OTP generated for {}: {}", mobileNumber, mockOtp);
            return mockOtp;
        }

        // Real API Call
        // return apiService.post(authBaseUrl + "/otp/generate", new
        // OtpGenerationRequest(mobileNumber), OtpResponse.class).getOtp();
        return null;
    }

    public boolean validateOtp(String mobileNumber, String otp) {
        if (mockEnabled) {
            // Basic validation for mock
            return "111111".equals(otp);
        }

        // Real API Call
        // return apiService.post(authBaseUrl + "/otp/validate", new
        // OtpValidationRequest(mobileNumber, otp), Boolean.class);
        return false;
    }
}
