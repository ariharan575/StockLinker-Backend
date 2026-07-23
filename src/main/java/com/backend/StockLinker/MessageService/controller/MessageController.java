package com.backend.StockLinker.MessageService.controller;

import com.backend.StockLinker.MessageService.dto.request.EditMessageRequest;
import com.backend.StockLinker.MessageService.dto.request.ReadMessageRequest;
import com.backend.StockLinker.MessageService.dto.request.SendMessageRequest;
import com.backend.StockLinker.MessageService.dto.response.MessageResponse;
import com.backend.StockLinker.MessageService.dto.response.PagedMessageResponse;
import com.backend.StockLinker.MessageService.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(messageService.sendMessage(request));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<PagedMessageResponse> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(messageService.getMessages(conversationId, pageable));
    }

    @PutMapping("/read/{conversationId}")
    public ResponseEntity<MessageResponse> markAsRead(
            @PathVariable String conversationId,
            @RequestBody(required = false) ReadMessageRequest request) {
        if (request == null) request = new ReadMessageRequest();
        return ResponseEntity.ok(messageService.markAsRead(conversationId, request));
    }

    @PutMapping("/delivered/{messageId}")
    public ResponseEntity<MessageResponse> markAsDelivered(@PathVariable String messageId) {
        return ResponseEntity.ok(messageService.markAsDelivered(messageId));
    }

    @PutMapping("/edit/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(messageService.editMessage(messageId, request));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.ok().build();
    }
}
