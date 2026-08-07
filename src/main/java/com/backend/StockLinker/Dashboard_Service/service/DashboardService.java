package com.backend.StockLinker.Dashboard_Service.service;

import com.backend.StockLinker.Dashboard_Service.dto.OmniSearchDto;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.Global_Request_Service.Repository.GlobalEnquiryRepository;
import com.backend.StockLinker.Order_Service.repository.OrderRepository;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductCategoryRepository;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductSubCategoryRepository;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.MasterProductRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BusinessProfileRepository businessProfileRepository;
    private final MasterProductRepository masterProductRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductSubCategoryRepository subCategoryRepository;
    private final OrderRepository orderRepository;
    private final GlobalEnquiryRepository globalEnquiryRepository;
    private final SellerProductRepository sellerProductRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getWelcomeInfo(String userId) {
        BusinessProfile profile = businessProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found"));

        long activeProducts = sellerProductRepository.countByBusinessProfileIdAndStatus(profile.getId(), "ACTIVE");
        long fulfilledOrders = orderRepository.countBySellerIdAndStatus(userId, com.backend.StockLinker.Order_Service.enums.OrderStatus.DELIVERED);

        java.time.LocalDateTime threeDaysAgo = java.time.LocalDateTime.now().minusDays(3);
        long recentEnquiries = globalEnquiryRepository.countRelevantEnquiriesSince(profile.getId(), threeDaysAgo);

        return Map.of(
                "ownerName", profile.getOwnerName(),
                "activeProducts", activeProducts,
                "fulfilledOrders", fulfilledOrders,
                "recentEnquiries", recentEnquiries
        );
    }

    @Transactional(readOnly = true)
    public OmniSearchDto globalSearch(String query, String userId) {
        if (query == null || query.trim().length() < 2) {
            return OmniSearchDto.builder()
                    .products(List.of()).categories(List.of()).sellers(List.of()).build();
        }

        List<OmniSearchDto.ProductSuggestion> products = masterProductRepository
                .findTop10ByProductNameContainingIgnoreCase(query)
                .stream().limit(5).map(p -> OmniSearchDto.ProductSuggestion.builder()
                        .id(p.getId())
                        .name(p.getProductName())
                        .build())
                .collect(Collectors.toList());

        List<OmniSearchDto.CategorySuggestion> categories = categoryRepository
                .findTop5ByNameContainingIgnoreCaseAndActiveTrue(query)
                .stream().map(c -> OmniSearchDto.CategorySuggestion.builder()
                        .id(c.getId())
                        .parentCategoryId(c.getId())
                        .name(c.getName())
                        .type("CATEGORY")
                        .build())
                .collect(Collectors.toList());

        categories.addAll(subCategoryRepository
                .findTop5ByNameContainingIgnoreCase(query)
                .stream().map(sc -> OmniSearchDto.CategorySuggestion.builder()
                        .id(sc.getId())
                        .parentCategoryId(sc.getProductCategory().getId())
                        .name(sc.getName())
                        .type("SUBCATEGORY")
                        .build())
                .collect(Collectors.toList()));

        List<OmniSearchDto.SellerSuggestion> sellers = List.of();

        BusinessProfile currentUser = businessProfileRepository.findByUserId(userId).orElse(null);

        if (currentUser != null && currentUser.getBusinessType() != null) {
            String targetRole = "";
            if (currentUser.getBusinessType().equalsIgnoreCase("wholesaler")) {
                targetRole = "shopkeeper";
            } else if (currentUser.getBusinessType().equalsIgnoreCase("shopkeeper")) {
                targetRole = "wholesaler";
            }

            if (!targetRole.isEmpty()) {
                sellers = businessProfileRepository
                        .findTop5ByBusinessNameContainingIgnoreCaseAndBusinessTypeIgnoreCase(query, targetRole)
                        .stream().map(bp -> OmniSearchDto.SellerSuggestion.builder()
                                .businessProfileId(bp.getId())
                                .businessName(bp.getBusinessName())
                                .location(bp.getBusinessAddress() != null ? bp.getBusinessAddress().getDistrict() : "Unknown")
                                .build())
                        .collect(Collectors.toList());
            }
        }

        return OmniSearchDto.builder()
                .products(products)
                .categories(categories)
                .sellers(sellers)
                .build();
    }
}