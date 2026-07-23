package com.backend.StockLinker.MessageService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSearchRequest {

    private String keyword;

    @Builder.Default
    private boolean includeArchived = false;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}