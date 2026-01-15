package com.sib.portal.service;

import com.sib.portal.dto.CustomerProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CustomerIntegrationService {

    private final ExternalApiService apiService;

    @Value("${step.api.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${api.base-url.customer}")
    private String customerBaseUrl;

    public CustomerIntegrationService(ExternalApiService apiService) {
        this.apiService = apiService;
    }

    public CustomerProfileResponse getCustomerProfile(String mobileNumber) {
        if (mockEnabled) {
            return new CustomerProfileResponse("CUST001", "Samuel John", "samueljohn@gmail.com", mobileNumber,
                    "VERIFIED");
        }

        return apiService.get(customerBaseUrl + "/profile?mobile=" + mobileNumber, CustomerProfileResponse.class);
    }
}
