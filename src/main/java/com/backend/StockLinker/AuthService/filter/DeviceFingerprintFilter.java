package com.backend.StockLinker.AuthService.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@Component
public class DeviceFingerprintFilter extends OncePerRequestFilter {

    private static final String DEVICE_COOKIE = "deviceId";
    private final boolean isProduction;

    public DeviceFingerprintFilter(Environment env) {
        this.isProduction = Arrays.asList(env.getActiveProfiles()).contains("prod");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String deviceId = getDeviceFromCookie(request);

        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
            setSecureCookie(response, deviceId);
        }

        request.setAttribute(DEVICE_COOKIE, deviceId);

        filterChain.doFilter(request, response);
    }

    private String getDeviceFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (DEVICE_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setSecureCookie(HttpServletResponse response, String deviceId) {
        ResponseCookie cookie = ResponseCookie.from(DEVICE_COOKIE, deviceId)
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(Duration.ofDays(365))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}