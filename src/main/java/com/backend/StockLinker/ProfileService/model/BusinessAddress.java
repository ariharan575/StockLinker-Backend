package com.backend.StockLinker.ProfileService.model;

import com.backend.StockLinker.AuthService.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "business_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BusinessAddress extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_profile_id", nullable = false, unique = true)
    private BusinessProfile businessProfile;

    @Column(name = "address" , length = 255)
    private String address;

    @Column(name = "Alternate_address")
    private String alternate_address;

    @Column(name = "street", length = 150)
    private String street;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "pincode", nullable = false, length = 20)
    private String pincode;

    @Column(name = "landmark", length = 150)
    private String landmark;

}