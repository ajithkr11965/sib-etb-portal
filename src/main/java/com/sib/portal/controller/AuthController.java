package com.sib.portal.controller;

import com.sib.portal.dto.LoginRequest;
import com.sib.portal.dto.OtpRequest;
import com.sib.portal.dto.CardRequestPayload;
import com.sib.portal.dto.CustomerProfileResponse;
import com.sib.portal.dto.CustomerRegistrationResponse;
import com.sib.portal.service.CaptchaService;
import com.sib.portal.service.CardIntegrationService;
import com.sib.portal.service.CustomerIntegrationService;
import com.sib.portal.service.OtpService;
import com.sib.portal.service.UserJourneyLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final OtpService otpService;
    private final CaptchaService captchaService;
    private final UserJourneyLogger userJourneyLogger;
    private final CardIntegrationService cardIntegrationService;
    private final CustomerIntegrationService customerIntegrationService;

    public AuthController(OtpService otpService, CaptchaService captchaService, UserJourneyLogger userJourneyLogger,
            CardIntegrationService cardIntegrationService, CustomerIntegrationService customerIntegrationService) {
        this.otpService = otpService;
        this.captchaService = captchaService;
        this.userJourneyLogger = userJourneyLogger;
        this.cardIntegrationService = cardIntegrationService;
        this.customerIntegrationService = customerIntegrationService;
    }

    @GetMapping("/")
    public String landingPage(Model model, HttpSession session) {
        // Simple landing page, no auth check needed to view (but login button will
        // redirect if auth'd)
        return "landing";
    }

    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session) {
        // Redirect if already logged in
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }

        // Always generate a new captcha on page load
        String captcha = captchaService.generateCaptcha();
        session.setAttribute("SESSION_CAPTCHA", captcha);

        LoginRequest loginRequest = new LoginRequest();
        model.addAttribute("loginRequest", loginRequest);
        model.addAttribute("captchaText", captcha);

        userJourneyLogger.logPageVisit("Anonymous", "Login Page");

        return "login";
    }

    @PostMapping("/continue-to-otp")
    public String continueToOtp(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {

        String sessionCaptcha = (String) session.getAttribute("SESSION_CAPTCHA");

        userJourneyLogger.logLoginAttempt(loginRequest.getMobileNumber());

        if (bindingResult.hasErrors()) {
            model.addAttribute("captchaText", sessionCaptcha); // maintain captcha
            userJourneyLogger.logLoginFailure(loginRequest.getMobileNumber(), "Validation Error");
            return "login";
        }

        if (!loginRequest.isAgreeToTerms()) {
            bindingResult.rejectValue("agreeToTerms", "error.agreeToTerms", "You must agree to the Terms of Service");
            model.addAttribute("captchaText", sessionCaptcha);
            userJourneyLogger.logLoginFailure(loginRequest.getMobileNumber(), "Terms not accepted");
            return "login";
        }

        if (sessionCaptcha == null || !sessionCaptcha.equalsIgnoreCase(loginRequest.getCaptcha())) {
            bindingResult.rejectValue("captcha", "error.captcha", "Invalid Captcha");
            model.addAttribute("captchaText", sessionCaptcha);
            userJourneyLogger.logLoginFailure(loginRequest.getMobileNumber(), "Invalid Captcha");
            return "login";
        }

        // Fetch customer details from registration API
        try {
            logger.info("Fetching customer registration details for mobile: XXXXXXX{}",
                    loginRequest.getMobileNumber().substring(loginRequest.getMobileNumber().length() - 4));

            CustomerRegistrationResponse registrationResponse =
                    customerIntegrationService.getCustomerRegistrationDetails(loginRequest.getMobileNumber());

            // Store customer details in session for later use
            if (registrationResponse != null && registrationResponse.getResponse() != null
                    && registrationResponse.getResponse().getBody() != null) {
                session.setAttribute("CUSTOMER_REGISTRATION_DATA", registrationResponse);
                logger.info("Customer registration details fetched and stored in session");
            } else {
                logger.warn("Empty customer registration response received");
            }

        } catch (Exception e) {
            logger.error("Error fetching customer registration details: {}", e.getMessage(), e);
            bindingResult.rejectValue("mobileNumber", "error.mobileNumber",
                    "Unable to verify customer details. Please try again later.");
            model.addAttribute("captchaText", sessionCaptcha);
            userJourneyLogger.logLoginFailure(loginRequest.getMobileNumber(), "Customer API Error");
            return "login";
        }

        // Generate OTP
        String otp = otpService.generateOtp(loginRequest.getMobileNumber());
        session.setAttribute("SESSION_OTP", otp);
        session.setAttribute("SESSION_MOBILE", loginRequest.getMobileNumber());
        session.setAttribute("SESSION_OTP_TIME", LocalDateTime.now());
        session.setAttribute("SESSION_OTP_ATTEMPTS", 0);

        return "redirect:/otp";
    }

    @GetMapping("/otp")
    public String otpPage(Model model, HttpSession session) {
        String mobile = (String) session.getAttribute("SESSION_MOBILE");
        if (mobile == null) {
            return "redirect:/";
        }

        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setMobileNumber(mobile);
        model.addAttribute("otpRequest", otpRequest);
        model.addAttribute("mobile", maskMobile(mobile));

        userJourneyLogger.logPageVisit(mobile, "OTP Page");

        return "otp";
    }

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

        if (bindingResult.hasErrors()) {
            model.addAttribute("mobile", maskMobile(mobile));
            return "otp";
        }

        if (attempts != null && attempts >= 3) {
            bindingResult.rejectValue("otp", "error.otp", "Max attempts reached. Please login again.");
            model.addAttribute("mobile", maskMobile(mobile));
            userJourneyLogger.logLoginFailure(mobile, "Max OTP attempts reached");
            return "otp";
        }

        session.setAttribute("SESSION_OTP_ATTEMPTS", (attempts == null ? 0 : attempts) + 1);

        if (!otpService.validateOtp(sessionOtp, otpRequest.getOtp(), generatedTime)) {
            bindingResult.rejectValue("otp", "error.otp", "Invalid or Expired OTP");
            model.addAttribute("mobile", maskMobile(mobile));
            userJourneyLogger.logLoginFailure(mobile, "Invalid OTP");
            return "otp";
        }

        // Success - Set Spring Security Context
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                mobile, otpRequest.getOtp(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        userJourneyLogger.logLoginSuccess(mobile);
        return "redirect:/dashboard";
    }

    @PostMapping("/resend-otp")
    @ResponseBody
    public String resendOtp(HttpSession session) {
        String mobile = (String) session.getAttribute("SESSION_MOBILE");
        if (mobile != null) {
            String otp = otpService.generateOtp(mobile);
            session.setAttribute("SESSION_OTP", otp);
            session.setAttribute("SESSION_OTP_TIME", LocalDateTime.now());
            session.setAttribute("SESSION_OTP_ATTEMPTS", 0);
            userJourneyLogger.logTransactionInitiated(mobile, "Resend OTP");
            return "OTP Resent";
        }
        return "Error";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // Security checks handled by SecurityConfig for /dashboard
        String mobile = SecurityContextHolder.getContext().getAuthentication().getName();
        userJourneyLogger.logPageVisit(mobile, "Dashboard");

        // Fetch Customer Details
        CustomerProfileResponse profile = customerIntegrationService.getCustomerProfile(mobile);
        model.addAttribute("customerName", profile.getFullName());
        model.addAttribute("customerEmail", profile.getEmail());

        return "dashboard";
    }

    @GetMapping("/debit-services")
    public String debitServices(HttpSession session, Model model) {
        String mobile = SecurityContextHolder.getContext().getAuthentication().getName();
        userJourneyLogger.logPageVisit(mobile, "Debit Services");
        return "debit-services";
    }

    @GetMapping("/card-request")
    public String cardRequest(HttpSession session, Model model) {
        String mobile = SecurityContextHolder.getContext().getAuthentication().getName();
        userJourneyLogger.logPageVisit(mobile, "Card Request");
        return "card-request";
    }

    @GetMapping("/address-selection")
    public String addressSelection(HttpSession session, Model model) {
        String mobile = SecurityContextHolder.getContext().getAuthentication().getName();
        userJourneyLogger.logPageVisit(mobile, "Address Selection");
        return "address-selection";
    }

    @GetMapping("/card-request-review")
    public String cardRequestReview(HttpSession session, Model model) {
        // Protected by SecurityConfig
        return "card-request-review";
    }

    @GetMapping("/card-request-otp")
    public String cardRequestOtp(HttpSession session, Model model) {
        String mobile = SecurityContextHolder.getContext().getAuthentication().getName();

        // Generate OTP for transaction (Simulation)
        if (mobile != null) {
            String otp = otpService.generateOtp(mobile);
            session.setAttribute("TX_OTP", otp); // Keeping TX OTP in session for simplicity
            model.addAttribute("mobileLast4", mobile.length() > 4 ? mobile.substring(mobile.length() - 4) : mobile);
        }

        model.addAttribute("otpRequest", new OtpRequest());
        return "card-request-otp";
    }

    @PostMapping("/submit-card-request")
    public String submitCardRequest(@ModelAttribute("otpRequest") OtpRequest otpRequest, HttpSession session,
            Model model) {

        String inputOtp = otpRequest.getOtp();
        String sessionTxOtp = (String) session.getAttribute("TX_OTP");
        String mobile = SecurityContextHolder.getContext().getAuthentication().getName();

        if ("111111".equals(inputOtp) || (sessionTxOtp != null && sessionTxOtp.equals(inputOtp))) {
            // Create Mock Payload
            CardRequestPayload payload = new CardRequestPayload();
            payload.setCustomerId("CUST_001");
            payload.setAccountId("ACC_001");
            payload.setCardVariant("PREMIUM");
            payload.setDeliveryAddressType("PERMANENT");

            // Call Integration Service
            cardIntegrationService.submitCardRequest(payload);

            // Log Success
            userJourneyLogger.logTransactionSuccess(mobile, "Debit Card Request");

            return "redirect:/card-request-success";
        } else {
            model.addAttribute("error", "Invalid OTP. Try 111111 for demo.");
            model.addAttribute("mobileLast4",
                    mobile != null && mobile.length() > 4 ? mobile.substring(mobile.length() - 4) : mobile);
            userJourneyLogger.logTransactionFailure(mobile, "Debit Card Request", "Invalid OTP");
            return "card-request-otp";
        }
    }

    @GetMapping("/card-request-success")
    public String cardRequestSuccess(HttpSession session, Model model) {
        return "card-request-success";
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4)
            return mobile;
        return "XXXXXXX" + mobile.substring(mobile.length() - 3);
    }
}
