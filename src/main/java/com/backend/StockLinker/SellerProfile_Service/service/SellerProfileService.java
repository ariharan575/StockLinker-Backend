package com.backend.StockLinker.SellerProfile_Service.service;

import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductCategory;
import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductSubCategory;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductCategoryRepository;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductSubCategoryRepository;
import com.backend.StockLinker.SellerProfile_Service.dto.SellerProfileResponse;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.backend.StockLinker.Profile_Service.model.SellerProduct;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import com.backend.StockLinker.SellerProfile_Service.Repository.StorefrontProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final BusinessProfileRepository businessProfileRepository;
    private final SellerProductRepository sellerProductRepository;
    private final ProductSubCategoryRepository productSubCategoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Transactional(readOnly = true)
    public SellerProfileResponse getStorefrontProfile(String businessProfileId,String viewerUserId) {

        BusinessProfile profile = businessProfileRepository.findById(businessProfileId)
                .orElseThrow(() -> new RuntimeException("Supplier profile not found or inactive."));

        BusinessProfile viewerProfile = businessProfileRepository.findByUserId(viewerUserId).orElse(null);

        if (viewerProfile != null && !viewerProfile.getId().equals(profile.getId())) {
            // ...block them if they are the same business type!
            if (viewerProfile.getBusinessType().equalsIgnoreCase(profile.getBusinessType())) {
                throw new RuntimeException("Access Denied: You cannot view profiles of the same business type.");
            }
        }

        // 1. Granular Structured Address Extraction
        String addressLine1 = null, addressLine2 = null, city = null, district = null, state = null, pincode = null, landmark = null;

        if (profile.getBusinessAddress() != null) {
            addressLine1 = profile.getBusinessAddress().getAddress();
            addressLine2 = profile.getBusinessAddress().getAlternate_address();
            city = profile.getBusinessAddress().getCity();
            district = profile.getBusinessAddress().getDistrict();
            state = profile.getBusinessAddress().getState();
            pincode = profile.getBusinessAddress().getPincode();
            landmark = profile.getBusinessAddress().getLandmark();
        }

        // 2. Logistics & Delivery Mapping
        Integer coverageRadiusKm = null; BigDecimal minimumOrderValue = null; BigDecimal deliveryCharge = null; String operatingDays = null;

        if (profile.getDeliveryConfiguration() != null) {
            coverageRadiusKm = profile.getDeliveryConfiguration().getCoverageRadiusKm();
            minimumOrderValue = profile.getDeliveryConfiguration().getMinimumOrderValue();
            deliveryCharge = profile.getDeliveryConfiguration().getDeliveryCharge();
            operatingDays = profile.getDeliveryConfiguration().getOperatingDays();
        }

        // 3. Resolve Real Category Names and SubCategory Images
        List<String> resolvedCategoryNames = new ArrayList<>();
        List<SellerProfileResponse.SubCategoryDto> subCategoryDtos = new ArrayList<>();

        if (profile.getCategoryIds() != null && !profile.getCategoryIds().isEmpty()) {
            List<String> catIds = Arrays.asList(profile.getCategoryIds().split(","));
            try {
                List<ProductCategory> categories = productCategoryRepository.findAllById(catIds);
                resolvedCategoryNames = categories.stream().map(ProductCategory::getName).collect(Collectors.toList());

                List<ProductSubCategory> subCats = productSubCategoryRepository.findByProductCategoryIdIn(catIds);
                subCategoryDtos = subCats.stream()
                        .map(sc -> SellerProfileResponse.SubCategoryDto.builder()
                                .id(sc.getId())
                                .name(sc.getName())
                                .image(sc.getImageName() != null ? sc.getImageName() : "https://picsum.photos/seed/" + sc.getId() + "/200")
                                .build())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                System.out.println("Error resolving categories: " + e.getMessage());
            }
        }

        // 4. Dynamic Market Rank & One-Time Rating Check
        int currentTrust = profile.getTrustScore() != null ? profile.getTrustScore() : 0;
        long rank = businessProfileRepository.countByTrustScoreGreaterThan(currentTrust) + 1;

        // Mocking the check if the viewer has already rated (Assume logic exists in DB)
        boolean hasViewerRated = false;

        return SellerProfileResponse.builder()
                .businessId(profile.getId())
                .userId(profile.getUserId())
                .businessName(profile.getBusinessName())
                .ownerName(profile.getOwnerName())
                .businessType(profile.getBusinessType())
                .addressLine1(addressLine1).addressLine2(addressLine2).city(city).district(district).state(state).pincode(pincode).landmark(landmark)
                .businessDescription(profile.getBusinessDescription())
                .website(profile.getWebsite())
                .whatsappNumber(profile.getWhatsappNumber())
                .verificationStatus(profile.getVerificationStatus())
                .trustScore(currentTrust)
                .marketplaceRank((int) rank)
                .totalOrdersFulfilled(profile.getTotalOrdersFulfilled() != null ? profile.getTotalOrdersFulfilled() : 0)
                .hasRated(hasViewerRated)
                .businessEmail(profile.getBusinessEmail())
                .mobileNumber(profile.getMobileNumber())
                .gstNumber(profile.getGstNumber())
                .yearsInBusiness(profile.getYearsInBusiness() != null ? profile.getYearsInBusiness() : 0)
                .openingTime(profile.getOpeningTime())
                .closingTime(profile.getClosingTime())
                .operatingDays(operatingDays)
                .storeSize(profile.getStoreSize() != null ? profile.getStoreSize().name() : null)
                .coverageRadiusKm(coverageRadiusKm)
                .minimumOrderValue(minimumOrderValue)
                .deliveryCharge(deliveryCharge)
                .deliverySupported(profile.isDeliverySupported())
                .rating(profile.getRating() != null ? profile.getRating() : 0.0)
                .reviewCount(profile.getReviewCount() != null ? profile.getReviewCount() : 0)
                .primaryCategoryNames(resolvedCategoryNames)
                .subCategories(subCategoryDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SellerProfileResponse.StorefrontProductDto> getStorefrontProducts(
            String businessProfileId, String search, String category, String brand, String sortPrice) {
        Specification<SellerProduct> spec = StorefrontProductSpecification.getBuyerVisibleProducts(businessProfileId, search, category, brand);
        Sort sort = Sort.unsorted();
        if (sortPrice != null && !sortPrice.equals("none")) {
            sort = sortPrice.equals("asc") ? Sort.by("price").ascending() : Sort.by("price").descending();
        }
        return sellerProductRepository.findAll(spec, sort).stream()
                .map(p -> SellerProfileResponse.StorefrontProductDto.builder()
                        .id(p.getId()).productName(p.getProductName()).brand(p.getBrand())
                        .category(p.getMasterProduct().getProductSubCategory().getProductCategory().getName())
                        .unit(p.getUnit()).price(p.getPrice()).minimumOrderQuantity(p.getMinimumOrderQuantity())
                        .bulkDealQuantity(p.getBulkDealQuantity()).bulkDealPrice(p.getBulkDealPrice()).availableStock(p.getAvailableStock())
                        .build()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getStorefrontFilters(String businessProfileId) {
        BusinessProfile profile = businessProfileRepository.findById(businessProfileId).orElseThrow();
        return Map.of("brands", sellerProductRepository.findDistinctBrandsBySellerId(profile.getUserId()),
                "categories", sellerProductRepository.findDistinctCategoriesBySellerId(profile.getUserId()));
    }

    @Transactional
    public void submitCommunityRating(String businessProfileId, Integer newRating, String raterUserId) {
        if (newRating == null || newRating < 1 || newRating > 5) return;
        BusinessProfile profile = businessProfileRepository.findById(businessProfileId).orElseThrow();

        // Update Ratings
        int currentReviews = profile.getReviewCount() != null ? profile.getReviewCount() : 0;
        double currentRating = profile.getRating() != null ? profile.getRating() : 0.0;
        double updatedRating = ((currentRating * currentReviews) + newRating) / (currentReviews + 1);
        profile.setRating(Math.round(updatedRating * 10.0) / 10.0);
        profile.setReviewCount(currentReviews + 1);

        // Algorithmic Trust Score & Market Rank Engine
        int currentTrust = profile.getTrustScore() != null ? profile.getTrustScore() : 50;
        if (newRating >= 4 && currentTrust < 100) profile.setTrustScore(Math.min(100, currentTrust + 2));
        else if (newRating <= 2 && currentTrust > 0) profile.setTrustScore(Math.max(0, currentTrust - 3));

        businessProfileRepository.save(profile);
    }
}