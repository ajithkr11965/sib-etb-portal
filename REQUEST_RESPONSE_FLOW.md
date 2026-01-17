# Request/Response Handling Flow

## Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. USER SUBMITS LOGIN                                          │
│    - Mobile: 9496807441                                        │
│    - Captcha: ABC123                                           │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. AUTHCONTROLLER (captcha validation)                         │
│    - Validates captcha                                          │
│    - Calls CustomerIntegrationService                           │
│    LOG: "Fetching customer registration details for            │
│          mobile: XXXXXXX7441"                                   │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. CUSTOMERINTEGRATIONSERVICE                                   │
│    - Generates UUID: WP26011716300045                          │
│    - Builds standard header (timestamp, channel, device)       │
│    - Builds body with mobile number                            │
│    LOG: "Fetching customer registration details for            │
│          mobile: XXXXXXX7441"                                   │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. REQUEST OBJECT CREATED                                       │
│    {                                                            │
│      "Request": {                                               │
│        "Header": {                                              │
│          "Timestamp": "20260117163000",                         │
│          "ChannelDetails": {...},                              │
│          "DeviceDetails": {...}                                │
│        },                                                       │
│        "Body": {                                                │
│          "UUID": "WP26011716300045",        ← Traceable!       │
│          "merchantCode": "MOB",                                │
│          "merchantName": "Mobile_Banking",                     │
│          "mobileNumber": "9496807441",                         │
│          "countryCode": "91"                                   │
│        }                                                        │
│      }                                                          │
│    }                                                            │
│    LOG: "Request JSON (before encryption): {masked JSON}"      │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. ENCRYPTEDAPISERVICE - Encryption                            │
│    - Converts JSON to string                                    │
│    - Encrypts with AES-128-CBC                                  │
│    - Result: "a4f5b2c8d1e7f3a9..." (hex string)               │
│    LOG: "Encrypted request (first 50 chars): a4f5b2c8d1..."   │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. HTTP REQUEST SENT                                            │
│    POST https://apiuatgwytrust.southindianbank.com:443/...    │
│    Headers:                                                     │
│      - SIB-Client-Id: 56e784f61446d05e266de8dbcd082b26        │
│      - SIB-Client-Secret: 4886b41763f7d306537b0764a0451e88    │
│      - Content-Type: application/json                          │
│    Body:                                                        │
│    {                                                            │
│      "Request": "a4f5b2c8d1e7f3a9..."  ← Encrypted            │
│    }                                                            │
│    LOG: "Calling encrypted API: https://apiuatgwytrust..."    │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. EXTERNAL API (ESB/Backend)                                  │
│    - Receives encrypted request                                 │
│    - Decrypts using same key/IV                                │
│    - Extracts UUID: WP26011716300045                           │
│    - Processes request                                          │
│    - Queries database for mobile 9496807441                    │
│    - Finds customer: GOPIKRISHNAN T M                          │
│    ESB LOG: "Processing request UUID: WP26011716300045"        │
│    ESB LOG: "Mobile: XXXXXXX7441, CIF: A55835680"             │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 8. EXTERNAL API - Response Created                             │
│    {                                                            │
│      "Response": {                                              │
│        "Header": {                                              │
│          "Timestamp": "20260117163002",                         │
│          "APIName": "digital-registration-int-api",            │
│          "APIVersion": "1.0.0"                                 │
│        },                                                       │
│        "Status": {                                              │
│          "Code": "200",                                         │
│          "Desc": "Success"                                     │
│        },                                                       │
│        "Body": {                                                │
│          "UUID": "WP26011716300045",       ← Same UUID!        │
│          "registerType": "E",                                  │
│          "custDetails": [                                      │
│            {                                                    │
│              "cifID": "A55835680",                             │
│              "custName": "GOPIKRISHNAN T M",                   │
│              "custDOB": "14-08-1992",                          │
│              "accountDetails": [...]                           │
│            }                                                    │
│          ]                                                      │
│        }                                                        │
│      }                                                          │
│    }                                                            │
│    - Encrypts response                                          │
│    - Returns encrypted data                                     │
│    ESB LOG: "Response sent for UUID: WP26011716300045"         │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 9. HTTP RESPONSE RECEIVED                                       │
│    Status: 200 OK                                               │
│    Body:                                                        │
│    {                                                            │
│      "Response": "b7c3a1f9e2d8..."  ← Encrypted                │
│    }                                                            │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 10. ENCRYPTEDAPISERVICE - Decryption                           │
│    - Receives encrypted response                                │
│    - Decrypts with AES-128-CBC                                  │
│    - Parses JSON to CustomerRegistrationResponse object        │
│    LOG: "Encrypted response (first 50 chars): b7c3a1f9e2..."  │
│    LOG: "Decrypted response: {masked JSON}"                    │
│    LOG: "Successfully completed encrypted API call to: ..."    │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 11. CUSTOMERINTEGRATIONSERVICE - Validation                    │
│    - Checks response status code                                │
│    - Validates: status.code == "200"                           │
│    LOG: "API Response Status: 200 - Success"                   │
│    LOG: "Successfully fetched customer registration details"   │
│    - Returns CustomerRegistrationResponse object               │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 12. AUTHCONTROLLER - Store in Session                          │
│    - Stores full response in session:                          │
│      session.setAttribute("CUSTOMER_REGISTRATION_DATA",        │
│                          registrationResponse)                 │
│    LOG: "Customer registration details fetched and             │
│          stored in session"                                    │
│    - Generates OTP                                              │
│    - Redirects to /otp page                                    │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ 13. USER SEES OTP PAGE                                          │
│    - OTP sent to mobile: XXXXXXX441                            │
│    - Session contains customer data for later use              │
└─────────────────────────────────────────────────────────────────┘
```

## UUID Tracking Through Logs

### Application Logs (Your Portal)

```
2026-01-17 16:30:00.123 INFO  [AuthController]
  Fetching customer registration details for mobile: XXXXXXX7441

