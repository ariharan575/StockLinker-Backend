package com.backend.StockLinker.ProfileService.controller;

import com.backend.StockLinker.ProfileService.dto.request.GlobalEnquiryRequest;
import com.backend.StockLinker.ProfileService.dto.response.CompareDataResponse;
import com.backend.StockLinker.ProfileService.service.CompareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/compare")
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;

    @GetMapping("/{masterProductId}")
    public ResponseEntity<CompareDataResponse> getCompareData(
            @PathVariable String masterProductId,
            @RequestParam(defaultValue = "50") int qty) {
        return ResponseEntity.ok(compareService.getCompareData(masterProductId, qty));
    }

    @PostMapping("/enquiry")
    public ResponseEntity<Map<String, String>> submitGlobalEnquiry(
            @RequestBody @Valid GlobalEnquiryRequest request,
            Authentication authentication) {

        String buyerId = authentication.getName();
        compareService.submitGlobalEnquiry(request, buyerId);

        return ResponseEntity.ok(Map.of("message", "Negotiation request broadcasted successfully"));
    }
}