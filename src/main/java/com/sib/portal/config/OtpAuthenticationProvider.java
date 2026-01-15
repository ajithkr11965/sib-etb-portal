package com.sib.portal.config;

import com.sib.portal.service.AuthIntegrationService;
import com.sib.portal.service.UserJourneyLogger;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class OtpAuthenticationProvider implements AuthenticationProvider {

    private final AuthIntegrationService authIntegrationService;
    private final UserJourneyLogger userJourneyLogger;

    public OtpAuthenticationProvider(AuthIntegrationService authIntegrationService,
            UserJourneyLogger userJourneyLogger) {
        this.authIntegrationService = authIntegrationService;
        this.userJourneyLogger = userJourneyLogger;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String mobile = authentication.getName();
        String otp = authentication.getCredentials().toString();

        // Validate OTP via Integration Service
        // NOTE: In a real flow, the 'sessionOtp' check might happen here if stateless,
        // or we pass session info. But since we have stateful HttpSession logic in
        // AuthController,
        // we might rely on AuthController to validate and then called this to
        // "finalize" login.
        // However, the cleanest Spring Security way is to put logic here.
        // But AuthController has the 'sessionOtp' (generated one).

        // For this implementation: We assume AuthController calls a custom method or we
        // pass the session OTP as part of 'details' or just simple validation if
        // API-based.
        // Given existing structure: AuthController validates OTP. If valid, we just
        // need to creates a token.
        // But if we want provider to do it, we need the logic.

        // Let's implement full validation here if we can, BUT session OTP is in
        // HttpSession.
        // Accessing HttpSession here is possible but messy.
        // BETTER APPROACH for this phase:
        // AuthController validates logic (as it has session OTP).
        // Then it calls this provider (or declares success) to creating the
        // Authentication object manually.
        // BUT to use Provider properly, `authenticate` should do the check.
        // Simplification for migration:
        // We will allow ANY credential validation here assuming the caller (Controller)
        // already checked it?
        // NO, that's insecure.
        //
        // CORRECT APPROACH:
        // The Controller checks the session OTP. If it matches, it calls a custom
        // method to "login".
        // Or we pass the session OTP as the "password" to this provider? No, user
        // enters input OTP.

        // Compromise for this Refactor:
        // Controller checks OTP against Session.
        // If valid, Controller creates a UsernamePasswordAuthenticationToken with
        // authorities and sets it in SecurityContextHolder.
        // This bypasses the Provider lookup but is standard for custom
        // non-UserDetailsService flows relying on Session OTPs.

        // HOWEVER, if we want to use the Provider, we can stick the validate logic here
        // if `authIntegrationService` supports it.
        // `authIntegrationService.validateOtp` is API based.

        if (authIntegrationService.validateOtp(mobile, otp)) {
            userJourneyLogger.logLoginSuccess(mobile);
            return new UsernamePasswordAuthenticationToken(mobile, otp,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        } else {
            userJourneyLogger.logLoginFailure(mobile, "Invalid OTP via Provider");
            throw new BadCredentialsException("Invalid OTP");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
