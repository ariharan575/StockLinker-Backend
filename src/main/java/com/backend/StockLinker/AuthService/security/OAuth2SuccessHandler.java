package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend.url}/oauth-success")
    private String oauthSuccessRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        try {
            log.debug("OAuth2 success handler called");

            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            if (oauthUser == null) {
                log.error("OAuth2 user is null");
                throw new BaseException(ErrorCode.OAUTH_FAILED, "OAuth2 user information is missing");
            }

            // 🔥  Get deviceId from request attribute FIRST
            String deviceId = (String) request.getAttribute("deviceId");

            log.info("Saving deviceId into request = {}", deviceId);

            // If still null, generate a new one
            if (deviceId == null || deviceId.isBlank()) {
                log.debug("Generated fallback device ID: {}", deviceId);
                throw new IllegalStateException("DeviceId missing");
            }

            log.info("Processing OAuth2 login with deviceId: {}", deviceId);

            // Process the Google login
            AuthResponse authResponse = authService.googleLogin(oauthUser, deviceId, request, response);

            String targetUrl = UriComponentsBuilder.fromUriString(oauthSuccessRedirectUrl)
                    .queryParam("status", authResponse.getAccountStatus())
                    .build().toUriString();

            log.info("OAuth2 login completed. Redirecting to: {}", targetUrl);

            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 authentication failed: {}", e.getMessage(), e);

            // Redirect to error page
            String errorUrl = UriComponentsBuilder.fromUriString(oauthSuccessRedirectUrl)
                    .queryParam("error", "Authentication failed")
                    .queryParam("errorMessage", e.getMessage())
                    .build().toUriString();

            response.sendRedirect(errorUrl);
        }
    }
}