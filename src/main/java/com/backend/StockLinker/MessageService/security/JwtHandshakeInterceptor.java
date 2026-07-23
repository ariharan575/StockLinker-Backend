package com.backend.StockLinker.MessageService.security;

import com.backend.StockLinker.AuthService.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * Authenticates the SockJS/STOMP handshake using the accessToken HttpOnly cookie —
 * the exact same cookie the REST JwtAuthenticationFilter reads. No token is ever
 * exposed to frontend JS; the browser attaches the cookie to the handshake request
 * automatically (same-origin, or CORS with credentials).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String token = extractToken(httpRequest);

        if (token == null || !jwtService.validateToken(token)) {
            log.warn("WebSocket handshake rejected — missing or invalid token");
            return false;
        }

        Claims claims = jwtService.extractAllClaims(token);
        if (!"access".equals(claims.get("type", String.class))) {
            log.warn("WebSocket handshake rejected — wrong token type");
            return false;
        }

        attributes.put("userId", claims.getSubject());
        attributes.put("authorities", claims.get("authorities", List.class));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
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
}