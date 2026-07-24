package com.backend.StockLinker.ProductCatagory_Service.service;

import com.backend.StockLinker.ProductCatagory_Service.dto.request.CategoryDTO;
import com.backend.StockLinker.ProductCatagory_Service.dto.request.SubCategoryDTO;
import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductCategory;
import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductSubCategory;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductCategoryRepository;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductSubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final ProductSubCategoryRepository subCategoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllActiveCategoriesWithSubcategories() {
        List<ProductCategory> activeCategories = categoryRepository.findByActiveTrue();

        return activeCategories.stream().map(category -> {
            List<ProductSubCategory> subCategories = subCategoryRepository.findByProductCategoryId(category.getId());
            return mapToCategoryDTO(category, subCategories);
        }).collect(Collectors.toList());
    }

    private CategoryDTO mapToCategoryDTO(ProductCategory category, List<ProductSubCategory> subCategories) {
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
                .build();
    }
}