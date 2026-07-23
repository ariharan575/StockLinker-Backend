package com.backend.StockLinker.ProfileService.service;

import com.backend.StockLinker.ProfileService.dto.request.SellerProductRequest;
import com.backend.StockLinker.ProfileService.dto.response.MasterProductSearchDto;
import com.backend.StockLinker.ProfileService.model.BusinessProfile;
import com.backend.StockLinker.ProfileService.model.MasterProduct;
import com.backend.StockLinker.ProfileService.model.SellerProduct;
import com.backend.StockLinker.ProfileService.repository.postgres.BusinessProfileRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.MasterProductRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.SellerProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public void saveBulkSellerProducts(List<SellerProductRequest> requests, String userId) {

        BusinessProfile businessProfile = businessProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Business Profile not found for user: " + userId));

        List<SellerProduct> sellerProductsToSave = requests.stream().map(req -> {
            MasterProduct masterProduct = masterProductRepository.findById(req.getMasterProductId())
                    .orElseThrow(() -> new RuntimeException("Invalid Master Product ID: " + req.getMasterProductId()));

            return SellerProduct.builder()
                    .sellerId(userId)
                    .businessProfileId(businessProfile.getId())
                    .masterProduct(masterProduct)
                    .productName(masterProduct.getProductName())
                    .brand(req.getBrand())
                    .unit(req.getUnit())
                    .packageSize(req.getPackageSize())
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
    }
}