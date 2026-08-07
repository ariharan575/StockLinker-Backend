package com.backend.StockLinker.Seller_Inventary_Service.Controller;

import com.backend.StockLinker.Seller_Inventary_Service.Dto.ProductUpdateRequest;
import com.backend.StockLinker.Seller_Inventary_Service.Dto.SellerProductResponse;
import com.backend.StockLinker.Seller_Inventary_Service.Services.SellerInventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return ResponseEntity.ok(inventoryService.getFilteredInventory(
                authentication.getName(), search, category, brand, availability, sortPrice, sortStock, page, size));
    }

    @GetMapping("/filters")
    public ResponseEntity<Map<String, List<String>>> getFilterOptions(Authentication authentication) {
        return ResponseEntity.ok(inventoryService.getFilterOptions(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SellerProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(inventoryService.updateProduct(id, request, authentication.getName(), httpRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable String id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        inventoryService.deleteProduct(id, authentication.getName(), httpRequest);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(Authentication authentication, HttpServletRequest httpRequest) {
        String csvData = inventoryService.exportInventoryCsv(authentication.getName(), httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData.getBytes());
    }
}