package com.backend.StockLinker.ProfileService.service;

import com.backend.StockLinker.ProfileService.dto.request.GlobalEnquiryRequest;
import com.backend.StockLinker.ProfileService.dto.response.CompareDataResponse;
import com.backend.StockLinker.ProfileService.model.BusinessProfile;
import com.backend.StockLinker.ProfileService.model.GlobalEnquiry;
import com.backend.StockLinker.ProfileService.model.MasterProduct;
import com.backend.StockLinker.ProfileService.model.SellerProduct;
import com.backend.StockLinker.ProfileService.repository.postgres.BusinessProfileRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.GlobalEnquiryRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.MasterProductRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.SellerProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompareService {

    private final SellerProductRepository sellerProductRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final MasterProductRepository masterProductRepository;
    private final GlobalEnquiryRepository globalEnquiryRepository;

    @Transactional(readOnly = true)
    public CompareDataResponse getCompareData(String masterProductId, int requestedQty) {
        MasterProduct masterProduct = masterProductRepository.findById(masterProductId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Fetch all seller products for this master product
        // Note: You might need to add this custom query to your repository if it's not there:
        // List<SellerProduct> findByMasterProductIdAndStatus(String masterProductId, String status);
        List<SellerProduct> allSellerProducts = sellerProductRepository.findAll().stream()
                .filter(p -> p.getMasterProduct().getId().equals(masterProductId) && "ACTIVE".equals(p.getStatus()))
                .collect(Collectors.toList());

        List<CompareDataResponse.SupplierRow> validSuppliers = new ArrayList<>();
        BigDecimal sumPrice = BigDecimal.ZERO;
        BigDecimal bestPrice = null;
        int validSupplierCount = 0;

        for (SellerProduct sp : allSellerProducts) {
            BusinessProfile bp = businessProfileRepository.findById(sp.getBusinessProfileId()).orElse(null);
            if (bp == null) continue;

            // Determine effective price for the requested quantity
            BigDecimal effectivePrice = sp.getPrice();
            if (sp.getBulkDealQuantity() != null && requestedQty >= sp.getBulkDealQuantity() && sp.getBulkDealPrice() != null) {
                effectivePrice = sp.getBulkDealPrice();
            }

            // Only consider for header math if stock is sufficient and MOQ is met
            if (sp.getAvailableStock() >= requestedQty && requestedQty >= sp.getMinimumOrderQuantity()) {
                sumPrice = sumPrice.add(effectivePrice);
                validSupplierCount++;
                if (bestPrice == null || effectivePrice.compareTo(bestPrice) < 0) {
                    bestPrice = effectivePrice;
                }
            }

            // Map supplier row data
            validSuppliers.add(CompareDataResponse.SupplierRow.builder()
                    .id(sp.getId())
                    .sellerId(sp.getSellerId())
                    .businessName(bp.getBusinessName())
                    .initials(bp.getBusinessName().substring(0, Math.min(2, bp.getBusinessName().length())).toUpperCase())
                    .verified("VERIFIED".equals(bp.getVerificationStatus()))
                    .location(bp.getBusinessAddress() != null ? bp.getBusinessAddress().getCity() : "N/A")
                    .rating(bp.getRating() != null ? bp.getRating() : 0.0)
                    .reviews(bp.getReviewCount() != null ? bp.getReviewCount() : 0)
                    .moq(sp.getMinimumOrderQuantity())
                    .moqUnit(sp.getUnit())
                    .moqPrice(sp.getPrice())
                    .calculatedUnitPrice(effectivePrice)
                    .bulkQty(sp.getBulkDealQuantity())
                    .bulkPrice(sp.getBulkDealPrice())
                    .availableStock(sp.getAvailableStock())
                    .trustScore(bp.getTrustScore() != null ? bp.getTrustScore() : 80)
                    .deliveryTime(bp.getDeliveryConfiguration() != null ? bp.getDeliveryConfiguration().getOperatingDays() : "2-3 Days")
                    .badge(bestPrice != null && bestPrice.equals(effectivePrice) ? "BEST DEAL" : null)
                    .build());
        }

        // Sort suppliers by calculated price
        validSuppliers.sort(Comparator.comparing(CompareDataResponse.SupplierRow::getCalculatedUnitPrice));

        // Header Math
        BigDecimal averagePrice = validSupplierCount > 0 ? sumPrice.divide(new BigDecimal(validSupplierCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (bestPrice == null) bestPrice = BigDecimal.ZERO;

        BigDecimal savingsPerUnit = averagePrice.subtract(bestPrice);
        if (savingsPerUnit.compareTo(BigDecimal.ZERO) < 0) savingsPerUnit = BigDecimal.ZERO;
        BigDecimal totalSavings = savingsPerUnit.multiply(new BigDecimal(requestedQty));

        CompareDataResponse.HeaderMetrics metrics = CompareDataResponse.HeaderMetrics.builder()
                .productName(masterProduct.getProductName())
                .supplierCount(validSuppliers.size())
                .averagePrice(averagePrice)
                .bestPrice(bestPrice)
                .savingsPerUnit(savingsPerUnit)
                .totalSavings(totalSavings)
                .build();

        // AI Volume Pricing Intelligence (The Boomathi Logic)
        // Find sellers who offer a bulk deal at a qty > requestedQty, where bulk price < current best price
        List<CompareDataResponse.AiVolumeDeal> aiDeals = new ArrayList<>();
        int rank = 1;

        List<SellerProduct> potentialBulkSellers = allSellerProducts.stream()
                .filter(sp -> sp.getBulkDealQuantity() != null
                        && sp.getBulkDealQuantity() > requestedQty
                        && sp.getBulkDealPrice() != null
                        && sp.getAvailableStock() >= sp.getBulkDealQuantity())
                .sorted(Comparator.comparing(SellerProduct::getBulkDealPrice))
                .collect(Collectors.toList());

        for (SellerProduct sp : potentialBulkSellers) {
            if (aiDeals.size() >= 3) break; // Top 3 only

            BusinessProfile bp = businessProfileRepository.findById(sp.getBusinessProfileId()).orElse(null);
            if (bp == null) continue;

            BigDecimal bulkSavingsVsAvg = averagePrice.subtract(sp.getBulkDealPrice());
            if (bulkSavingsVsAvg.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal totalBulkSavings = bulkSavingsVsAvg.multiply(new BigDecimal(sp.getBulkDealQuantity()));

                aiDeals.add(CompareDataResponse.AiVolumeDeal.builder()
                        .rank(rank++)
                        .sellerId(sp.getSellerId())
                        .businessName(bp.getBusinessName())
                        .location(bp.getBusinessAddress() != null ? bp.getBusinessAddress().getCity() : "N/A")
                        .rating(bp.getRating() != null ? bp.getRating() : 0.0)
                        .reviewCount(bp.getReviewCount() != null ? bp.getReviewCount() : 0)
                        .requiredQuantity(sp.getBulkDealQuantity())
                        .unitPrice(sp.getBulkDealPrice())
                        .totalSavingsVsMarket(totalBulkSavings)
                        .build());
            }
        }

        return CompareDataResponse.builder()
                .headerMetrics(metrics)
                .aiVolumeDeals(aiDeals)
                .suppliers(validSuppliers)
                .build();
    }

    @Transactional
    public void submitGlobalEnquiry(GlobalEnquiryRequest request, String buyerId) {
        MasterProduct product = masterProductRepository.findById(request.getMasterProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        GlobalEnquiry enquiry = GlobalEnquiry.builder()
                .buyerId(buyerId)
                .masterProduct(product)
                .requestedQuantity(request.getRequestedQuantity())
                .targetPrice(request.getTargetPrice())
                .message(request.getMessage())
                .status("OPEN")
                .build();

        globalEnquiryRepository.save(enquiry);
    }
}