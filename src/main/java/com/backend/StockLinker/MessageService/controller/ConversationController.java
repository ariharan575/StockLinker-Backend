package com.backend.StockLinker.MessageService.controller;

import com.backend.StockLinker.MessageService.dto.request.ConversationSearchRequest;
import com.backend.StockLinker.MessageService.dto.request.CreateConversationRequest;
import com.backend.StockLinker.MessageService.dto.response.ConversationListResponse;
import com.backend.StockLinker.MessageService.dto.response.ConversationResponse;
import com.backend.StockLinker.MessageService.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * POST /api/chat/conversations
     * Creates a new buyer<->seller conversation, or returns the existing one if it already exists.
     */
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse response = conversationService.createOrGetConversation(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * GET /api/chat/conversations
     * Lists the current user's conversations, paginated, newest last-message first.
     */
    @GetMapping
    public ResponseEntity<ConversationListResponse> listConversations(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ConversationSearchRequest request = ConversationSearchRequest.builder()
                .keyword(keyword)
                .includeArchived(includeArchived)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(conversationService.listConversations(request));
    }

    /**
     * GET /api/chat/conversations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversation(@PathVariable String id) {
        return ResponseEntity.ok(conversationService.getConversationById(id));
    }
}