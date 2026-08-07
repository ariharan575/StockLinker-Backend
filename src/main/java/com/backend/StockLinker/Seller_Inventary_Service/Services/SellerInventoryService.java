package com.backend.StockLinker.Seller_Inventary_Service.Services;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.Exception.customExceptions.ForbiddenException;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.Seller_Inventary_Service.Dto.ProductUpdateRequest;
import com.backend.StockLinker.Seller_Inventary_Service.Dto.SellerProductResponse;
import com.backend.StockLinker.Profile_Service.model.SellerProduct;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import com.backend.StockLinker.Seller_Inventary_Service.Repository.SellerProductSpecification;
import jakarta.persistence.criteria.JoinType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
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

    // Auditing
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @Transactional(readOnly = true)
    public List<SellerProductResponse> getFilteredInventory(
            String sellerId, String search, String category, String brand,
            String availability, String sortPrice, String sortStock, int page, int size) {

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

        return repository.findAll(spec, PageRequest.of(page, size, sort))
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SellerProductResponse updateProduct(String id, ProductUpdateRequest request, String sellerId, HttpServletRequest httpRequest) {
        SellerProduct product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("Unauthorized action: You do not own this product.");
        }

        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        product.setMinimumOrderQuantity(request.getMinimumOrderQuantity());
        product.setBulkDealQuantity(request.getBulkDealQuantity());
        product.setBulkDealPrice(request.getBulkDealPrice());
        product.setAvailableStock(request.getAvailableStock());

        SellerProduct updated = repository.save(product);

        logAudit(sellerId, AuditAction.PRODUCT_UPDATED, "Updated product: " + product.getProductName(), httpRequest);

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
    public void deleteProduct(String id, String sellerId, HttpServletRequest httpRequest) {
        SellerProduct product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("Unauthorized action");
        }

        String productName = product.getProductName();
        repository.delete(product);

        logAudit(sellerId, AuditAction.PRODUCT_DELETED, "Deleted product: " + productName, httpRequest);
    }

    @Transactional(readOnly = true)
    public String exportInventoryCsv(String sellerId, HttpServletRequest httpRequest) {
        List<SellerProduct> products = repository.findAll((root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("masterProduct", JoinType.LEFT)
                        .fetch("productSubCategory", JoinType.LEFT)
                        .fetch("productCategory", JoinType.LEFT);
            }
            return cb.equal(root.get("sellerId"), sellerId);
        });

        StringWriter writer = new StringWriter();
        PrintWriter csvWriter = new PrintWriter(writer);

        csvWriter.println("ID,Product Name,Brand,Category,Base Price,Bulk Deal Qty,Bulk Deal Price,Unit,Stock,MOQ,Status");
        for (SellerProduct p : products) {
            csvWriter.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    p.getId(), p.getProductName(), p.getBrand(),
                    p.getMasterProduct().getProductSubCategory().getProductCategory().getName(),
                    p.getPrice(), p.getBulkDealQuantity(), p.getBulkDealPrice(),
                    p.getUnit(), p.getAvailableStock(), p.getMinimumOrderQuantity(), p.getStatus());
        }

        logAudit(sellerId, AuditAction.INVENTORY_EXPORTED, "Exported inventory to CSV", httpRequest);

        return writer.toString();
    }

    private void logAudit(String userId, AuditAction action, String details, HttpServletRequest request) {
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.INVENTORY) // Or PRODUCT
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
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