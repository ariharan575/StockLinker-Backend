package com.backend.StockLinker.Dashboard_Service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OmniSearchDto {
    private List<ProductSuggestion> products;
    private List<CategorySuggestion> categories;
    private List<SellerSuggestion> sellers;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductSuggestion {
        private String id;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategorySuggestion {
        private String id;
        private String parentCategoryId; // Needed so the frontend knows which tab to open
        private String name;
        private String type; // "CATEGORY" or "SUBCATEGORY"
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SellerSuggestion {
        private String businessProfileId;
        private String businessName;
        private String location;
    }
}