2026-01-17 16:30:00.125 INFO  [CustomerIntegrationService]
  Fetching customer registration details for mobile: XXXXXXX7441

2026-01-17 16:30:00.127 DEBUG [EncryptedApiService]
  Request JSON (before encryption): {"Request":{"Header":{...},"Body":{"UUID":"WP26011716300045",...}}}

2026-01-17 16:30:00.145 DEBUG [EncryptedApiService]
  Encrypted request (first 50 chars): a4f5b2c8d1e7f3a9b5c2f1d8e4a7b3c6d9f2e5a1b7c4...

2026-01-17 16:30:00.146 INFO  [EncryptedApiService]
  Calling encrypted API: https://apiuatgwytrust.southindianbank.com:443/...

2026-01-17 16:30:00.678 DEBUG [EncryptedApiService]
  Encrypted response (first 50 chars): b7c3a1f9e2d8c5b2f1a4e7d3c6b9f2e5a1c4b7d8...

2026-01-17 16:30:00.695 DEBUG [EncryptedApiService]
  Decrypted response: {"Response":{"Status":{"Code":"200"},...}}

2026-01-17 16:30:00.698 INFO  [EncryptedApiService]
  Successfully completed encrypted API call to: https://apiuatgwytrust...

2026-01-17 16:30:00.699 INFO  [CustomerIntegrationService]
  API Response Status: 200 - Success

2026-01-17 16:30:00.700 INFO  [CustomerIntegrationService]
  Successfully fetched customer registration details

2026-01-17 16:30:00.702 INFO  [AuthController]
  Customer registration details fetched and stored in session
```

### ESB/Backend Logs

```
2026-01-17 16:30:00.150 INFO  [ESB-Gateway]
  Received encrypted request from Web Portal

2026-01-17 16:30:00.165 INFO  [ESB-Gateway]
  Decrypted request - UUID: WP26011716300045

2026-01-17 16:30:00.167 INFO  [ESB-Gateway]
  Routing to: digital-registration-int-api

