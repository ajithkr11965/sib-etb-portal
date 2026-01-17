# Multiple Customer Profile Handling

## Problem Statement

A single mobile number may be registered to multiple customers:
- Parent and child accounts
- Joint account holders
- Primary and supplementary cardholders
- Family banking relationships

When `custDetails[]` contains multiple records, we need to let the user select which profile to use.

## Current vs Proposed Flow

### Current Flow (Single Profile)
```
User Login → Captcha → API Call → OTP → Validate OTP → Dashboard
                                    ↓
                          (Uses first custDetails[0])
```

### Proposed Flow (Multiple Profiles) - CORRECTED
```
User Login → Captcha → API Call → OTP → Validate OTP → Check custDetails.length
                                                                 ↓
                                                ┌────────────────┴────────────────┐
                                                ↓                                 ↓
                                          Single Profile                   Multiple Profiles
                                                ↓                                 ↓
                                            Dashboard                    Profile Selection Page
                                                                                  ↓
                                                                              Dashboard
```

### Key Point
**OTP is sent IMMEDIATELY after API validates phone number, regardless of profile count.**
Profile selection happens AFTER OTP validation, not before.

## Implementation Plan

### Phase 1: Detection & Storage

**1.1 Update AuthController.java - continueToOtp() method**
```java
// After API call - ALWAYS generate OTP regardless of profile count
CustomerRegistrationResponse registrationResponse =
    customerIntegrationService.getCustomerRegistrationDetails(mobileNumber);

List<CustomerDetails> custDetails = registrationResponse.getResponse()
    .getBody().getCustDetails();

// Store in session
session.setAttribute("CUSTOMER_REGISTRATION_DATA", registrationResponse);
session.setAttribute("CUSTOMER_COUNT", custDetails.size());

// Log profile count
logger.info("Found {} customer profile(s) for mobile: XXXXXXX{}",
    custDetails.size(),
    mobileNumber.substring(mobileNumber.length() - 4));

// ALWAYS generate OTP - phone validation is the same regardless of profile count
String otp = otpService.generateOtp(mobileNumber);
session.setAttribute("SESSION_OTP", otp);
session.setAttribute("SESSION_MOBILE", mobileNumber);
session.setAttribute("SESSION_OTP_TIME", LocalDateTime.now());
session.setAttribute("SESSION_OTP_ATTEMPTS", 0);

// Profile selection will happen AFTER OTP validation
return "redirect:/otp";
```

**1.2 Update AuthController.java - validateOtp() method**
```java
@PostMapping("/validate-otp")
public String validateOtp(@Valid @ModelAttribute("otpRequest") OtpRequest otpRequest,
        BindingResult bindingResult,
        HttpSession session,
        Model model) {

    String sessionOtp = (String) session.getAttribute("SESSION_OTP");
    LocalDateTime generatedTime = (LocalDateTime) session.getAttribute("SESSION_OTP_TIME");
    String mobile = (String) session.getAttribute("SESSION_MOBILE");
    Integer attempts = (Integer) session.getAttribute("SESSION_OTP_ATTEMPTS");

    if (mobile == null) {
        return "redirect:/";
    }

    // ... existing OTP validation logic ...

    if (!otpService.validateOtp(sessionOtp, otpRequest.getOtp(), generatedTime)) {
        bindingResult.rejectValue("otp", "error.otp", "Invalid or Expired OTP");
        model.addAttribute("mobile", maskMobile(mobile));
        userJourneyLogger.logLoginFailure(mobile, "Invalid OTP");
        return "otp";
    }

    // SUCCESS - OTP validated
    // Set Spring Security Context
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            mobile, otpRequest.getOtp(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    userJourneyLogger.logLoginSuccess(mobile);

    // NOW check for multiple profiles
    Integer customerCount = (Integer) session.getAttribute("CUSTOMER_COUNT");

    if (customerCount != null && customerCount > 1) {
        // Multiple profiles - redirect to selection page
        logger.info("Multiple profiles detected, redirecting to profile selection");
        session.setAttribute("PROFILE_SELECTION_REQUIRED", true);
        return "redirect:/select-profile";
    } else {
        // Single profile - auto-select and go to dashboard
        CustomerRegistrationResponse customerData =
            (CustomerRegistrationResponse) session.getAttribute("CUSTOMER_REGISTRATION_DATA");

        if (customerData != null && customerData.getResponse() != null
                && customerData.getResponse().getBody() != null) {
            List<CustomerDetails> custDetails = customerData.getResponse().getBody().getCustDetails();
            if (!custDetails.isEmpty()) {
                session.setAttribute("SELECTED_PROFILE", custDetails.get(0));
                session.setAttribute("SELECTED_CIF_ID", custDetails.get(0).getCifID());
                logger.info("Auto-selected single profile: {}", custDetails.get(0).getCifID());
            }
        }

        return "redirect:/dashboard";
    }
}
```

