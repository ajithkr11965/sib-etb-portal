package com.sib.portal.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileResponse {
    private String customerId;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String kycStatus;
}
