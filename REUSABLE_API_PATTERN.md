# Reusable Encrypted API Pattern

## Overview
Most APIs follow the same structure where only the **Body** section changes. We've created reusable components to simplify integration of new APIs significantly.

## Key Components

### 1. EncryptedApiService
**Location:** `src/main/java/com/sib/portal/service/EncryptedApiService.java`

Generic service that handles:
- Request encryption (AES-128-CBC)
- HTTP call with proper headers
- Response decryption
- Error handling and logging
- Sensitive data masking

**Usage:**
```java
CustomerRegistrationResponse response = encryptedApiService.callEncryptedApi(
    url,
    requestObject,
    ResponseClass.class
);
```

### 2. BaseApiRequest
**Location:** `src/main/java/com/sib/portal/dto/BaseApiRequest.java`

Provides:
- Common Header structure (Timestamp, ChannelDetails, DeviceDetails)
- HeaderBuilder with default values
- Helper methods for UUID generation

**Usage:**
```java
// Get standard header
BaseApiRequest.Header header = BaseApiRequest.HeaderBuilder.buildStandardHeader();

// Generate UUID
String uuid = BaseApiRequest.generateRequestUUID();
```

## Adding a New API (Quick Guide)

### Step 1: Create Body DTO (Only the unique part!)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Body {
    @JsonProperty("UUID")
    private String uuid;

    @JsonProperty("fieldName1")
    private String fieldName1;

    @JsonProperty("fieldName2")
    private String fieldName2;

    // Add your API-specific fields here
}
```

### Step 2: Add Configuration

```properties
api.your-api.url=https://apiuatgwytrust.southindianbank.com:443/your/endpoint
```

### Step 3: Create Integration Service

```java
@Service
public class YourIntegrationService {

    private final EncryptedApiService encryptedApiService;

    @Value("${api.your-api.url}")
    private String apiUrl;

    public YourIntegrationService(EncryptedApiService encryptedApiService) {
        this.encryptedApiService = encryptedApiService;
    }

    public YourResponse callYourApi(String param) throws Exception {
        // Build request
        YourRequest request = buildRequest(param);

        // Call API (encryption/decryption handled automatically!)
        YourResponse response = encryptedApiService.callEncryptedApi(
            apiUrl,
            request,
            YourResponse.class
        );

        // Validate and return
        return response;
    }

    private YourRequest buildRequest(String param) {
        BaseApiRequest.Header header = BaseApiRequest.HeaderBuilder.buildStandardHeader();

        YourRequest.Body body = YourRequest.Body.builder()
            .uuid(BaseApiRequest.generateRequestUUID())
            .fieldName1(param)
            .build();

        return YourRequest.builder()
            .request(YourRequest.Request.builder()
                .header(header)
                .body(body)
                .build())
            .build();
    }
}
```

## Benefits

**Before (Manual):**
- ~150 lines of code per API
- Duplicate encryption/decryption logic
- Duplicate error handling
- Manual header construction

**After (Using Pattern):**
- ~30-40 lines of code per API
- Reuse EncryptedApiService
- Consistent error handling
- Automatic header generation

## Pattern Flow

```
┌─────────────────────────────────────────┐
│  Your Integration Service               │
│  1. Build Body (unique to your API)    │
│  2. Use BaseApiRequest for Header      │
│  3. Call encryptedApiService           │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  EncryptedApiService (Generic)          │
│  • JSON conversion                      │
│  • AES-128-CBC encryption               │
│  • HTTP call with headers               │
│  • Response decryption                  │
│  • Error handling                       │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  External API                           │
└─────────────────────────────────────────┘
```

## Example: Customer Registration API

See `CustomerIntegrationService.java` for a working implementation that demonstrates:
- Using EncryptedApiService for API calls
- Building requests with BaseApiRequest
- Error handling and validation
- Logging best practices

## Common APIs You Can Integrate

All following APIs use the same pattern - only the Body changes:

1. ✅ Customer Registration (implemented)
2. Account Balance
3. Transaction History
4. Fund Transfer
5. Statement Request
6. Card Activation
7. OTP Generation
8. KYC Verification

For each API, you only need to:
1. Define the Body structure (10-20 lines)
2. Create integration service (~30 lines)
3. Call `encryptedApiService.callEncryptedApi()`

That's it!
