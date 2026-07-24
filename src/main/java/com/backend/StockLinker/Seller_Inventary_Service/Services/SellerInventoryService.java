package com.backend.StockLinker.Seller_Inventary_Service.Services;

import com.backend.StockLinker.Seller_Inventary_Service.Dto.ProductUpdateRequest;
import com.backend.StockLinker.Seller_Inventary_Service.Dto.SellerProductResponse;
import com.backend.StockLinker.Profile_Service.model.SellerProduct;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import com.backend.StockLinker.Seller_Inventary_Service.Repository.SellerProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerInventoryService {

    private final SellerProductRepository repository;

    @Transactional(readOnly = true)
    public List<SellerProductResponse> getFilteredInventory(
            String sellerId, String search, String category, String brand,
            String availability, String sortPrice, String sortStock) {

        Specification<SellerProduct> spec = SellerProductSpecification.getFilteredProducts(
                sellerId, search, category, brand, availability);

        Sort sort = Sort.unsorted();
        if (sortPrice != null && !sortPrice.equals("none")) {
            sort = sortPrice.equals("asc") ? Sort.by("price").ascending() : Sort.by("price").descending();
        } else if (sortStock != null && !sortStock.equals("none")) {
            sort = sortStock.equals("asc") ? Sort.by("availableStock").ascending() : Sort.by("availableStock").descending();
        } else {
            sort = Sort.by("updatedAt").descending();
        }

        return repository.findAll(spec, sort).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // NEW: Update Method
    @Transactional
    public SellerProductResponse updateProduct(String id, ProductUpdateRequest request, String sellerId) {
        SellerProduct product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Unauthorized action: You do not own this product.");
        }

        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        product.setMinimumOrderQuantity(request.getMinimumOrderQuantity());
        product.setBulkDealQuantity(request.getBulkDealQuantity());
        product.setBulkDealPrice(request.getBulkDealPrice());
        product.setAvailableStock(request.getAvailableStock());

        SellerProduct updated = repository.save(product);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getFilterOptions(String sellerId) {
        return Map.of(
                "brands", repository.findDistinctBrandsBySellerId(sellerId),
                "categories", repository.findDistinctCategoriesBySellerId(sellerId)
        );
    }

    @Transactional
    public void deleteProduct(String id, String sellerId) {
        SellerProduct product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Unauthorized action");
        }
        repository.delete(product);
    }

    @Transactional(readOnly = true)
    public String exportInventoryCsv(String sellerId) {
        List<SellerProduct> products = repository.findAll((root, query, cb) -> cb.equal(root.get("sellerId"), sellerId));
        StringWriter writer = new StringWriter();
        PrintWriter csvWriter = new PrintWriter(writer);

        // Updated CSV Headers
        csvWriter.println("ID,Product Name,Brand,Category,Base Price,Bulk Deal Qty,Bulk Deal Price,Unit,Stock,MOQ,Status");
        for (SellerProduct p : products) {
            csvWriter.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    p.getId(), p.getProductName(), p.getBrand(),
                    p.getMasterProduct().getProductSubCategory().getProductCategory().getName(),
                    p.getPrice(), p.getBulkDealQuantity(), p.getBulkDealPrice(),
                    p.getUnit(), p.getAvailableStock(), p.getMinimumOrderQuantity(), p.getStatus());
        }
        return writer.toString();
    }

    private SellerProductResponse mapToResponse(SellerProduct p) {
        return SellerProductResponse.builder()
                .id(p.getId())
                .productName(p.getProductName())
                .brand(p.getBrand())
                .category(p.getMasterProduct().getProductSubCategory().getProductCategory().getName())
                .unit(p.getUnit())
                .price(p.getPrice())
                .minimumOrderQuantity(p.getMinimumOrderQuantity())
                .bulkDealQuantity(p.getBulkDealQuantity())
                .bulkDealPrice(p.getBulkDealPrice())
                .availableStock(p.getAvailableStock())
                .status(p.getStatus())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}