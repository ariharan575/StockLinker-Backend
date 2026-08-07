package com.backend.StockLinker.Profile_Service.controller;

import com.backend.StockLinker.Onboading_Service.dto.response.ApiResponse;
import com.backend.StockLinker.Profile_Service.service.ProfileService;
import com.backend.StockLinker.Profile_Service.Dto.ProfileDTO.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    private String getUserId(Principal principal) {
        return principal != null ? principal.getName() : "test-user-id";
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FullProfileResponse>> getProfile(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profileService.getProfile(getUserId(principal))));
    }

    @PutMapping("/account")
    public ResponseEntity<ApiResponse<String>> updateAccount(@RequestBody AccountUpdateRequest request, Principal principal, HttpServletRequest httpRequest) {
        profileService.updateAccount(getUserId(principal), request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Account updated successfully", null));
    }

    @PutMapping("/business")
    public ResponseEntity<ApiResponse<String>> updateBusiness(@RequestBody BusinessUpdateRequest request, Principal principal, HttpServletRequest httpRequest) {
        profileService.updateBusiness(getUserId(principal), request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Business details updated successfully", null));
    }

    @PutMapping("/store")
    public ResponseEntity<ApiResponse<String>> updateStore(@RequestBody StoreUpdateRequest request, Principal principal, HttpServletRequest httpRequest) {
        profileService.updateStore(getUserId(principal), request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Store address updated successfully", null));
    }

    @PutMapping("/delivery-insights")
    public ResponseEntity<ApiResponse<String>> updateDeliveryAndInsights(@RequestBody DeliveryInsightsUpdateRequest request, Principal principal, HttpServletRequest httpRequest) {
        profileService.updateDeliveryAndInsights(getUserId(principal), request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Delivery & Insights updated successfully", null));
    }
}