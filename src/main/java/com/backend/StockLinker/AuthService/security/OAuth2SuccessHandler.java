package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.service.AuthFlowService;
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

    private final UserRepository userRepository;
    private final AuthFlowService authFlowService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // Extract Google data
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");
        String googleId = oauthUser.getAttribute("sub");

        log.info("OAuth2 login success for email: {}", email);

        // Find or create user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setName(name);
                    u.setAvatarUrl(picture);
                    u.setProvider("GOOGLE");
                    u.setUniqueId(googleId);
                    return userRepository.save(u);
                });

        // Get device ID from cookie or generate new
        String deviceId = getDeviceIdFromCookie(request);
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = generateDeviceId(request);
        }

        // Process login through auth flow
        AuthResponse authResponse = authFlowService.login(
                user,
                "GOOGLE",
                deviceId,
                request,
                response
        );

        // Build redirect URL with all parameters
        String redirectUrl = String.format(
                "http://localhost:5173/oauth-success?token=%s&refreshToken=%s&userId=%s&isNewUser=%s&needsRoleSelection=%s&hasBusinessRole=%s",
                authResponse.getAccessToken(),
                authResponse.getRefreshToken(),
                authResponse.getUserId(),
                authResponse.isNewUser(),
                authResponse.isNeedsRoleSelection(),
                authResponse.isHasBusinessRole()
        );

        log.info("Redirecting to: {}", redirectUrl);
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