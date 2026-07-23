package com.backend.StockLinker.OrderService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUserOrderUpdate(String userId, String orderId, String status, String eventType, Map<String, Object> extraData) {
        try {
            // Use a standard HashMap to prevent all Java type inference errors
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", orderId);
            payload.put("status", status);
            payload.put("eventType", eventType);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("data", extraData != null ? extraData : new java.util.HashMap<>());

            // Cast payload to Object to resolve the ambiguous method call error
            messagingTemplate.convertAndSend("/topic/orders/" + userId, (Object) payload);
            log.info("Successfully pushed WebSocket order update to user: {}, event: {}", userId, eventType);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user: {}", userId, e);
        }
    }
}