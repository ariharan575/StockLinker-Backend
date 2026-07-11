package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
import com.backend.StockLinker.AuthService.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    // Inject AuthService directly instead of UserRepository and AuthFlowService
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");
        String googleId = oauthUser.getAttribute("sub");

        log.info("OAuth2 login success for email: {}", email);

        String deviceId = getDeviceIdFromCookie(request);
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = generateDeviceId(request);
        }

        // Delegate entire creation and flow processing to the unified AuthService
        AuthResponse authResponse = authService.loginWithGoogle(
                email,
                name,
                picture,
                googleId,
                deviceId,
                request,
                response
        );

        // Build redirect URL matching strict enterprise DTO contract
        String redirectUrl = String.format(
                "http://localhost:5173/oauth-success?token=%s&refreshToken=%s&userId=%s&role=%s&accountStatus=%s&nextStep=%s"
        );

        log.info("OAuth processing complete. Redirecting frontend to correct pipeline stage.");
        response.sendRedirect(redirectUrl);
    }

    private String getDeviceIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("deviceId".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String generateDeviceId(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        return UUID.nameUUIDFromBytes((userAgent + ip).getBytes()).toString();
    }
}