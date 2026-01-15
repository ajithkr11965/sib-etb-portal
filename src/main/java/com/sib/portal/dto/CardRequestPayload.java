package com.sib.portal.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequestPayload {
    private String customerId;
    private String accountId;
    private String cardVariant; // CLASSIC, PREMIUM
    private String deliveryAddressType; // PERMANENT, COMMUNICATION
}
