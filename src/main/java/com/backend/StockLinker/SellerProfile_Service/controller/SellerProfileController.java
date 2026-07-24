package com.backend.StockLinker.SellerProfile_Service.controller;

import com.backend.StockLinker.SellerProfile_Service.dto.SellerProfileResponse;
import com.backend.StockLinker.SellerProfile_Service.service.SellerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/storefront")
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerProfileService storefrontService;

    // 1. Fetch Business Details
    @GetMapping("/{businessProfileId}/profile")
    public ResponseEntity<SellerProfileResponse> getStorefrontProfile(@PathVariable String businessProfileId) {
        return ResponseEntity.ok(storefrontService.getStorefrontProfile(businessProfileId));
    }

    // 2. Fetch Filtered Products
    @GetMapping("/{businessProfileId}/products")
    public ResponseEntity<List<SellerProfileResponse.StorefrontProductDto>> getStorefrontProducts(
            @PathVariable String businessProfileId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false, defaultValue = "all") String brand,
            @RequestParam(required = false, defaultValue = "none") String sortPrice) {

        return ResponseEntity.ok(storefrontService.getStorefrontProducts(businessProfileId, search, category, brand, sortPrice));
    }

    // 3. Fetch Dynamic Filters
    @GetMapping("/{businessProfileId}/filters")
    public ResponseEntity<Map<String, List<String>>> getStorefrontFilters(@PathVariable String businessProfileId) {
        return ResponseEntity.ok(storefrontService.getStorefrontFilters(businessProfileId));
    }
}