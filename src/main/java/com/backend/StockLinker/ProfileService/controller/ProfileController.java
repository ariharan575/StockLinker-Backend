package com.backend.StockLinker.ProfileService.controller;

import com.backend.StockLinker.ProfileService.dto.request.ProfileDTO.*;
import com.backend.StockLinker.ProfileService.dto.response.ApiResponse;
import com.backend.StockLinker.ProfileService.service.ProfileService;
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
    public ResponseEntity<ApiResponse<String>> updateAccount(@RequestBody AccountUpdateRequest request, Principal principal) {
        profileService.updateAccount(getUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success("Account updated successfully", null));
    }

    @PutMapping("/business")
    public ResponseEntity<ApiResponse<String>> updateBusiness(@RequestBody BusinessUpdateRequest request, Principal principal) {
        profileService.updateBusiness(getUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success("Business details updated successfully", null));
    }

    @PutMapping("/delivery")
    public ResponseEntity<ApiResponse<String>> updateDelivery(@RequestBody DeliveryUpdateRequest request, Principal principal) {
        profileService.updateDelivery(getUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success("Delivery details updated successfully", null));
    }
}