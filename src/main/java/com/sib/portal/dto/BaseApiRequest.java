package com.sib.portal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Base request structure for all API calls.
 * Provides common Header structure and builder methods.
 * Extend this for specific API requests by adding your custom Body.
 */
public class BaseApiRequest {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter UUID_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final Random RANDOM = new Random();

    // Application identifier for request tracking
    private static final String APP_IDENTIFIER = "WP"; // WP = Web Portal

    /**
     * Standard Header structure used across all APIs.
     */
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

    /**
     * Builder for creating standard API Header with default values.
     */
    public static class HeaderBuilder {

        /**
         * Build a standard header with current timestamp and default channel details.
         */
        public static Header buildStandardHeader() {
            return Header.builder()
                    .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMAT))
                    .channelDetails(buildDefaultChannelDetails())
                    .deviceDetails(buildDefaultDeviceDetails())
                    .build();
        }

        /**
         * Build a header with custom device details.
         */
        public static Header buildHeaderWithDeviceInfo(String clientIP, String os, String browserType) {
            return Header.builder()
                    .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMAT))
                    .channelDetails(buildDefaultChannelDetails())
                    .deviceDetails(DeviceDetails.builder()
                            .deviceID("")
                            .imeiNumber("")
                            .clientIP(clientIP)
                            .os(os)
                            .browserType(browserType)
                            .mobileNumber("")
                            .geoLocation(GeoLocation.builder()
                                    .latitude("")
                                    .longitude("")
                                    .build())
                            .build())
                    .build();
        }

        /**
         * Build default channel details (WEB channel).
         */
        private static ChannelDetails buildDefaultChannelDetails() {
            return ChannelDetails.builder()
                    .channelID("MOB")
                    .channelType("WEB")
                    .channelSubClass("Retail")
                    .branchCode("")
                    .channelCusHdr(ChannelCusHdr.builder()
                            .channelProtocol("")
                            .build())
                    .build();
        }

        /**
         * Build default device details (empty values).
         */
        private static DeviceDetails buildDefaultDeviceDetails() {
            return DeviceDetails.builder()
                    .deviceID("")
                    .imeiNumber("")
                    .clientIP("")
                    .os("")
                    .browserType("")
                    .mobileNumber("")
                    .geoLocation(GeoLocation.builder()
                            .latitude("")
                            .longitude("")
                            .build())
                    .build();
        }
    }

    /**
     * Generate a 16-character traceable UUID for request tracking.
     *
     * Format: WPYYMMDDHHMMSSRR
     * - WP: Application identifier (Web Portal)
     * - YYMMDDHHMMSS: Timestamp (12 digits)
     * - RR: Random digits (2 digits) for uniqueness
     *
     * Example: WP26011716300045
     * - WP: Web Portal
     * - 260117: January 17, 2026
     * - 163000: 4:30:00 PM
     * - 45: Random digits
     *
     * This pattern allows ESB team to:
     * 1. Identify the source application (WP)
     * 2. Know when the request was made (timestamp)
     * 3. Track unique requests (random suffix)
     *
     * @return 16-character traceable UUID
     */
    public static String generateRequestUUID() {
        // Get timestamp in YYMMDDHHMMSS format (12 digits)
        String timestamp = LocalDateTime.now().format(UUID_TIMESTAMP_FORMAT);

        // Generate 2 random digits for uniqueness
        int randomSuffix = RANDOM.nextInt(100); // 0-99
        String randomStr = String.format("%02d", randomSuffix);

        // Combine: APP_IDENTIFIER (2) + timestamp (12) + random (2) = 16 chars
        return APP_IDENTIFIER + timestamp + randomStr;
    }
}
