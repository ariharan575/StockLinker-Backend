package com.backend.StockLinker.SellerProfile_Service.controller;

import com.backend.StockLinker.SellerProfile_Service.dto.SellerProfileResponse;
import com.backend.StockLinker.SellerProfile_Service.service.SellerProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @GetMapping("/{businessProfileId}/profile")
    public ResponseEntity<SellerProfileResponse> getStorefrontProfile(
            @PathVariable String businessProfileId,
            Authentication auth,
            HttpServletRequest request) {
        return ResponseEntity.ok(storefrontService.getStorefrontProfile(businessProfileId, auth.getName(), request));
    }

    @GetMapping("/{businessProfileId}/products")
    public ResponseEntity<Page<SellerProfileResponse.StorefrontProductDto>> getStorefrontProducts(
            @PathVariable String businessProfileId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false, defaultValue = "all") String brand,
            @RequestParam(required = false, defaultValue = "none") String sortPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Now returning a Page wrapper instead of a flat List for proper slicing
        return ResponseEntity.ok(storefrontService.getStorefrontProducts(businessProfileId, search, category, brand, sortPrice, page, size));
    }

    @GetMapping("/{businessProfileId}/filters")
    public ResponseEntity<Map<String, List<String>>> getStorefrontFilters(@PathVariable String businessProfileId) {
        return ResponseEntity.ok(storefrontService.getStorefrontFilters(businessProfileId));
    }

    @PostMapping("/{businessProfileId}/rate")
    public ResponseEntity<Map<String, String>> submitPartnerRating(
            @PathVariable String businessProfileId,
            @RequestBody Map<String, Integer> payload,
            Authentication auth,
            HttpServletRequest request) {
        storefrontService.submitCommunityRating(businessProfileId, payload.get("rating"), auth.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Rating submitted successfully"));
    }
}