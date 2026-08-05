package com.backend.StockLinker.Global_Request_Service.controller;

import com.backend.StockLinker.Global_Request_Service.Dto.EnquiryResponseDto;
import com.backend.StockLinker.Global_Request_Service.service.GlobalEnquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/enquiries")
@RequiredArgsConstructor
public class GlobalEnquiryController {

    private final GlobalEnquiryService globalEnquiryService;

    @GetMapping("/relevant")
    public ResponseEntity<List<EnquiryResponseDto>> getRelevantEnquiries(Authentication authentication) {
        return ResponseEntity.ok(globalEnquiryService.getRelevantEnquiries(authentication.getName()));
    }

    @PostMapping("/{enquiryId}/accept")
    public ResponseEntity<Map<String, String>> acceptEnquiryAsOrder(
            @PathVariable String enquiryId,
            Authentication authentication) {
        globalEnquiryService.acceptEnquiryAsOrder(enquiryId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Order accepted and is now processing."));
    }
}