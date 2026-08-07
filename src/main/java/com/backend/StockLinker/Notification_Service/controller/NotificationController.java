package com.backend.StockLinker.Notification_Service.controller;

import com.backend.StockLinker.Notification_Service.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(Authentication auth) {
        return ResponseEntity.ok(notificationService.getUserNotifications(auth.getName()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id, Authentication auth, HttpServletRequest request) {
        notificationService.markAsRead(id, auth.getName(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication auth, HttpServletRequest request) {
        notificationService.markAllAsRead(auth.getName(), request);
        return ResponseEntity.ok().build();
    }
}