package com.backend.StockLinker.ProfileService.controller;

import com.backend.StockLinker.ProfileService.dto.request.AddressInfoRequestDto;
import com.backend.StockLinker.ProfileService.dto.request.BusinessInfoRequestDto;
import com.backend.StockLinker.ProfileService.dto.request.MarketplaceInfoRequestDto;
import com.backend.StockLinker.ProfileService.dto.response.ApiResponse;
import com.backend.StockLinker.ProfileService.dto.response.CategoryResponseDto;
import com.backend.StockLinker.ProfileService.service.OnboardingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    // --- NEW: Fetch Categories for Frontend ---
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getActiveCategories() {
        return ResponseEntity.ok(ApiResponse.success("Categories fetched successfully", onboardingService.getActiveCategories()));
    }

    @PostMapping("/step1/business")
    public ResponseEntity<ApiResponse<String>> saveBusinessInfo(
            @Valid @RequestBody BusinessInfoRequestDto requestDto,
            HttpServletRequest request) {

        onboardingService.saveBusinessInfo(requestDto, request);
        return ResponseEntity.ok(ApiResponse.success("Business info saved successfully.", "Success"));
    }

    @PostMapping("/step2/address")
    public ResponseEntity<ApiResponse<String>> saveAddressInfo(
            @Valid @RequestBody AddressInfoRequestDto requestDto,
            HttpServletRequest request) {

        onboardingService.saveAddressInfo(requestDto, request);
        return ResponseEntity.ok(ApiResponse.success("Address info saved successfully.", "Success"));
    }

    @PostMapping("/step3/marketplace")
    public ResponseEntity<ApiResponse<String>> completeMarketplaceInfo(
            @Valid @RequestBody MarketplaceInfoRequestDto requestDto,
            HttpServletRequest request) {

        onboardingService.saveMarketplaceInfo(requestDto, request);
        return ResponseEntity.ok(ApiResponse.success("Onboarding completed successfully. Account is now ACTIVE.", "Success"));
    }
}