### Phase 2: Profile Selection Page

**2.1 Create ProfileSelectionRequest.java**
```java
package com.sib.portal.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class ProfileSelectionRequest {
    @NotBlank(message = "Please select a profile")
    private String selectedCifId;
}
```

**2.2 Add endpoint in AuthController.java**
```java
@GetMapping("/select-profile")
public String selectProfilePage(Model model, HttpSession session) {
    // Check if profile selection is required
    Boolean selectionRequired = (Boolean) session.getAttribute("PROFILE_SELECTION_REQUIRED");
    if (selectionRequired == null || !selectionRequired) {
        return "redirect:/login";
    }

    // Get customer data from session
    CustomerRegistrationResponse customerData =
        (CustomerRegistrationResponse) session.getAttribute("CUSTOMER_REGISTRATION_DATA");

    if (customerData == null) {
        return "redirect:/login";
    }

    List<CustomerDetails> profiles = customerData.getResponse()
        .getBody().getCustDetails();

    model.addAttribute("profiles", profiles);
    model.addAttribute("profileCount", profiles.size());
    model.addAttribute("selectionRequest", new ProfileSelectionRequest());

    String mobile = (String) session.getAttribute("SESSION_MOBILE");
    model.addAttribute("mobile", maskMobile(mobile));

    userJourneyLogger.logPageVisit(mobile, "Profile Selection Page");

    return "select-profile";
}

@PostMapping("/confirm-profile")
public String confirmProfile(@Valid @ModelAttribute("selectionRequest") ProfileSelectionRequest request,
                            BindingResult bindingResult,
                            HttpSession session,
                            Model model) {

    String mobile = (String) session.getAttribute("SESSION_MOBILE");

    if (bindingResult.hasErrors()) {
        // Reload profiles
        CustomerRegistrationResponse customerData =
            (CustomerRegistrationResponse) session.getAttribute("CUSTOMER_REGISTRATION_DATA");
        List<CustomerDetails> profiles = customerData.getResponse()
            .getBody().getCustDetails();
        model.addAttribute("profiles", profiles);
        model.addAttribute("mobile", maskMobile(mobile));
        return "select-profile";
    }

    // Find selected profile by CIF ID
    CustomerRegistrationResponse customerData =
        (CustomerRegistrationResponse) session.getAttribute("CUSTOMER_REGISTRATION_DATA");

    CustomerDetails selectedProfile = customerData.getResponse()
        .getBody().getCustDetails().stream()
        .filter(profile -> profile.getCifID().equals(request.getSelectedCifId()))
        .findFirst()
        .orElse(null);

    if (selectedProfile == null) {
        bindingResult.rejectValue("selectedCifId", "error.selectedCifId",
            "Invalid profile selection");
        return "select-profile";
    }

    // Store selected profile in session
    session.setAttribute("SELECTED_PROFILE", selectedProfile);
    session.setAttribute("SELECTED_CIF_ID", selectedProfile.getCifID());
    session.setAttribute("PROFILE_SELECTION_REQUIRED", false);

    userJourneyLogger.logTransactionInitiated(mobile,
        "Profile Selected: " + selectedProfile.getCifID());

    // OTP already validated - go directly to dashboard
    return "redirect:/dashboard";
}
```

**2.3 Create select-profile.html (Thymeleaf template)**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Select Profile - SIB Portal</title>
    <link rel="stylesheet" th:href="@{/css/style.css}">
