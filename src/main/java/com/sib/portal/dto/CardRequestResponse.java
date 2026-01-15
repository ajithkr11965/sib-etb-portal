package com.sib.portal.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequestResponse {
    private String requestId;
    private String status; // SUBMITTED, PENDING
    private String message;
}
