package com.backend.StockLinker.MessageService.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the WebSocket session Principal from the attributes JwtHandshakeInterceptor
 * populated during the handshake. UsernamePasswordAuthenticationToken satisfies
 * Principal (via Authentication), so downstream @MessageMapping / SimpMessagingTemplate
 * code sees the same identity shape as the REST layer's SecurityContext.
 */
@Component
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    @SuppressWarnings("unchecked")
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String userId = (String) attributes.get("userId");
        List<String> authorityStrings = (List<String>) attributes.get("authorities");

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (authorityStrings != null) {
            for (String auth : authorityStrings) {
                authorities.add(new SimpleGrantedAuthority(auth));
            }
        }

        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }
}
