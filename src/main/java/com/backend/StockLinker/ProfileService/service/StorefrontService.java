package com.backend.StockLinker.ProfileService.service;

import com.backend.StockLinker.ProfileService.dto.response.StorefrontResponse;
import com.backend.StockLinker.ProfileService.model.BusinessProfile;
import com.backend.StockLinker.ProfileService.model.SellerProduct;
import com.backend.StockLinker.ProfileService.repository.postgres.BusinessProfileRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.SellerProductRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.StorefrontProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorefrontService {

    private final BusinessProfileRepository businessProfileRepository;
    private final SellerProductRepository sellerProductRepository;

    @Transactional(readOnly = true)
    public StorefrontResponse getStorefrontProfile(String businessProfileId) {

        BusinessProfile profile = businessProfileRepository.findById(businessProfileId)
                .orElseThrow(() -> new RuntimeException("Supplier profile not found or inactive."));

        // Safely extract short location
        String location = profile.getBusinessAddress() != null ?
                profile.getBusinessAddress().getCity() + ", " + profile.getBusinessAddress().getState() : "Location Unspecified";

        // Safely extract full formatted address
        String fullAddress = "Address not provided";
        if (profile.getBusinessAddress() != null) {
            fullAddress = String.format("%s, %s, %s, %s - %s",
                    profile.getBusinessAddress().getAddress() != null ? profile.getBusinessAddress().getAddress() : "",
                    profile.getBusinessAddress().getArea() != null ? profile.getBusinessAddress().getArea() : "",
                    profile.getBusinessAddress().getCity() != null ? profile.getBusinessAddress().getCity() : "",
                    profile.getBusinessAddress().getState() != null ? profile.getBusinessAddress().getState() : "",
                    profile.getBusinessAddress().getPincode() != null ? profile.getBusinessAddress().getPincode() : ""
            ).replaceAll("^, |, $", "").replaceAll(", ,", ", ").trim();
        }

        // Safely extract Delivery Configurations
        Integer coverageRadiusKm = null;
        BigDecimal minimumOrderValue = null;
        BigDecimal deliveryCharge = null;
        String operatingDays = null;

        if (profile.getDeliveryConfiguration() != null) {
            coverageRadiusKm = profile.getDeliveryConfiguration().getCoverageRadiusKm();
            minimumOrderValue = profile.getDeliveryConfiguration().getMinimumOrderValue();
            deliveryCharge = profile.getDeliveryConfiguration().getDeliveryCharge();
            operatingDays = profile.getDeliveryConfiguration().getOperatingDays();
        }

        return StorefrontResponse.builder()
                .businessId(profile.getId())
                .businessName(profile.getBusinessName())
                .ownerName(profile.getOwnerName())
                .businessType(profile.getBusinessType())
                .location(location)
                .fullAddress(fullAddress)
                .storeSize(profile.getStoreSize() != null ? profile.getStoreSize().name() : null)
                .coverageRadiusKm(coverageRadiusKm)
                .minimumOrderValue(minimumOrderValue)
                .deliveryCharge(deliveryCharge)
                .operatingDays(operatingDays)
                .rating(profile.getRating() != null ? profile.getRating() : 0.0)
                .reviewCount(profile.getReviewCount() != null ? profile.getReviewCount() : 0)
                .yearsInBusiness(profile.getYearsInBusiness())
                .businessEmail(profile.getBusinessEmail())
                .mobileNumber(profile.getMobileNumber())
                .gstNumber(profile.getGstNumber())
                .deliverySupported(profile.isDeliverySupported())
                .openingTime(profile.getOpeningTime())
                .closingTime(profile.getClosingTime())
                .products(null) // Handled by separate endpoint
                .build();
    }

    @Transactional(readOnly = true)
    public List<StorefrontResponse.StorefrontProductDto> getStorefrontProducts(
            String businessProfileId, String search, String category, String brand, String sortPrice) {

        Specification<SellerProduct> spec = StorefrontProductSpecification.getBuyerVisibleProducts(
                businessProfileId, search, category, brand);

        Sort sort = Sort.unsorted();
        if (sortPrice != null && !sortPrice.equals("none")) {
            sort = sortPrice.equals("asc") ? Sort.by("price").ascending() : Sort.by("price").descending();
        }

        List<SellerProduct> products = sellerProductRepository.findAll(spec, sort);

        return products.stream()
                .map(p -> StorefrontResponse.StorefrontProductDto.builder()
                        .id(p.getId())
                        .productName(p.getProductName())
                        .brand(p.getBrand())
                        .category(p.getMasterProduct().getProductSubCategory().getProductCategory().getName())
                        .unit(p.getUnit())
                        .packageSize(p.getPackageSize())
                        .price(p.getPrice())
                        .minimumOrderQuantity(p.getMinimumOrderQuantity())
                        .bulkDealQuantity(p.getBulkDealQuantity())
                        .bulkDealPrice(p.getBulkDealPrice())
                        .availableStock(p.getAvailableStock())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getStorefrontFilters(String businessProfileId) {
        BusinessProfile profile = businessProfileRepository.findById(businessProfileId)
                .orElseThrow(() -> new RuntimeException("Supplier profile not found."));

        return Map.of(
                "brands", sellerProductRepository.findDistinctBrandsBySellerId(profile.getUserId()),
                "categories", sellerProductRepository.findDistinctCategoriesBySellerId(profile.getUserId())
        );
    }
}