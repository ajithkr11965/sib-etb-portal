package com.sib.portal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper for encrypted response payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedResponse {

    @JsonProperty("Response")
    private String response;
}
