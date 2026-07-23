package com.backend.StockLinker.ProfileService.repository.postgres;

import com.backend.StockLinker.ProfileService.model.SellerProduct;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class SellerProductSpecification {

    public static Specification<SellerProduct> getFilteredProducts(
            String sellerId, String search, String category, String brand, String availability) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("sellerId"), sellerId));

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), searchPattern);
                Predicate brandMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), searchPattern);
                predicates.add(criteriaBuilder.or(nameMatch, brandMatch));
            }

            if (category != null && !category.equalsIgnoreCase("all")) {
                predicates.add(criteriaBuilder.equal(
                        root.join("masterProduct").join("productSubCategory").join("productCategory").get("name"), category));
            }

            if (brand != null && !brand.equalsIgnoreCase("all")) {
                predicates.add(criteriaBuilder.equal(root.get("brand"), brand));
            }

            if (availability != null && !availability.equalsIgnoreCase("all")) {
                if (availability.equalsIgnoreCase("out")) {
                    predicates.add(criteriaBuilder.equal(root.get("availableStock"), 0));
                } else if (availability.equalsIgnoreCase("low")) {
                    predicates.add(criteriaBuilder.and(
                            criteriaBuilder.greaterThan(root.get("availableStock"), 0),
                            criteriaBuilder.lessThanOrEqualTo(root.get("availableStock"), root.get("minimumOrderQuantity"))
                    ));
                } else if (availability.equalsIgnoreCase("available")) {
                    predicates.add(criteriaBuilder.greaterThan(root.get("availableStock"), root.get("minimumOrderQuantity")));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}