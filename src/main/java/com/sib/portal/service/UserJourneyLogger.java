package com.sib.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserJourneyLogger {

    private static final Logger logger = LoggerFactory.getLogger("UserJourney");

    public void logLoginAttempt(String mobile) {
        log("LOGIN_ATTEMPT", mobile, "User attempting to login");
    }

    public void logLoginSuccess(String mobile) {
        log("LOGIN_SUCCESS", mobile, "User logged in successfully");
    }

    public void logLoginFailure(String mobile, String reason) {
        log("LOGIN_FAILURE", mobile, "Login failed: " + reason);
    }

    public void logPageVisit(String mobile, String pageName) {
        log("PAGE_VISIT", mobile, "Visited page: " + pageName);
    }

    public void logTransactionInitiated(String mobile, String transactionType) {
        log("TX_INITIATED", mobile, "Started transaction: " + transactionType);
    }

    public void logTransactionSuccess(String mobile, String transactionType) {
        log("TX_SUCCESS", mobile, "Transaction successful: " + transactionType);
    }

    public void logTransactionFailure(String mobile, String transactionType, String reason) {
        log("TX_FAILURE", mobile, "Transaction failed: " + transactionType + " - " + reason);
    }

    private void log(String stage, String mobile, String message) {
        // Mask mobile for privacy in logs
        String maskedMobile = (mobile != null && mobile.length() > 4)
                ? "XXXXXXX" + mobile.substring(mobile.length() - 4)
                : mobile;

        // Structure: [TIMESTAMP] [STAGE] [USER] [MESSAGE]
        logger.info("[{}] [{}] [{}] {}", LocalDateTime.now(), stage, maskedMobile, message);
    }
}
