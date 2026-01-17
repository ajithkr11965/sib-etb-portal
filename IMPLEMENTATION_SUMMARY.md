# Customer Registration API Implementation Summary

## Overview
This document summarizes the implementation of the customer registration API integration that is called after captcha validation on login.

## Implementation Date
2026-01-17

## Changes Made

### 1. Encryption Service
**File:** `src/main/java/com/sib/portal/service/EncryptionService.java`
- Created AES-128-CBC encryption/decryption service
- Uses Client ID (first 32 chars) as encryption key
- Uses Client Secret (first 16 chars) as IV (Initialization Vector)
- Implements hex-encoded output format matching the API specification

### 2. DTOs Created
All DTOs are located in `src/main/java/com/sib/portal/dto/`

**Request DTOs:**
- `CustomerRegistrationRequest.java` - Main request structure with nested classes:
  - Header (Timestamp, ChannelDetails, DeviceDetails)
  - Body (UUID, merchantCode, merchantName, mobileNumber, countryCode)
- `EncryptedRequest.java` - Wrapper for encrypted request payload

**Response DTOs:**
- `CustomerRegistrationResponse.java` - Main response structure with nested classes:
  - Header (API info)
  - Status (Code, Description)
  - Body (UUID, registerType, custDetails)
  - CustomerDetails (CIF, DOB, Name, Aadhaar, PAN, accounts)
  - AccountDetails (account information)
- `EncryptedResponse.java` - Wrapper for encrypted response payload

### 3. Configuration
**File:** `src/main/resources/application.properties`

Added the following configuration properties:
```properties
api.customer.registration.url=https://apiuatgwytrust.southindianbank.com:443/sib/int/digital/rest/v1/customer/registration/details
api.customer.registration.client-id=56e784f61446d05e266de8dbcd082b26
api.customer.registration.client-secret=4886b41763f7d306537b0764a0451e88
api.customer.registration.merchant-code=MOB
api.customer.registration.merchant-name=Mobile_Banking
api.customer.registration.country-code=91
```

**Note:** For production, consider moving sensitive credentials to environment variables.

### 4. Application Configuration
**File:** `src/main/java/com/sib/portal/config/AppConfig.java`
- Created configuration class with RestTemplate and ObjectMapper beans
- These beans are required by CustomerIntegrationService

### 5. Customer Integration Service
**File:** `src/main/java/com/sib/portal/service/CustomerIntegrationService.java`

Added new method: `getCustomerRegistrationDetails(String mobileNumber)`

**Flow:**
1. Builds request payload with timestamp and UUID
2. Converts request to JSON
3. Encrypts the JSON using EncryptionService
4. Wraps encrypted data in EncryptedRequest
5. Calls API with appropriate headers (SIB-Client-Id, SIB-Client-Secret)
6. Receives encrypted response
7. Decrypts the response
8. Parses decrypted JSON to CustomerRegistrationResponse
9. Validates response status (expects "200")
10. Returns customer details

**Security Features:**
- Sensitive data masking in logs (mobile numbers, PAN, Aadhaar)
- Comprehensive error handling and logging

### 6. Auth Controller Updates
**File:** `src/main/java/com/sib/portal/controller/AuthController.java`

Updated `continueToOtp` method to:
1. Validate captcha (existing)
2. **NEW:** Call customer registration API
3. **NEW:** Store customer details in session as "CUSTOMER_REGISTRATION_DATA"
4. **NEW:** Handle API errors with user-friendly messages
5. Generate and send OTP (existing)

**Error Handling:**
- If API call fails, user sees: "Unable to verify customer details. Please try again later."
- Error is logged with full stack trace
- User journey logger records the failure

## API Integration Details

### Request Structure (Before Encryption)
```json
{
  "Request": {
    "Header": {
      "Timestamp": "20260117163727",
      "ChannelDetails": {
        "ChannelID": "MOB",
        "ChannelType": "WEB",
        "ChannelSubClass": "Retail",
        "BranchCode": "",
        "ChannelCusHdr": {
          "ChannelProtocol": ""
        }
      },
      "DeviceDetails": {
        "DeviceID": "",
        "IMEINumber": "",
        "ClientIP": "",
        "OS": "",
        "BrowserType": "",
        "MobileNumber": "",
        "GeoLocation": {
          "Latitude": "",
          "Longitude": ""
        }
      }
    },
    "Body": {
      "UUID": "5255145851155525",
      "merchantCode": "MOB",
      "merchantName": "Mobile_Banking",
      "mobileNumber": "9496807441",
      "countryCode": "91"
    }
  }
}
```

### Request Structure (After Encryption - Wire Format)
```json
{
  "Request": "hex_encoded_encrypted_string"
}
```

### HTTP Headers
- `SIB-Client-Id`: 56e784f61446d05e266de8dbcd082b26
- `SIB-Client-Secret`: 4886b41763f7d306537b0764a0451e88
- `Content-Type`: application/json

