package com.backend.StockLinker.Products_Service.Controller;

import com.backend.StockLinker.Products_Service.Dto.SellerProductRequest;
import com.backend.StockLinker.Products_Service.Dto.MasterProductSearchDto;
import com.backend.StockLinker.Products_Service.Services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/master/search")
    public ResponseEntity<List<MasterProductSearchDto>> searchMasterProducts(@RequestParam String q) {
        return ResponseEntity.ok(productService.searchMasterProducts(q));
    }

    @PostMapping("/seller/bulk")
    public ResponseEntity<Map<String, String>> saveBulkSellerProducts(
            @RequestBody @Valid List<SellerProductRequest> requests,
            Authentication authentication) {

        String userId = authentication.getName();
        productService.saveBulkSellerProducts(requests, userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Products catalog updated successfully"
        ));
    }
}