package com.backend.StockLinker.Dashboard_Service.service;

import com.backend.StockLinker.Dashboard_Service.dto.OmniSearchDto;
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
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // 1. Fetch Active Products
        long activeProducts = sellerProductRepository.countByBusinessProfileIdAndStatus(profile.getId(), "ACTIVE");

        // 2. Fetch Fulfilled Orders
        long fulfilledOrders = orderRepository.countBySellerIdAndStatus(userId, com.backend.StockLinker.Order_Service.enums.OrderStatus.DELIVERED);

        // 3. Fetch Enquiries from the last 3 days
        java.time.LocalDateTime threeDaysAgo = java.time.LocalDateTime.now().minusDays(3);
        long recentEnquiries = globalEnquiryRepository.countRelevantEnquiriesSince(profile.getId(), threeDaysAgo);

        // Return unified KPI payload
        return Map.of(
                "ownerName", profile.getOwnerName(),
                "activeProducts", activeProducts,
                "fulfilledOrders", fulfilledOrders,
                "recentEnquiries", recentEnquiries
        );
    }

    @Transactional(readOnly = true)
    public OmniSearchDto globalSearch(String query, String userId) { // Added userId parameter
        if (query == null || query.trim().length() < 2) {
            return OmniSearchDto.builder()
                    .products(List.of()).categories(List.of()).sellers(List.of()).build();
        }

        // 1. Search Products (Common for all)
        List<OmniSearchDto.ProductSuggestion> products = masterProductRepository
                .findTop10ByProductNameContainingIgnoreCase(query)
                .stream().limit(5).map(p -> OmniSearchDto.ProductSuggestion.builder()
                        .id(p.getId())
                        .name(p.getProductName())
                        .build())
                .collect(Collectors.toList());

        // 2. Search Categories & Subcategories (Common for all)
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

        // 3. ROLE-BASED Search Sellers
        List<OmniSearchDto.SellerSuggestion> sellers = List.of();

        // Fetch the current user to know their role
        BusinessProfile currentUser = businessProfileRepository.findByUserId(userId).orElse(null);

        if (currentUser != null && currentUser.getBusinessType() != null) {
            String targetRole = "";

            // Logic: Wholesaler searches for Shopkeepers, Shopkeepers search for Wholesalers
            if (currentUser.getBusinessType().equalsIgnoreCase("wholesaler")) {
                targetRole = "shopkeeper";
            } else if (currentUser.getBusinessType().equalsIgnoreCase("shopkeeper")) {
                targetRole = "wholesaler";
            }

            if (!targetRole.isEmpty()) {
                // Fetch ONLY the target role
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