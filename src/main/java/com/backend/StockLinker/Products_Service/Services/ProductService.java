package com.backend.StockLinker.Products_Service.Services;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.Products_Service.Dto.SellerProductRequest;
import com.backend.StockLinker.Products_Service.Dto.MasterProductSearchDto;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.backend.StockLinker.Profile_Service.model.MasterProduct;
import com.backend.StockLinker.Profile_Service.model.SellerProduct;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.MasterProductRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final MasterProductRepository masterProductRepository;
    private final SellerProductRepository sellerProductRepository;
    private final BusinessProfileRepository businessProfileRepository;

    // Auditing
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @Transactional(readOnly = true)
    public List<MasterProductSearchDto> searchMasterProducts(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        List<MasterProduct> products = masterProductRepository.findTop10ByProductNameContainingIgnoreCase(query);

        return products.stream()
                .map(p -> new MasterProductSearchDto(p.getId(), p.getProductName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveBulkSellerProducts(List<SellerProductRequest> requests, String userId, HttpServletRequest httpRequest) {

        BusinessProfile businessProfile = businessProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Business Profile not found for user: " + userId));

        List<SellerProduct> sellerProductsToSave = requests.stream().map(req -> {
            MasterProduct masterProduct = masterProductRepository.findById(req.getMasterProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid Master Product ID: " + req.getMasterProductId()));

            return SellerProduct.builder()
                    .sellerId(userId)
                    .businessProfileId(businessProfile.getId())
                    .masterProduct(masterProduct)
                    .productName(masterProduct.getProductName())
                    .brand(req.getBrand())
                    .unit(req.getUnit())
                    .price(req.getPrice())
                    .minimumOrderQuantity(req.getMinimumOrderQuantity())
                    .bulkDealQuantity(req.getBulkDealQuantity())
                    .bulkDealPrice(req.getBulkDealPrice())
                    .availableStock(req.getAvailableStock())
                    .status("ACTIVE")
                    .build();
        }).collect(Collectors.toList());

        sellerProductRepository.saveAll(sellerProductsToSave);
        log.info("Successfully saved {} products for business profile {}", sellerProductsToSave.size(), businessProfile.getId());

        logAudit(userId, AuditAction.BULK_PRODUCTS_ADDED, "Added " + requests.size() + " bulk products to catalog", httpRequest);
    }

    private void logAudit(String userId, AuditAction action, String details, HttpServletRequest request) {
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.PRODUCT)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
    }
}