2026-01-17 16:30:00.245 INFO  [Registration-API]
  Processing UUID: WP26011716300045, Mobile: XXXXXXX7441

2026-01-17 16:30:00.567 INFO  [Registration-API]
  Found customer CIF: A55835680 for UUID: WP26011716300045

2026-01-17 16:30:00.645 INFO  [ESB-Gateway]
  Response encrypted for UUID: WP26011716300045

2026-01-17 16:30:00.650 INFO  [ESB-Gateway]
  Response sent - UUID: WP26011716300045, Status: 200, Duration: 500ms
```

## Error Handling Scenarios

### Scenario 1: Network Timeout

```
┌─────────────────────────────────────────────────────────────────┐
│ HTTP Request → ⏱️ Timeout (no response)                         │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ ENCRYPTEDAPISERVICE                                             │
│   - Catches Exception                                            │
│   LOG: "Error calling encrypted API                             │
│         https://apiuatgwytrust...: Connection timeout"         │
│   - Throws Exception                                             │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ CUSTOMERINTEGRATIONSERVICE                                      │
│   - Catches Exception                                            │
│   LOG: "Error fetching customer registration details:           │
│         Connection timeout"                                     │
│   - Throws Exception with user-friendly message                 │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ AUTHCONTROLLER                                                  │
│   - Catches Exception                                            │
│   LOG: "Error fetching customer registration details:           │
│         Failed to fetch customer details: Connection timeout"  │
│   - Sets error on form:                                         │
│     "Unable to verify customer details.                        │
│      Please try again later."                                  │
│   - Returns to login page with error message                    │
└─────────────────────────────────────────────────────────────────┘
```

### Scenario 2: Customer Not Found

```
┌─────────────────────────────────────────────────────────────────┐
│ API Returns:                                                    │
│ {                                                               │
│   "Response": {                                                 │
│     "Status": {                                                 │
│       "Code": "404",                                            │
│       "Desc": "Customer not found"                             │
│     }                                                           │
│   }                                                             │
│ }                                                               │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ CUSTOMERINTEGRATIONSERVICE                                      │
│   - Validates status code                                        │
│   - if (!"200".equals(statusCode))                             │
│   LOG: "API Response Status: 404 - Customer not found"         │
│   - Throws Exception: "API returned error:                      │
│                       404 - Customer not found"                │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ AUTHCONTROLLER                                                  │
│   - Catches Exception                                            │
│   - Shows error to user:                                        │
│     "Unable to verify customer details.                        │
│      Please try again later."                                  │
│   - User can retry or contact support                           │
└─────────────────────────────────────────────────────────────────┘
```

### Scenario 3: Decryption Failed

```
┌─────────────────────────────────────────────────────────────────┐
│ ENCRYPTEDAPISERVICE - Decryption                               │
│   - Receives malformed encrypted response                       │
│   - encryptionService.decrypt() throws Exception               │
│   LOG: "Error calling encrypted API: Decryption failed"        │
│   - Throws Exception                                            │
└────────────────┬────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│ AUTHCONTROLLER                                                  │
│   - Shows generic error to user                                 │
│   - Logs full stack trace for debugging                         │
└─────────────────────────────────────────────────────────────────┘
```

## Session Data Storage

### What Gets Stored

```java
// Full response object stored in session
CustomerRegistrationResponse response = {
  "Response": {
    "Header": {...},
    "Status": {"Code": "200", "Desc": "Success"},
    "Body": {
      "UUID": "WP26011716300045",
      "registerType": "E",
      "custDetails": [
        {
          "cifID": "A55835680",
          "custName": "GOPIKRISHNAN T M",
          "custDOB": "14-08-1992",
          "aadhaar": "304350786293",
          "pan": "CQKPM1094G",
          "accountDetails": [...]
        }
      ]
    }
  }
}

session.setAttribute("CUSTOMER_REGISTRATION_DATA", response);
```

### How to Access Later

```java
// In any controller/service that has access to HttpSession

// Get the full response
CustomerRegistrationResponse customerData =
    (CustomerRegistrationResponse) session.getAttribute("CUSTOMER_REGISTRATION_DATA");

