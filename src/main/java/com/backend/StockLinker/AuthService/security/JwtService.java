package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.model.User;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j  
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long jwtAccessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpiration;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    // =========================================================
    // 🔐 GET SIGNING KEY
    // =========================================================
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // =========================================================
    // ✅ GENERATE ACCESS TOKEN
    // =========================================================
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("userId", user.getId());

        String roleNames = (user.getRole() != null && !user.getRole().isEmpty()) ? user.getRole() : null;

        claims.put("roles", roleNames);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId())
                .setIssuer(jwtIssuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtAccessTokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // =========================================================
    // 🔍 EXTRACT PERMISSIONS FROM USER
    // =========================================================
    private String extractPermissions(User user) {
        if (user.getRole() == null) return null;

        return user.getRole();
    }

    // =========================================================
    // 📤 EXTRACT ALL CLAIMS
    // =========================================================
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // =========================================================
    // 🔍 EXTRACT USER ID FROM TOKEN
    // =========================================================
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    // =========================================================
    // 🔍 EXTRACT TOKEN TYPE
    // =========================================================
    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    // =========================================================
    // 🔍 EXTRACT ROLES FROM TOKEN
    // =========================================================
    public Set<String> extractRoles(String token) {
        Object rolesObj = extractAllClaims(token).get("roles");

        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj)
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    // =========================================================
    // 🔍 EXTRACT PERMISSIONS FROM TOKEN
    // =========================================================
    public Set<String> extractPermissionsFromToken(String token) {
        Object permissionsObj = extractAllClaims(token).get("permissions");

        if (permissionsObj instanceof List<?>) {
            return ((List<?>) permissionsObj)
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    // =========================================================
    // ✅ VALIDATE TOKEN
    // =========================================================
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Invalid signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Empty claims: {}", e.getMessage());
        }
        return false;
    }

    // =========================================================
    // ⏰ CHECK IF TOKEN IS EXPIRED
    // =========================================================
    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}