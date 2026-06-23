package com.backend.StockLinker.onboarding.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Audit logging service for onboarding operations.
 */
@Slf4j
@Service
public class OnboardingAuditService {

    /**
     * Log onboarding step activity.
     *
     * @param userId authenticated user id
     * @param action action performed
     * @param description activity description
     */
    public void logOnboardingActivity(
            final Long userId,
            final String action,
            final String description
    ) {

        log.info(
                """
                Onboarding Activity Logged:
                UserId: {}
                Action: {}
                Description: {}
                Timestamp: {}
                """,
                userId,
                action,
                description,
                LocalDateTime.now()
        );
    }

    /**
     * Log onboarding validation failure.
     *
     * @param userId authenticated user id
     * @param reason failure reason
     */
    public void logValidationFailure(
            final Long userId,
            final String reason
    ) {

        log.warn(
                """
                Onboarding Validation Failed:
                UserId: {}
                Reason: {}
                Timestamp: {}
                """,
                userId,
                reason,
                LocalDateTime.now()
        );
    }

    /**
     * Log onboarding completion.
     *
     * @param userId authenticated user id
     */
    public void logOnboardingCompletion(
            final Long userId
    ) {

        log.info(
                """
                Onboarding Completed Successfully:
                UserId: {}
                Timestamp: {}
                """,
                userId,
                LocalDateTime.now()
        );
    }

    /**
     * Log draft recovery event.
     *
     * @param userId authenticated user id
     */
    public void logDraftRecovery(
            final Long userId
    ) {

        log.info(
                """
                Onboarding Draft Recovered:
                UserId: {}
                Timestamp: {}
                """,
                userId,
                LocalDateTime.now()
        );
    }
}