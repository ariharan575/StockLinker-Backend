package com.backend.StockLinker.Order_Service.service;

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
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", orderId);
            payload.put("status", status);
            payload.put("eventType", eventType);
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("data", extraData != null ? extraData : new java.util.HashMap<>());

            messagingTemplate.convertAndSend("/topic/orders/" + userId, (Object) payload);
            log.info("WebSocket push sent to user: {}, event: {}", userId, eventType);
        } catch (Exception e) {
            log.error("WebSocket push failed for user: {}", userId, e);
        }
    }
}