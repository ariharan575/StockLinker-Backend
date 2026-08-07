package com.backend.StockLinker.ProductCatagory_Service.service;

import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.ProductCatagory_Service.dto.request.CategoryDTO;
import com.backend.StockLinker.ProductCatagory_Service.dto.request.SubCategoryDTO;
import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductCategory;
import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductSubCategory;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductCategoryRepository;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductSubCategoryRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final ProductSubCategoryRepository subCategoryRepository;
    private final SellerProductRepository sellerProductRepository;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllActiveCategoriesWithSubcategories() {
        // 1. Fetch all active categories (1 Query)
        List<ProductCategory> activeCategories = categoryRepository.findByActiveTrue();

        if (activeCategories.isEmpty()) {
            log.warn("No active product categories found in the database.");
            throw new ResourceNotFoundException("No active categories found in the system.");
        }

        // 2. Fetch all subcategories that belong to active categories (1 Query)
        List<ProductSubCategory> allSubCategories = subCategoryRepository.findAllActiveWithCategory();

        // Group subcategories by Category ID in memory for instant access
        Map<String, List<ProductSubCategory>> subCategoryMap = allSubCategories.stream()
                .collect(Collectors.groupingBy(sub -> sub.getProductCategory().getId()));

        // 3. Fetch all seller counts for all categories at once (1 Query)
        List<Object[]> countResults = sellerProductRepository.countDistinctSellersGroupByCategory();

        // Map the results: Category ID -> Seller Count
        Map<String, Long> sellerCountMap = countResults.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        // 4. Map everything together instantly (0 Database Queries here!)
        return activeCategories.stream().map(category -> {
            List<ProductSubCategory> subCategories = subCategoryMap.getOrDefault(category.getId(), List.of());
            long count = sellerCountMap.getOrDefault(category.getId(), 0L);
            return mapToCategoryDTO(category, subCategories, count);
        }).collect(Collectors.toList());
    }

    private CategoryDTO mapToCategoryDTO(ProductCategory category, List<ProductSubCategory> subCategories, long sellerCount) {
        List<SubCategoryDTO> subCategoryDTOs = subCategories.stream()
                .map(sub -> SubCategoryDTO.builder()
                        .id(sub.getId())
                        .name(sub.getName())
                        .slug(sub.getSlug())
                        .imageName(sub.getImageName())
                        .build())
                .collect(Collectors.toList());

        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .icon(category.getIcon())
                .imageName(category.getImageName())
                .subcategories(subCategoryDTOs)
                .sellerCount(sellerCount)
                .build();
    }
}