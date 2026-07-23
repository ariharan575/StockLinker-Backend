package com.backend.StockLinker.ProfileService.repository.postgres;

import com.backend.StockLinker.ProfileService.model.SellerProduct;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class StorefrontProductSpecification {

    public static Specification<SellerProduct> getBuyerVisibleProducts(
            String businessProfileId, String search, String category, String brand) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // STRICT SAAS RULES: Only this specific business, only ACTIVE status, and available stock > 0
            predicates.add(criteriaBuilder.equal(root.get("businessProfileId"), businessProfileId));
            predicates.add(criteriaBuilder.equal(root.get("status"), "ACTIVE"));
            predicates.add(criteriaBuilder.greaterThan(root.get("availableStock"), 0));

            // Dynamic Search (Name or Brand)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), searchPattern);
                Predicate brandMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), searchPattern);
                predicates.add(criteriaBuilder.or(nameMatch, brandMatch));
            }

            // Category Filter
            if (category != null && !category.equalsIgnoreCase("all")) {
                predicates.add(criteriaBuilder.equal(
                        root.join("masterProduct").join("productSubCategory").join("productCategory").get("name"), category));
            }

            // Brand Filter
            if (brand != null && !brand.equalsIgnoreCase("all")) {
                predicates.add(criteriaBuilder.equal(root.get("brand"), brand));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
