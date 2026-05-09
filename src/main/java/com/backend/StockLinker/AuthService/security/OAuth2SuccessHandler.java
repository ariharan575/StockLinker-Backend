 package com.backend.StockLinker.AuthService.security;

import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.service.AuthFlowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

 @Component
 @RequiredArgsConstructor
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

         // Device ID
         String deviceId = request.getParameter("deviceId");
         if (deviceId == null || deviceId.isBlank()) {
             deviceId = generateDeviceId(request);
         }

         // Unified auth flow - get COMPLETE response
         Map<String, Object> result = authFlowService.login(
                 user,
                 "GOOGLE",
                 deviceId,
                 request,
                 response
         );

         // Extract ALL data
         String accessToken = (String) result.get("accessToken");
         String refreshToken = (String) result.get("refreshToken");
         String userId = (String) result.get("userId");
         Boolean isNewUser = (Boolean) result.get("isNewUser");
         Boolean hasBusinessRole = (Boolean) result.get("hasBusinessRole");

         // Build redirect URL with ALL parameters
         String redirectUrl = String.format(
                 "http://localhost:5173/oauth-success?token=%s&userId=%s&isNewUser=%s&hasBusinessRole=%s",
                 accessToken,
                 userId,
                 isNewUser != null ? isNewUser : false,
                 hasBusinessRole != null ? hasBusinessRole : false
         );

         response.sendRedirect(redirectUrl);
     }

     private String generateDeviceId(HttpServletRequest request) {
         String userAgent = request.getHeader("User-Agent");
         String ip = request.getRemoteAddr();
         return UUID.nameUUIDFromBytes((userAgent + ip).getBytes()).toString();
     }
 }