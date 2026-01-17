package com.sib.portal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for customer registration details API.
 * This represents the decrypted response structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegistrationResponse {

    @JsonProperty("Response")
    private Response response;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @JsonProperty("Header")
        private ResponseHeader header;

        @JsonProperty("Status")
        private Status status;

        @JsonProperty("Body")
        private ResponseBody body;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseHeader {
        @JsonProperty("Timestamp")
        private String timestamp;

        @JsonProperty("APIName")
        private String apiName;

        @JsonProperty("APIVersion")
        private String apiVersion;

        @JsonProperty("Interface")
        private String interfaceName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        @JsonProperty("Code")
        private String code;

        @JsonProperty("Desc")
        private String desc;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseBody {
        @JsonProperty("UUID")
        private String uuid;

        @JsonProperty("registerType")
        private String registerType;

        @JsonProperty("custDetails")
        private List<CustomerDetails> custDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDetails {
        @JsonProperty("cifID")
        private String cifID;

        @JsonProperty("constCode")
        private String constCode;

        @JsonProperty("custDOB")
        private String custDOB;

        @JsonProperty("custName")
        private String custName;

        @JsonProperty("aadhaar")
        private String aadhaar;

        @JsonProperty("pan")
        private String pan;

        @JsonProperty("operativeAcctCnt")
        private String operativeAcctCnt;

        @JsonProperty("accountDetails")
        private List<AccountDetails> accountDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountDetails {
        @JsonProperty("foracid")
        private String foracid;

        @JsonProperty("schemCode")
        private String schemCode;

        @JsonProperty("modeofOper")
        private String modeofOper;

        @JsonProperty("acctName")
        private String acctName;

        @JsonProperty("tranFlag")
        private String tranFlag;

        @JsonProperty("schemType")
        private String schemType;

        @JsonProperty("schemDesc")
        private String schemDesc;

        @JsonProperty("branchCode")
        private String branchCode;

        @JsonProperty("branchName")
        private String branchName;

        @JsonProperty("ifscCode")
        private String ifscCode;
    }
}