### Response Structure (After Decryption)
```json
{
  "Response": {
    "Header": {
      "Timestamp": "20260117164427",
      "APIName": "digital-registration-int-api",
      "APIVersion": "1.0.0",
      "Interface": "MobCust_Registration_APP"
    },
    "Status": {
      "Code": "200",
      "Desc": "Success"
    },
    "Body": {
      "UUID": "5255145851155525",
      "registerType": "E",
      "custDetails": [...]
    }
  }
}
```

## Usage Flow

1. **User visits login page** → Captcha is generated and displayed
2. **User enters mobile number and captcha** → Form is submitted
3. **Backend validates captcha** → If valid, proceeds
4. **Backend calls customer registration API:**
   - Encrypts request with AES-128-CBC
   - Sends to external API
   - Decrypts response
   - Validates status code
5. **Customer details stored in session** → For later use
6. **OTP is generated and sent** → User proceeds to OTP page

## Session Data Storage

The customer registration response is stored in the session with key:
```java
session.setAttribute("CUSTOMER_REGISTRATION_DATA", registrationResponse);
```

This data can be accessed in subsequent pages/controllers using:
```java
CustomerRegistrationResponse customerData =
    (CustomerRegistrationResponse) session.getAttribute("CUSTOMER_REGISTRATION_DATA");
```

## Security Considerations

### Implemented:
- ✅ AES-128-CBC encryption for API communication
- ✅ Sensitive data masking in logs
- ✅ Proper error handling without exposing internal details
- ✅ HTTPS endpoint for API communication
- ✅ Session-based storage (server-side)

### Recommendations:
- 🔸 Move credentials to environment variables for production
- 🔸 Implement request/response signing for additional security
- 🔸 Add rate limiting for API calls
- 🔸 Consider implementing circuit breaker pattern for API resilience
- 🔸 Add API call timeouts and retry logic
- 🔸 Encrypt sensitive data in session storage

## Testing Checklist

### Unit Testing (Recommended)
- [ ] Test EncryptionService.encrypt() with known input/output
- [ ] Test EncryptionService.decrypt() with known input/output
- [ ] Test request payload building in CustomerIntegrationService
- [ ] Test error handling in CustomerIntegrationService

### Integration Testing
- [ ] Test with mock API server
- [ ] Test with actual UAT API endpoint
- [ ] Test error scenarios (network failure, invalid response)
- [ ] Test with valid mobile number
- [ ] Test with invalid mobile number

### Manual Testing Steps
1. Start the application
2. Navigate to login page
3. Enter a valid mobile number (e.g., 9496807441)
4. Enter the displayed captcha
5. Check logs for:
   - "Fetching customer registration details for mobile"
   - "Calling customer registration API"
   - "Successfully fetched customer registration details"
6. Verify customer details are stored in session
7. Verify OTP page loads successfully

### Log Monitoring
Monitor these log entries:
- `CustomerIntegrationService` - API call logs
- `EncryptionService` - Encryption/decryption logs
- `AuthController` - Customer API integration logs

## API Configuration Toggle

To disable API calls during development, set:
```properties
step.api.mock.enabled=true
```

**Note:** The new customer registration API is NOT affected by this mock flag. It will always call the real API. If you need to mock this API as well, add conditional logic in the `getCustomerRegistrationDetails` method.

## Files Modified/Created

### Created:
1. `src/main/java/com/sib/portal/service/EncryptionService.java`
2. `src/main/java/com/sib/portal/dto/CustomerRegistrationRequest.java`
3. `src/main/java/com/sib/portal/dto/CustomerRegistrationResponse.java`
4. `src/main/java/com/sib/portal/dto/EncryptedRequest.java`
5. `src/main/java/com/sib/portal/dto/EncryptedResponse.java`
6. `src/main/java/com/sib/portal/config/AppConfig.java`

### Modified:
1. `src/main/resources/application.properties`
2. `src/main/java/com/sib/portal/service/CustomerIntegrationService.java`
3. `src/main/java/com/sib/portal/controller/AuthController.java`

## Troubleshooting

### Issue: "Encryption failed"
- Verify Client ID is at least 32 characters
- Verify Client Secret is at least 16 characters
- Check logs for detailed error messages

### Issue: "API returned error"
- Check API endpoint URL is correct
- Verify client-id and client-secret headers
- Check network connectivity to API endpoint
- Verify mobile number format (10 digits)
- Check API logs for detailed error response

### Issue: "Unable to verify customer details"
- This is the user-facing error message
- Check backend logs for actual error
- Common causes:
  - Network timeout
  - Invalid credentials
  - API endpoint unavailable
  - Malformed request/response

## Next Steps

1. **Testing:** Test the implementation with actual API endpoint
2. **Mock Implementation:** Consider adding a mock mode for development
3. **Enhanced Error Handling:** Add specific error messages based on API response
4. **Customer Data Display:** Use stored customer data in dashboard/other pages
5. **Session Management:** Ensure customer data is cleared on logout
6. **Performance:** Monitor API call latency and add caching if needed

## Contact
For questions or issues, refer to the implementation details in the respective service files.
