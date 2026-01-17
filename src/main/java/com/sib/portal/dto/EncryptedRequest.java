package com.sib.portal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper for encrypted request payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedRequest {

    @JsonProperty("Request")
    private String request;
}