// Access customer details
if (customerData != null && customerData.getResponse() != null) {
    List<CustomerDetails> custDetails =
        customerData.getResponse().getBody().getCustDetails();

    if (!custDetails.isEmpty()) {
        CustomerDetails customer = custDetails.get(0);
        String cifID = customer.getCifID();
        String name = customer.getCustName();
        String dob = customer.getCustDOB();
        List<AccountDetails> accounts = customer.getAccountDetails();

        // Use this data in your business logic
    }
}
```

## Correlation for Troubleshooting

When ESB team reports an issue with UUID `WP26011716300045`:

### 1. Search Your Application Logs

```bash
grep "WP26011716300045" application.log
```

**Output:**
```
2026-01-17 16:30:00.127 DEBUG Request JSON: ...UUID":"WP26011716300045"...
2026-01-17 16:30:00.146 INFO  Calling encrypted API: https://apiuatgwytrust...
2026-01-17 16:30:00.698 INFO  Successfully completed encrypted API call
```

### 2. Check ESB Logs

```bash
grep "WP26011716300045" esb.log
```

**Output:**
```
2026-01-17 16:30:00.165 INFO  Decrypted request - UUID: WP26011716300045
2026-01-17 16:30:00.245 INFO  Processing UUID: WP26011716300045
2026-01-17 16:30:00.650 INFO  Response sent - UUID: WP26011716300045
```

### 3. Full Correlation

```
Your Portal:    16:30:00.146 → Request sent
ESB Gateway:    16:30:00.165 → Request received (19ms delay)
Backend API:    16:30:00.245 → Processing started (80ms delay)
Backend API:    16:30:00.567 → Processing completed (322ms processing)
ESB Gateway:    16:30:00.650 → Response sent (83ms delay)
Your Portal:    16:30:00.678 → Response received (28ms delay)

Total time: 532ms
```

## Response Validation Checklist

```java
// In CustomerIntegrationService
CustomerRegistrationResponse response = encryptedApiService.callEncryptedApi(...);

// ✓ Check 1: Response not null
if (response == null) {
    throw new Exception("Empty response");
}

// ✓ Check 2: Response wrapper not null
if (response.getResponse() == null) {
    throw new Exception("Empty response wrapper");
}

// ✓ Check 3: Status not null
if (response.getResponse().getStatus() == null) {
    throw new Exception("Missing status");
}

// ✓ Check 4: Status code is 200
String statusCode = response.getResponse().getStatus().getCode();
if (!"200".equals(statusCode)) {
    throw new Exception("API returned error: " + statusCode);
}

// ✓ Check 5: Body contains data
if (response.getResponse().getBody() == null) {
    logger.warn("Empty body in response");
}

// ✓ All checks passed - return response
return response;
```

## Performance Monitoring

### Key Metrics to Track

```java
// Wrap API call with timing
long startTime = System.currentTimeMillis();

CustomerRegistrationResponse response =
    encryptedApiService.callEncryptedApi(...);

long duration = System.currentTimeMillis() - startTime;

logger.info("API call completed in {}ms for UUID: {}",
    duration,
    request.getBody().getUuid());

// Alert if > 1000ms
if (duration > 1000) {
    logger.warn("Slow API response: {}ms", duration);
}
```

### Metrics to Monitor

1. **Request Count**: Total API calls per hour
2. **Success Rate**: % of 200 responses
3. **Error Rate**: % of non-200 responses
4. **Response Time**: Average/P95/P99 latency
5. **Timeout Rate**: % of connection timeouts

## Summary

**Key Points:**

1. **UUID Tracking**: `WP26011716300045` flows through entire system
2. **Encryption**: Handled automatically by `EncryptedApiService`
3. **Error Handling**: Three layers (Service → Integration → Controller)
4. **Session Storage**: Full response stored for later use
5. **Logging**: Comprehensive logs at each step for debugging
6. **Validation**: Multiple checks before returning data
7. **Correlation**: UUID enables end-to-end request tracking
