package com.backend.StockLinker.ProfileService.model;

import com.backend.StockLinker.AuthService.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity
@Table(name = "delivery_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DeliveryConfiguration extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_profile_id", nullable = false, unique = true)
    private BusinessProfile businessProfile;

    @Column(name = "coverage_radius_km")
    private Integer coverageRadiusKm;

    @Column(name = "minimum_order_value", precision = 12, scale = 2)
    private BigDecimal minimumOrderValue;

    @Column(name = "delivery_charge", precision = 12, scale = 2)
    private BigDecimal deliveryCharge;

    @Column(name = "operating_days", length = 100)
    private String operatingDays;

    @Column(name = "route_schedule", length = 255)
    private String routeSchedule;
}