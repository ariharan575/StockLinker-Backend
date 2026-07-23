package com.backend.StockLinker.MessageService.controller;

import com.backend.StockLinker.MessageService.dto.response.UnreadCountResponse;
import com.backend.StockLinker.MessageService.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/chat/unread
 * Split into its own controller since it doesn't belong under /conversations or /messages specifically.
 */
@RestController
@RequestMapping("/api/chat/unread")
@RequiredArgsConstructor
public class UnreadController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(messageService.getUnreadCount());
    }
}