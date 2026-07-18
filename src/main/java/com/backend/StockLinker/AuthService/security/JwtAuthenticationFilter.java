package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.enums.AuditAction;
import com.backend.StockLinker.AuthService.enums.ResourceType;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.dto.request.AuditLogRequest;
import com.backend.StockLinker.AuthService.model.UserDevice;
import com.backend.StockLinker.AuthService.repository.UserDeviceRepository;
import com.backend.StockLinker.AuthService.service.AuditService;
import com.backend.StockLinker.AuthService.service.IpAddressService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuditService auditService;
    private final UserDeviceRepository userDeviceRepository;
    private final IpAddressService idAddressService;

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (token != null && jwtService.validateToken(token)) {
                Claims claims = jwtService.extractAllClaims(token);
                String tokenType = claims.get("type", String.class);

                if ("access".equals(tokenType)) {
                    String userId = claims.getSubject();
                    String tokenDeviceId = claims.get("deviceId", String.class);
                    // Strictly read from attribute configured by DeviceFingerprintFilter
                    String requestDeviceId = (String) request.getAttribute("deviceId");

                    if (tokenDeviceId != null && !tokenDeviceId.equals(requestDeviceId)) {
                        log.warn("Device mismatch detected for user: {}", userId);
                        auditService.log(AuditLogRequest.builder()
                                .userId(userId)
                                .action(AuditAction.DEVICE_MISMATCH)
                                .resourceType(ResourceType.DEVICE)
                                .resourceId(tokenDeviceId)
                                .ipAddress(idAddressService.getClientIp(request))
                                .userAgent(request.getHeader("User-Agent"))
                                .deviceId(requestDeviceId)
                                .requestUri(request.getRequestURI())
                                .status(AuditLog.Status.FAILURE)
                                .failureReason("Access token deviceId does not match request deviceId")
                                .build());

                        SecurityContextHolder.clearContext();
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        String errorJson = String.format(
                                "{\"status\": 401, \"error\": \"UNAUTHORIZED\", \"message\": \"Unauthorized - Device mismatch\", \"path\": \"%s\"}",
                                request.getRequestURI()
                        );
                        response.getWriter().write(errorJson);
                        return;
                    }

                    // NEW: DB Device Active Verification for immediate logout-all protection
                    if (userId != null && tokenDeviceId != null) {

                        UserDevice userDevice = userDeviceRepository.findByUserIdAndDeviceId(userId, tokenDeviceId).orElse(null);

                        if (userDevice == null || !userDevice.isActive()) {
                            log.warn("Active session missing or device deactivated for user: {}", userId);
                            auditService.log(AuditLogRequest.builder()
                                    .userId(userId)
                                    .action(AuditAction.INVALID_TOKEN)
                                    .resourceType(ResourceType.DEVICE)
                                    .resourceId(tokenDeviceId)
                                    .ipAddress(idAddressService.getClientIp(request))
                                    .userAgent(request.getHeader("User-Agent"))
                                    .deviceId(requestDeviceId)
                                    .requestUri(request.getRequestURI())
                                    .status(AuditLog.Status.FAILURE)
                                    .failureReason("Device session is completely inactive or disabled")
                                    .build());

                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized - Device session inactive\"}");
                            return; // Halt chain and enforce failure immediately
                        }
                    }

                    // Extract Roles & Permissions statelessly
                    List<String> authorityStrings = claims.get("authorities", List.class);
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (authorityStrings != null) {
                        for (String auth : authorityStrings) {
                            authorities.add(new SimpleGrantedAuthority(auth));
                        }
                    }

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Authenticated user statelessly: {}", userId);
                }
            }
        } catch (ExpiredJwtException e) {
            String userId = (e.getClaims() != null) ? e.getClaims().getSubject() : "UNKNOWN";
            auditService.log(AuditLogRequest.builder()
                    .userId(userId)
                    .action(AuditAction.EXPIRED_TOKEN)
                    .resourceType(ResourceType.TOKEN)
                    .ipAddress(idAddressService.getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .deviceId((String) request.getAttribute("deviceId"))
                    .requestUri(request.getRequestURI())
                    .status(AuditLog.Status.FAILURE)
                    .failureReason("Access token expired")
                    .build());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT authentication error: {}", e.getMessage());
            auditService.log(AuditLogRequest.builder()
                    .userId("UNKNOWN")
                    .action(AuditAction.INVALID_TOKEN)
                    .resourceType(ResourceType.TOKEN)
                    .ipAddress(idAddressService.getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .deviceId((String) request.getAttribute("deviceId"))
                    .requestUri(request.getRequestURI())
                    .status(AuditLog.Status.FAILURE)
                    .failureReason("Malformed or invalid JWT signature")
                    .build());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }



    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/phone/login") ||
                path.equals("/api/auth/guest/login") ||
                path.equals("/api/auth/refresh") ||
                path.startsWith("/oauth2/") ||
                path.equals("/api/auth/oauth-success");
    }
}