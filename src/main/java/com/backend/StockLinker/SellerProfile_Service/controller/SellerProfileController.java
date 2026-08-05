package com.backend.StockLinker.SellerProfile_Service.controller;

import com.backend.StockLinker.SellerProfile_Service.dto.SellerProfileResponse;
import com.backend.StockLinker.SellerProfile_Service.service.SellerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/storefront")
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerProfileService storefrontService;

    // 1. Fetch World-Class Business Details (Resolved Categories & Structured Address)
    @GetMapping("/{businessProfileId}/profile")
    public ResponseEntity<SellerProfileResponse> getStorefrontProfile(
            @PathVariable String businessProfileId,
            Authentication auth) { // <-- Add Authentication
        return ResponseEntity.ok(storefrontService.getStorefrontProfile(businessProfileId, auth.getName()));
    }

    // 2. Fetch Filtered Products (Preserved existing logic perfectly)
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

    // 4. Community Rating Engine Submission
    @PostMapping("/{businessProfileId}/rate")
    public ResponseEntity<Map<String, String>> submitPartnerRating(
            @PathVariable String businessProfileId,
            @RequestBody Map<String, Integer> payload,
            Authentication auth) {
        storefrontService.submitCommunityRating(businessProfileId, payload.get("rating"), auth.getName());
        return ResponseEntity.ok(Map.of("message", "Rating submitted successfully"));
    }
}