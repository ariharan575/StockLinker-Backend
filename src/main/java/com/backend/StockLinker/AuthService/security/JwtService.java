package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long jwtAccessTokenExpiration;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    private SecretKey cachedSigningKey;

    private JwtParser jwtParser;

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_DEVICE_ID = "deviceId";
    private static final String CLAIM_AUTHORITIES = "authorities";


    @PostConstruct
    public void init() {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        this.cachedSigningKey = Keys.hmacShaKeyFor(keyBytes);

        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(this.cachedSigningKey)
                .setAllowedClockSkewSeconds(60)
                .requireIssuer(this.jwtIssuer)
                .build();

        log.info("JwtService initialized with cached SecretKey and optimized JwtParser.");
    }

    private SecretKey getSigningKey() {
        return this.cachedSigningKey;
    }

    public String generateAccessToken(User user, String deviceId) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Cannot generate token for a null user or missing user ID");
        }

        Map<String, Object> claims = new HashMap<>();

        claims.put(CLAIM_TYPE, "access");
        claims.put(CLAIM_DEVICE_ID, deviceId);

        // Map Roles and Permissions directly into the JWT payload for stateless authentication
        List<String> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add("ROLE_" + user.getRole().getName());
            if (user.getRole().getPermissions() != null) {
                user.getRole().getPermissions().forEach(permission ->
                        authorities.add(permission.getName())
                );
            }
        }
        claims.put(CLAIM_AUTHORITIES, authorities);

        return Jwts.builder()
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getId())
                .setIssuer(jwtIssuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtAccessTokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public boolean validateToken(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("Malformed JWT: {}", e.getMessage());
        } catch (SignatureException e) {
            log.debug("Invalid signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("Empty claims: {}", e.getMessage());
        }
        return false;
    }
}