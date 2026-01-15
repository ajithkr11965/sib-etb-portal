package com.sib.portal.service;

import com.sib.portal.dto.CardDetailsResponse;
import com.sib.portal.dto.CardRequestPayload;
import com.sib.portal.dto.CardRequestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class CardIntegrationService {

    private final ExternalApiService apiService;

    @Value("${step.api.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${api.base-url.cards}")
    private String cardsBaseUrl;

    public CardIntegrationService(ExternalApiService apiService) {
        this.apiService = apiService;
    }

    public List<CardDetailsResponse> getCardsForAccount(String accountId) {
        if (mockEnabled) {
            // Return dummy data structure
            return Arrays.asList(
                    new CardDetailsResponse("1", "XXXX-XXXX-XXXX-1234", "VISA_CLASSIC", "ACTIVE", "12/28"),
                    new CardDetailsResponse("2", "XXXX-XXXX-XXXX-5678", "RUPAY_PLATINUM", "BLOCKED", "05/25"));
        }

        // Real API Call (when enabled)
        // return Arrays.asList(apiService.get(cardsBaseUrl + "/account/" + accountId,
        // CardDetailsResponse[].class));
        return null;
    }

    public CardRequestResponse submitCardRequest(CardRequestPayload payload) {
        if (mockEnabled) {
            return new CardRequestResponse("REQ-" + UUID.randomUUID().toString().substring(0, 8), "SUCCESS",
                    "Card request submitted successfully");
        }

        return apiService.post(cardsBaseUrl + "/request", payload, CardRequestResponse.class);
    }
}
