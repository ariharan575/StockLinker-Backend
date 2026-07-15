package com.backend.StockLinker.AuthService.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
public class DeviceFingerprintFilter extends OncePerRequestFilter {

    public static final String DEVICE_COOKIE_NAME = "deviceId";
    public static final String DEVICE_HEADER_NAME = "X-Device-Id";

    private final boolean isProduction;

    public DeviceFingerprintFilter(Environment env) {
        this.isProduction = env.acceptsProfiles(Profiles.of("prod", "production"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        log.debug("DeviceFingerprintFilter processing request: {}", path);

        log.info("Device Filter executed");
        log.info("URI = {}", request.getRequestURI());

        String deviceId = null;

        // 1. Try to get from cookie first
        deviceId = getValidDeviceFromCookie(request);

        // 2. Header
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = request.getHeader(DEVICE_HEADER_NAME);
        }

        // 3. Generate
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
        }

        // Set cookie if device was generated or from header
        if (getValidDeviceFromCookie(request) == null) {
            setSecureCookie(response, deviceId);
        }

        // ALWAYS set as request attribute - Single Source of Truth
        request.setAttribute(DEVICE_COOKIE_NAME, deviceId);
        log.debug("Device ID set in request attribute: {}", deviceId);

        filterChain.doFilter(request, response);
    }

    private String getValidDeviceFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (DEVICE_COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (isValidUUID(value)) {
                    log.debug("Found valid device ID in cookie: {}", value);
                    return value;
                } else {
                    log.warn("Invalid device ID format in cookie: {}", value);
                }
            }
        }
        return null;
    }

    private boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.isBlank() || uuid.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void setSecureCookie(HttpServletResponse response, String deviceId) {
        ResponseCookie cookie = ResponseCookie.from(DEVICE_COOKIE_NAME, deviceId)
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(Duration.ofDays(365))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.debug("Set device cookie: {}", deviceId);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Never skip - we need deviceId for ALL requests
        return false;
    }
}