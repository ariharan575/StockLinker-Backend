package com.backend.StockLinker.SellerProfile_Service.service;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.Exception.customExceptions.ForbiddenException;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
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

    // Auditing
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @Transactional(readOnly = true)
    public SellerProfileResponse getStorefrontProfile(String businessProfileId, String viewerUserId, HttpServletRequest request) {

        BusinessProfile profile = businessProfileRepository.findById(businessProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier profile not found or inactive."));

        BusinessProfile viewerProfile = businessProfileRepository.findByUserId(viewerUserId).orElse(null);

        if (viewerProfile != null && !viewerProfile.getId().equals(profile.getId())) {
            if (viewerProfile.getBusinessType().equalsIgnoreCase(profile.getBusinessType())) {
                throw new ForbiddenException("Access Denied: You cannot view profiles of the same business type.");
            }
        }

        // Audit Logging
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(viewerUserId)
                .action(AuditAction.STOREFRONT_VIEWED)
                .resourceType(ResourceType.BUSINESS)
                .resourceId(businessProfileId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .build());

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

        Integer coverageRadiusKm = null; BigDecimal minimumOrderValue = null; BigDecimal deliveryCharge = null; String operatingDays = null;

        if (profile.getDeliveryConfiguration() != null) {
            coverageRadiusKm = profile.getDeliveryConfiguration().getCoverageRadiusKm();
            minimumOrderValue = profile.getDeliveryConfiguration().getMinimumOrderValue();
            deliveryCharge = profile.getDeliveryConfiguration().getDeliveryCharge();
            operatingDays = profile.getDeliveryConfiguration().getOperatingDays();
        }

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

        int currentTrust = profile.getTrustScore() != null ? profile.getTrustScore() : 0;
        long rank = businessProfileRepository.countByTrustScoreGreaterThan(currentTrust) + 1;

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
    public Page<SellerProfileResponse.StorefrontProductDto> getStorefrontProducts(
            String businessProfileId, String search, String category, String brand, String sortPrice, int page, int size) {

        Specification<SellerProduct> spec = StorefrontProductSpecification.getBuyerVisibleProducts(businessProfileId, search, category, brand);
        Sort sort = Sort.unsorted();
        if (sortPrice != null && !sortPrice.equals("none")) {
            sort = sortPrice.equals("asc") ? Sort.by("price").ascending() : Sort.by("price").descending();
        }

        // Returns a Page directly, which Spring Boot maps to {content: [...], totalPages: X, ...}
        Page<SellerProduct> productPage = sellerProductRepository.findAll(spec, PageRequest.of(page, size, sort));

        return productPage.map(p -> SellerProfileResponse.StorefrontProductDto.builder()
                .id(p.getId()).productName(p.getProductName()).brand(p.getBrand())
                .category(p.getMasterProduct().getProductSubCategory().getProductCategory().getName())
                .unit(p.getUnit()).price(p.getPrice()).minimumOrderQuantity(p.getMinimumOrderQuantity())
                .bulkDealQuantity(p.getBulkDealQuantity()).bulkDealPrice(p.getBulkDealPrice()).availableStock(p.getAvailableStock())
                .build());
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getStorefrontFilters(String businessProfileId) {
        BusinessProfile profile = businessProfileRepository.findById(businessProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        return Map.of("brands", sellerProductRepository.findDistinctBrandsBySellerId(profile.getUserId()),
                "categories", sellerProductRepository.findDistinctCategoriesBySellerId(profile.getUserId()));
    }

    @Transactional
    public void submitCommunityRating(String businessProfileId, Integer newRating, String raterUserId, HttpServletRequest request) {
        if (newRating == null || newRating < 1 || newRating > 5) return;
        BusinessProfile profile = businessProfileRepository.findById(businessProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        int currentReviews = profile.getReviewCount() != null ? profile.getReviewCount() : 0;
        double currentRating = profile.getRating() != null ? profile.getRating() : 0.0;
        double updatedRating = ((currentRating * currentReviews) + newRating) / (currentReviews + 1);
        profile.setRating(Math.round(updatedRating * 10.0) / 10.0);
        profile.setReviewCount(currentReviews + 1);

        int currentTrust = profile.getTrustScore() != null ? profile.getTrustScore() : 50;
        if (newRating >= 4 && currentTrust < 100) profile.setTrustScore(Math.min(100, currentTrust + 2));
        else if (newRating <= 2 && currentTrust > 0) profile.setTrustScore(Math.max(0, currentTrust - 3));

        businessProfileRepository.save(profile);

        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(raterUserId)
                .action(AuditAction.RATING_SUBMITTED)
                .resourceType(ResourceType.BUSINESS)
                .resourceId(businessProfileId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .newValue(String.valueOf(newRating))
                .status(AuditLog.Status.SUCCESS)
                .build());
    }
}