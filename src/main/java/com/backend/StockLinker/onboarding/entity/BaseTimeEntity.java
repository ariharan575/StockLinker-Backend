package com.backend.StockLinker.onboarding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity for timestamp management.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseTimeEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        final LocalDateTime currentDateTime = LocalDateTime.now();
        this.createdAt = currentDateTime;
        this.updatedAt = currentDateTime;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}