</head>
<body>
    <div class="container">
        <div class="card">
            <h2>Select Your Profile</h2>

            <p class="info-message">
                Multiple profiles found for mobile number <strong th:text="${mobile}"></strong>.
                Please select the profile you want to use.
            </p>

            <form th:action="@{/confirm-profile}" method="post" th:object="${selectionRequest}">
                <div class="profile-list">
                    <div th:each="profile : ${profiles}" class="profile-card">
                        <input type="radio"
                               th:id="${'profile_' + profile.cifID}"
                               th:field="*{selectedCifId}"
                               th:value="${profile.cifID}"
                               required>
                        <label th:for="${'profile_' + profile.cifID}">
                            <div class="profile-info">
                                <h3 th:text="${profile.custName}">Customer Name</h3>
                                <p><strong>CIF ID:</strong> <span th:text="${profile.cifID}">CIF</span></p>
                                <p><strong>Date of Birth:</strong> <span th:text="${profile.custDOB}">DOB</span></p>
                                <p><strong>Constitution Code:</strong> <span th:text="${profile.constCode}">Code</span></p>
                                <p><strong>Accounts:</strong> <span th:text="${profile.operativeAcctCnt}">0</span></p>

                                <!-- Show account summary -->
                                <div class="account-summary" th:if="${profile.accountDetails != null && !profile.accountDetails.isEmpty()}">
                                    <strong>Accounts:</strong>
                                    <ul>
                                        <li th:each="account : ${profile.accountDetails}">
                                            <span th:text="${account.acctName}"></span> -
                                            <span th:text="${account.foracid}"></span>
                                            (<span th:text="${account.schemDesc}"></span>)
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </label>
                    </div>
                </div>

                <div class="error-message" th:if="${#fields.hasErrors('selectedCifId')}">
                    <span th:errors="*{selectedCifId}"></span>
                </div>

                <button type="submit" class="btn btn-primary">Continue</button>
            </form>

            <a th:href="@{/login}" class="link-secondary">Back to Login</a>
        </div>
    </div>
</body>
</html>
```

### Phase 3: Session Management

**3.1 Session Attributes**
```java
// After API call (multiple profiles)
session.setAttribute("CUSTOMER_REGISTRATION_DATA", fullResponse);
session.setAttribute("CUSTOMER_COUNT", 3);
session.setAttribute("PROFILE_SELECTION_REQUIRED", true);

// After profile selection
session.setAttribute("SELECTED_PROFILE", selectedProfile);
session.setAttribute("PROFILE_SELECTION_REQUIRED", false);
session.setAttribute("SELECTED_CIF_ID", "A55835680");
session.setAttribute("SELECTED_CUSTOMER_NAME", "GOPIKRISHNAN T M");

// After OTP validation
// Profile remains in session for all subsequent operations
```

**3.2 Access Selected Profile**
```java
// In any controller/service
CustomerDetails selectedProfile =
    (CustomerDetails) session.getAttribute("SELECTED_PROFILE");

if (selectedProfile != null) {
    String cifId = selectedProfile.getCifID();
    String name = selectedProfile.getCustName();
    String dob = selectedProfile.getCustDOB();
    List<AccountDetails> accounts = selectedProfile.getAccountDetails();
}
```

### Phase 4: Dashboard Updates

**4.1 Update dashboard.html**
```html
<!-- Show selected profile info -->
<div class="profile-header">
    <h2>Welcome, <span th:text="${customerName}">User</span></h2>
    <p>CIF ID: <span th:text="${cifId}">A12345</span></p>
    <p>Accounts: <span th:text="${accountCount}">0</span></p>
</div>

