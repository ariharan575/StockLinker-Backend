package com.backend.StockLinker.Business_Connection_Service.Entity;

import com.backend.StockLinker.Auth_Service.model.BaseEntity;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BusinessConnection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @JsonIgnore
    private BusinessProfile requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    @JsonIgnore
    private BusinessProfile receiver;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // PENDING, CONNECTED, BLOCKED

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;
}