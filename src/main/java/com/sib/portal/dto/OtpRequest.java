package com.sib.portal.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class OtpRequest {

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    private String otp;

    // Hidden field to carry forward mobile if needed, or rely on session
    private String mobileNumber;
}