<!-- Switch profile option -->
<a th:href="@{/switch-profile}" class="link">Switch Profile</a>
```

**4.2 Update AuthController dashboard method**
```java
@GetMapping("/dashboard")
public String dashboard(HttpSession session, Model model) {
    String mobile = SecurityContextHolder.getContext().getAuthentication().getName();
    userJourneyLogger.logPageVisit(mobile, "Dashboard");

    // Get selected profile
    CustomerDetails selectedProfile =
        (CustomerDetails) session.getAttribute("SELECTED_PROFILE");

    if (selectedProfile != null) {
        model.addAttribute("customerName", selectedProfile.getCustName());
        model.addAttribute("cifId", selectedProfile.getCifID());
        model.addAttribute("accountCount", selectedProfile.getOperativeAcctCnt());
        model.addAttribute("dob", selectedProfile.getCustDOB());
    } else {
        // Fallback to old method
        CustomerProfileResponse profile = customerIntegrationService.getCustomerProfile(mobile);
        model.addAttribute("customerName", profile.getFullName());
    }

    return "dashboard";
}
```

### Phase 5: Profile Switching

**5.1 Add switch profile endpoint**
```java
@GetMapping("/switch-profile")
public String switchProfile(HttpSession session) {
    // Clear selected profile
    session.removeAttribute("SELECTED_PROFILE");
    session.removeAttribute("SELECTED_CIF_ID");

    // Set flag to show profile selection
    session.setAttribute("PROFILE_SELECTION_REQUIRED", true);

    return "redirect:/select-profile";
}
```

## Security Considerations

### 1. OTP Before Profile Selection ✅
```java
// OTP is ALWAYS sent after API validates phone number
// Profile selection happens AFTER OTP validation
// This ensures:
// 1. Phone number ownership is verified first
// 2. User is authenticated before seeing customer data
// 3. Multiple profiles are only shown to authenticated users

// After captcha validation
String otp = otpService.generateOtp(mobileNumber); // ← ALWAYS send OTP

// After OTP validation
if (customerCount > 1) {
    return "redirect:/select-profile"; // ← Show profiles AFTER auth
}
```

### 2. Session Validation
```java
// Before showing profile selection page
Boolean selectionRequired = (Boolean) session.getAttribute("PROFILE_SELECTION_REQUIRED");
if (selectionRequired == null || !selectionRequired) {
    return "redirect:/login"; // Force re-login
}

// Also check if user is authenticated
String mobile = SecurityContextHolder.getContext().getAuthentication().getName();
if (mobile == null || "anonymousUser".equals(mobile)) {
    return "redirect:/login"; // Not authenticated
}
```

### 3. CIF ID Validation
```java
// When user submits profile selection
// Ensure selected CIF ID exists in their session data
CustomerDetails selectedProfile = customerData.getResponse()
    .getBody().getCustDetails().stream()
    .filter(profile -> profile.getCifID().equals(request.getSelectedCifId()))
    .findFirst()
    .orElse(null);

if (selectedProfile == null) {
    // Invalid CIF ID - potential tampering
    userJourneyLogger.logSecurityEvent(mobile, "Invalid CIF selection attempt");
    return "error";
}
```

## User Experience Flow

### Scenario 1: Single Profile (Current Flow)
```
Login Page (mobile: 9496807441, captcha: ABC123)
  ↓
API Call → Validates phone, finds 1 customer
  ↓
OTP Page (OTP sent to XXXXXXX441)
  ↓
User enters OTP and validates
  ↓
Dashboard (Welcome, GOPIKRISHNAN T M)
```

### Scenario 2: Multiple Profiles (New Flow)
```
Login Page (mobile: 9496807441, captcha: ABC123)
  ↓
API Call → Validates phone, finds 3 customers
  ↓
OTP Page (OTP sent to XXXXXXX441) ← Same as single profile
  ↓
User enters OTP and validates ✅ Authenticated
  ↓
Profile Selection Page (NOW show profiles)
  [•] GOPIKRISHNAN T M (CIF: A55835680) - Parent
      DOB: 14-08-1992, Accounts: 2
  [ ] GOPIKRISHNAN JUNIOR (CIF: A55835681) - Child
      DOB: 01-05-2015, Accounts: 1
  [ ] GOPIKRISHNA T M (CIF: A55835682) - Spouse
      DOB: 20-03-1995, Accounts: 1
  ↓
User selects Parent profile
  ↓
Dashboard (Welcome, GOPIKRISHNAN T M - CIF: A55835680)
```

### Scenario 3: Profile Switching (Requires Re-authentication)
```
Dashboard (Current: GOPIKRISHNAN T M)
  ↓
