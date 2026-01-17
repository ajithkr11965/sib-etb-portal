package com.sib.portal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for customer registration details API.
 * This represents the structure before encryption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegistrationRequest {

    @JsonProperty("Request")
    private Request request;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @JsonProperty("Header")
        private Header header;

        @JsonProperty("Body")
        private Body body;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        @JsonProperty("Timestamp")
        private String timestamp;

        @JsonProperty("ChannelDetails")
        private ChannelDetails channelDetails;

        @JsonProperty("DeviceDetails")
        private DeviceDetails deviceDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelDetails {
        @JsonProperty("ChannelID")
        private String channelID;

        @JsonProperty("ChannelType")
        private String channelType;

        @JsonProperty("ChannelSubClass")
        private String channelSubClass;

        @JsonProperty("BranchCode")
        private String branchCode;

        @JsonProperty("ChannelCusHdr")
        private ChannelCusHdr channelCusHdr;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelCusHdr {
        @JsonProperty("ChannelProtocol")
        private String channelProtocol;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceDetails {
        @JsonProperty("DeviceID")
        private String deviceID;

        @JsonProperty("IMEINumber")
        private String imeiNumber;

        @JsonProperty("ClientIP")
        private String clientIP;

        @JsonProperty("OS")
        private String os;

        @JsonProperty("BrowserType")
        private String browserType;

        @JsonProperty("MobileNumber")
        private String mobileNumber;

        @JsonProperty("GeoLocation")
        private GeoLocation geoLocation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocation {
        @JsonProperty("Latitude")
        private String latitude;

        @JsonProperty("Longitude")
        private String longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {
        @JsonProperty("UUID")
        private String uuid;

        @JsonProperty("merchantCode")
        private String merchantCode;

        @JsonProperty("merchantName")
        private String merchantName;

        @JsonProperty("mobileNumber")
        private String mobileNumber;

        @JsonProperty("countryCode")
        private String countryCode;
    }
}
