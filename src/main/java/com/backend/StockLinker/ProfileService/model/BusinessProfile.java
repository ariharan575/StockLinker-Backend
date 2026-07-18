package com.backend.StockLinker.ProfileService.model;

import com.backend.StockLinker.AuthService.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;

@Entity
@Table(name = "business_profiles", indexes = {
        @Index(name = "idx_bp_user_id", columnList = "user_id", unique = true),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BusinessProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(name = "business_type", nullable = false, length = 50)
    private String businessType;

    @Column(name = "business_email", length = 100)
    private String businessEmail;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    private String alternateMobileNumber;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Column(name = "years_in_business")
    private Integer yearsInBusiness;

    @Column(name = "business_description", columnDefinition = "TEXT")
    private String businessDescription;

    @Column(name = "delivery_supported", nullable = false)
    private boolean deliverySupported;

    @Column(name = "store_size", length = 50)
    private String storeSize;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "category_ids", columnDefinition = "TEXT")
    private String categoryIds;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @OneToOne(mappedBy = "businessProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private BusinessAddress businessAddress;
}