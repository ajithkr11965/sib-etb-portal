package com.sib.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_VALIDITY_MINUTES = 2;
    private final AuthIntegrationService authIntegrationService;

    public OtpService(AuthIntegrationService authIntegrationService) {
        this.authIntegrationService = authIntegrationService;
    }

    public String generateOtp(String mobileNumber) {
        return authIntegrationService.generateOtp(mobileNumber);
    }

    public boolean validateOtp(String sessionOtp, String inputOtp, LocalDateTime generatedTime) {
        if (sessionOtp == null || inputOtp == null) {
            return false;
        }

        // Check expiration (2 minutes)
        if (generatedTime == null || generatedTime.plusMinutes(OTP_VALIDITY_MINUTES).isBefore(LocalDateTime.now())) {
            logger.warn("OTP expired");
            return false;
        }

        // Magic OTP for Dev is handled in AuthIntegrationService if mock enabled,
        // but we can keep a local check effectively or just delegate.
        // Delegate fully to integration service for validation logic.
        return authIntegrationService.validateOtp(null, inputOtp) || sessionOtp.equals(inputOtp);
    }
}
