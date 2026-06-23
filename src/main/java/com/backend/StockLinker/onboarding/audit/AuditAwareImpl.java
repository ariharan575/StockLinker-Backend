package com.backend.StockLinker.onboarding.audit;

import com.backend.StockLinker.onboarding.util.Constants;
import com.backend.StockLinker.onboarding.util.SecurityUtil;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Auditor aware implementation for JPA auditing.
 */
@Component
public class AuditAwareImpl implements AuditorAware<String> {

    /**
     * Fetch current auditor.
     *
     * @return authenticated auditor
     */
    @Override
    public Optional<String> getCurrentAuditor() {

        try {

            Long userId =
                    SecurityUtil.getCurrentUserId();

            return Optional.of(
                    String.valueOf(userId)
            );

        } catch (Exception exception) {

            return Optional.of(
                    Constants.SYSTEM_USER
            );
        }
    }
}