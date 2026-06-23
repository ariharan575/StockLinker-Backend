package com.backend.StockLinker.onboarding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response DTO after onboarding completion.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingCompletionResponse {

    private Long userId;

    private Boolean onboardingCompleted;

    private Integer completionPercentage;

    private String completionMessage;

    private LocalDateTime completedAt;
}