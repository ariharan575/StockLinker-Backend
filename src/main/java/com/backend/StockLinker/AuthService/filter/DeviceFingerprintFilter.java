package com.backend.StockLinker.AuthService.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class DeviceFingerprintFilter extends OncePerRequestFilter {

    private static final String DEVICE_COOKIE = "deviceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String deviceId = getDeviceFromCookie(request);

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            setCookie(response, deviceId);
        }

        request.setAttribute("deviceId", deviceId);

        filterChain.doFilter(request, response);
    }

    private String getDeviceFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie c : request.getCookies()) {
            if (DEVICE_COOKIE.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private void setCookie(HttpServletResponse response, String deviceId) {
        Cookie cookie = new Cookie(DEVICE_COOKIE, deviceId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 365);
        response.addCookie(cookie);
    }
}
