package com.backend.StockLinker.config;

import com.backend.StockLinker.ProfileService.model.*;
import com.backend.StockLinker.ProfileService.repository.postgres.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MockProfileSeeder implements CommandLineRunner {

    private final BusinessProfileRepository profileRepository;
    private final BusinessAddressRepository addressRepository;
    private final DeliveryConfigurationRepository deliveryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (profileRepository.count() < 6) {
            // 3 UNIVERSAL WHOLESALERS
            createMockUser("mock-ws-1", "Apex Electronics Mfg", "WHOLESALER", "Universal", 4.8);
            createMockUser("mock-ws-2", "Global Imports Ltd", "WHOLESALER", "Universal", 4.5);
            createMockUser("mock-ws-3", "Zenith Traders Co", "WHOLESALER", "Universal", 4.7);

            // 3 UNIVERSAL SHOPKEEPERS
            createMockUser("mock-sk-1", "City Center Mart", "SHOPKEEPER", "Universal", 4.2);
            createMockUser("mock-sk-2", "Pudukottai Retail Hub", "SHOPKEEPER", "Universal", 4.4);
            createMockUser("mock-sk-3", "Thiruvarur General Store", "SHOPKEEPER", "Universal", 4.3);

            System.out.println("✅ Universal Mock Database: 6 Flexible Ghost Profiles Seeded.");
        }
    }

    private void createMockUser(String userId, String name, String type, String district, Double rating) {
        if (profileRepository.findByUserId(userId).isPresent()) return;

        BusinessProfile profile = new BusinessProfile();
        profile.setUserId(userId);
        profile.setBusinessName(name);
        profile.setOwnerName(name + " Owner");
        profile.setBusinessType(type);
        profile.setMobileNumber("9999999999");
        profile.setDeliverySupported(true);
        profile.setStatus("ACTIVE");
        profile.setVerificationStatus("VERIFIED");
        profile.setRating(rating);
        profile.setReviewCount(120);
        profile.setYearsInBusiness(5);
        profile.setTotalOrdersFulfilled(500);
        profile.setResponseTime("< 1 hr");
        profile = profileRepository.save(profile);

        BusinessAddress address = new BusinessAddress();
        address.setBusinessProfile(profile);
        address.setDistrict(district);
        address.setCity(district);
        address.setState("Tamil Nadu");
        address.setPincode("600000");
        addressRepository.save(address);

        DeliveryConfiguration delivery = new DeliveryConfiguration();
        delivery.setBusinessProfile(profile);
        delivery.setCoverageRadiusKm(50);
        delivery.setOperatingDays("Same Day");
        deliveryRepository.save(delivery);
    }
}