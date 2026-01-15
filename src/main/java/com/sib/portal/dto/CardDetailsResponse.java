package com.sib.portal.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDetailsResponse {
    private String cardId;
    private String maskedCardNumber;
    private String cardType; // e.g., VISA_CLASSIC, VISA_PLATINUM
    private String status;
    private String expiryDate;
}
