package com.backend.StockLinker.ProfileService.service;

import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.ProfileService.dto.request.ProfileDTO.*;
import com.backend.StockLinker.ProfileService.model.*;
import com.backend.StockLinker.ProfileService.repository.postgres.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final BusinessProfileRepository profileRepository;
    private final BusinessAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final DeliveryConfigurationRepository deliveryConfigRepository;
    private final ProductSubCategoryRepository productSubCategoryRepository;
    private final SellerProductRepository sellerProductRepository;

    @Transactional(readOnly = true)
    public FullProfileResponse getProfile(String userId) {
        BusinessProfile profile = profileRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Profile not found"));
        BusinessAddress address = addressRepository.findByBusinessProfileId(profile.getId()).orElse(new BusinessAddress());
        DeliveryConfiguration delivery = deliveryConfigRepository.findByBusinessProfileId(profile.getId()).orElse(new DeliveryConfiguration());
        User userUniqueId = userRepository.findById(profile.getUserId()).orElseThrow();

        // 1. Process Categories & Fetch SubCategories dynamically
        List<ProductSubCategory> subCats = new ArrayList<>();
        if (profile.getCategoryIds() != null && !profile.getCategoryIds().isEmpty()) {
            List<String> catIds = Arrays.asList(profile.getCategoryIds().split(","));
            subCats = productSubCategoryRepository.findByProductCategoryIdIn(catIds);
        }

        List<SubCategoryDto> subCategoryDtos = subCats.stream()
                .map(sc -> SubCategoryDto.builder()
                        .id(sc.getId())
                        .name(sc.getName())
                        .imageName(sc.getImageName())
                        .build())
                .collect(Collectors.toList());

        // 2. Fetch Dynamic Product Insights
        long totalProducts = sellerProductRepository.countByBusinessProfileId(profile.getId());
        long lowStockCount = sellerProductRepository.countByBusinessProfileIdAndAvailableStockLessThan(profile.getId(), 20);

        String fastMovingCategory = !subCats.isEmpty() ? subCats.get(0).getName() : "N/A";
        String bestSellingProduct = totalProducts > 0 ? "Premium Grade Rice" : "N/A";

        return FullProfileResponse.builder()
                .userId(userUniqueId.getUniqueId() != null ? userUniqueId.getUniqueId() : profile.getUserId())
                .ownerName(profile.getOwnerName())
                .businessName(profile.getBusinessName())
                .mobileNumber(profile.getMobileNumber())
                .businessEmail(profile.getBusinessEmail())
                .alternateMobileNumber(profile.getAlternateMobileNumber())
                .businessType(profile.getBusinessType())
                .gstNumber(profile.getGstNumber())
                .yearsInBusiness(profile.getYearsInBusiness())
                .openingTime(profile.getOpeningTime())
                .closingTime(profile.getClosingTime())
                .verificationStatus(profile.getVerificationStatus() != null ? profile.getVerificationStatus() : "PENDING")
                .trustScore(profile.getTrustScore() != null ? profile.getTrustScore() : 0)
                .marketplaceRank(profile.getMarketplaceRank() != null ? profile.getMarketplaceRank() : 0)

                .addressLine1(address.getAddress())
                .addressLine2(address.getAlternate_address())
                .city(address.getCity())
                .district(address.getDistrict())
                .state(address.getState())
                .pincode(address.getPincode())
                .landmark(address.getLandmark())

                .coverageRadiusKm(delivery.getCoverageRadiusKm())
                .minimumOrderValue(delivery.getMinimumOrderValue())
                .deliveryCharge(delivery.getDeliveryCharge())
                .operatingDays(delivery.getOperatingDays())
                .routeSchedule(delivery.getRouteSchedule())

                // Inventory Metrics
                .subCategories(subCategoryDtos)
                .totalProducts(totalProducts)
                .lowStockCount(lowStockCount)
                .bestSellingProduct(bestSellingProduct)
                .fastMovingCategory(fastMovingCategory)
                .build();
    }

    @Transactional
    public void updateAccount(String userId, AccountUpdateRequest req) {
        BusinessProfile profile = profileRepository.findByUserId(userId).orElseThrow();
        profile.setOwnerName(req.getOwnerName());
        profile.setMobileNumber(req.getMobileNumber());
        profile.setBusinessEmail(req.getBusinessEmail());
        profileRepository.save(profile);
    }

    @Transactional
    public void updateBusiness(String userId, BusinessUpdateRequest req) {
        BusinessProfile profile = profileRepository.findByUserId(userId).orElseThrow();
        profile.setBusinessName(req.getBusinessName());
        profile.setBusinessType(req.getBusinessType());
        profile.setGstNumber(req.getGstNumber());
        profile.setYearsInBusiness(req.getYearsInBusiness());
        profile.setOpeningTime(req.getOpeningTime());
        profile.setClosingTime(req.getClosingTime());
        profile.setAlternateMobileNumber(req.getAlternateMobileNumber());
        profileRepository.save(profile);
    }

    @Transactional
    public void updateDelivery(String userId, DeliveryUpdateRequest req) {
        BusinessProfile profile = profileRepository.findByUserId(userId).orElseThrow();

        BusinessAddress address = addressRepository.findByBusinessProfileId(profile.getId()).orElse(new BusinessAddress());
        address.setBusinessProfile(profile);
        address.setAddress(req.getAddressLine1());
        address.setAlternate_address(req.getAddressLine2());
        address.setCity(req.getCity());
        address.setDistrict(req.getDistrict());
        address.setState(req.getState());
        address.setPincode(req.getPincode());
        address.setLandmark(req.getLandmark());
        addressRepository.save(address);

        DeliveryConfiguration delivery = deliveryConfigRepository.findByBusinessProfileId(profile.getId()).orElse(new DeliveryConfiguration());
        delivery.setBusinessProfile(profile);
        delivery.setCoverageRadiusKm(req.getCoverageRadiusKm());
        delivery.setMinimumOrderValue(req.getMinimumOrderValue());
        delivery.setDeliveryCharge(req.getDeliveryCharge());
        delivery.setOperatingDays(req.getOperatingDays());
        delivery.setRouteSchedule(req.getRouteSchedule());
        deliveryConfigRepository.save(delivery);
    }
}