Click "Switch Profile"
  ↓
Logout and redirect to login
  ↓
Login again with OTP
  ↓
Profile Selection Page
  [ ] GOPIKRISHNAN T M (CIF: A55835680) - Parent
  [•] GOPIKRISHNAN JUNIOR (CIF: A55835681) - Child
  [ ] GOPIKRISHNA T M (CIF: A55835682) - Spouse
  ↓
User selects Child profile
  ↓
Dashboard (Welcome, GOPIKRISHNAN JUNIOR - CIF: A55835681)
```

**Note:** Profile switching requires re-authentication (logout + login with OTP) for security.

## Database Schema (Optional)

If you want to persist profile selections:

```sql
CREATE TABLE user_profile_selections (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mobile_number VARCHAR(10) NOT NULL,
    selected_cif_id VARCHAR(20) NOT NULL,
    selection_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_date TIMESTAMP,
    INDEX idx_mobile (mobile_number)
);

-- Remember last used profile per mobile number
SELECT selected_cif_id
FROM user_profile_selections
WHERE mobile_number = '9496807441'
ORDER BY last_used_date DESC
LIMIT 1;
```

## Testing Scenarios

### Test Case 1: Single Profile
- **Input:** Mobile with 1 customer
- **Expected:** Direct to OTP page
- **Verify:** No profile selection shown

### Test Case 2: Two Profiles
- **Input:** Mobile with 2 customers
- **Expected:** Profile selection page shown
- **Verify:** Both profiles displayed correctly

### Test Case 3: Invalid Profile Selection
- **Input:** Tampered CIF ID in form submission
- **Expected:** Error message
- **Verify:** Security event logged

### Test Case 4: Profile Switching
- **Input:** Switch from Profile A to Profile B
- **Expected:** New OTP required, dashboard updates
- **Verify:** All subsequent operations use Profile B

## CSS Styling (select-profile.html)

```css
.profile-list {
    display: flex;
    flex-direction: column;
    gap: 15px;
    margin: 20px 0;
}

.profile-card {
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    padding: 15px;
    transition: all 0.3s ease;
}

.profile-card input[type="radio"]:checked + label {
    border-color: #007bff;
    background-color: #f0f8ff;
}

.profile-card:hover {
    border-color: #007bff;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.profile-info h3 {
    margin: 0 0 10px 0;
    color: #333;
}

.profile-info p {
    margin: 5px 0;
    color: #666;
}

.account-summary {
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px solid #e0e0e0;
}

.account-summary ul {
    list-style: none;
    padding-left: 0;
}

.account-summary li {
    padding: 5px 0;
    font-size: 0.9em;
}
```

## Summary

**Key Changes:**
1. ✅ API call validates phone number and fetches customer data
2. ✅ OTP sent IMMEDIATELY after API call (regardless of profile count)
3. ✅ After OTP validation, check for multiple profiles
4. ✅ Show profile selection page AFTER authentication
5. ✅ Store selected profile in session
6. ✅ Use selected profile for all operations
7. ✅ Profile switching requires re-authentication

**Correct Flow:**
```
Login → Captcha → API (validate phone) → OTP → Validate OTP →
    → Check profiles → [Single: Dashboard] or [Multiple: Select Profile → Dashboard]
```

**Session Data:**
- `CUSTOMER_REGISTRATION_DATA`: Full API response
- `CUSTOMER_COUNT`: Number of profiles
- `PROFILE_SELECTION_REQUIRED`: Boolean flag (set after OTP validation)
- `SELECTED_PROFILE`: Currently active profile
- `SELECTED_CIF_ID`: Quick access to CIF

**Security:**
- ✅ OTP required BEFORE showing profiles (phone ownership verified)
- ✅ Profiles shown only to authenticated users
- ✅ CIF ID validation on selection
- ✅ Session validation on all protected pages
- ✅ Profile switching requires re-authentication
- ✅ Security event logging for suspicious activity

This approach ensures:
- Phone number ownership is verified first (OTP)
- Customer data is protected (shown only after auth)
- Users can manage multiple profiles under same mobile
- Clear selection process with profile details
- Secure switching between profiles
- All operations tied to selected profile
