package com.backend.StockLinker.ProfileService.controller;

import com.backend.StockLinker.ProfileService.dto.response.SellerProductResponse;
import com.backend.StockLinker.ProfileService.service.SellerInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class SellerInventoryController {

    private final SellerInventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<SellerProductResponse>> getInventory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false, defaultValue = "all") String brand,
            @RequestParam(required = false, defaultValue = "all") String availability,
            @RequestParam(required = false, defaultValue = "none") String sortPrice,
            @RequestParam(required = false, defaultValue = "none") String sortStock,
            Authentication authentication) {

        return ResponseEntity.ok(inventoryService.getFilteredInventory(
                authentication.getName(), search, category, brand, availability, sortPrice, sortStock));
    }

    @GetMapping("/filters")
    public ResponseEntity<Map<String, List<String>>> getFilterOptions(Authentication authentication) {
        return ResponseEntity.ok(inventoryService.getFilterOptions(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id, Authentication authentication) {
        inventoryService.deleteProduct(id, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(Authentication authentication) {
        String csvData = inventoryService.exportInventoryCsv(authentication.getName());
        byte[] output = csvData.getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(output);
    }
}