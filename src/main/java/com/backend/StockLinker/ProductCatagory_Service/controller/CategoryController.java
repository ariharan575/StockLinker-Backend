package com.backend.StockLinker.ProductCatagory_Service.controller;

import com.backend.StockLinker.ProductCatagory_Service.dto.request.CategoryDTO;
import com.backend.StockLinker.ProductCatagory_Service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getCategories() {
        List<CategoryDTO> categories = categoryService.getAllActiveCategoriesWithSubcategories();
        return ResponseEntity.ok(categories);
    }
}