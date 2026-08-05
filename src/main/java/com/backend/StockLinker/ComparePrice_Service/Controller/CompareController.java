package com.backend.StockLinker.ComparePrice_Service.Controller;

import com.backend.StockLinker.ComparePrice_Service.Service.CompareService;
import com.backend.StockLinker.ComparePrice_Service.dto.ComparePageResponseDto;
import com.backend.StockLinker.Global_Request_Service.Dto.GlobalEnquiryRequest;
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
    public ResponseEntity<ComparePageResponseDto> getCompareData(
            @PathVariable String masterProductId,
            @RequestParam Integer qty) {

        ComparePageResponseDto response = compareService.getCompareData(masterProductId, qty);
        return ResponseEntity.ok(response);
    }

    // Add this to CompareController.java

    @GetMapping("/featured")
    public ResponseEntity<java.util.List<com.backend.StockLinker.ComparePrice_Service.dto.FeaturedComparisonDto>> getFeaturedComparisons() {
        return ResponseEntity.ok(compareService.getDailyFeaturedComparisons());
    }

    // ADD THIS ENDPOINT TO CompareController.java

    @GetMapping("/dashboard-highlight")
    public ResponseEntity<ComparePageResponseDto> getDashboardHighlight() {
        return ResponseEntity.ok(compareService.getDashboardHighlight());
    }

    @PostMapping("/enquiry")
    public ResponseEntity<Map<String, String>> submitEnquiry(
            @RequestBody @Valid GlobalEnquiryRequest request,
            Authentication authentication) {

        // Validating user authentication guarantees the ID is passed securely
        String buyerId = authentication.getName();
        compareService.submitEnquiry(request, buyerId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Negotiation Request Broadcasted Successfully"
        ));
    }
}