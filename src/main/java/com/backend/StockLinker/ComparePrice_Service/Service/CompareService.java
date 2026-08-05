package com.backend.StockLinker.ComparePrice_Service.Service;

import com.backend.StockLinker.ComparePrice_Service.dto.*;
import com.backend.StockLinker.Exception.BaseException;
import com.backend.StockLinker.Exception.ErrorCode;
import com.backend.StockLinker.Global_Request_Service.Dto.GlobalEnquiryRequest;
import com.backend.StockLinker.Global_Request_Service.Entity.GlobalEnquiry;
import com.backend.StockLinker.Global_Request_Service.Repository.GlobalEnquiryRepository;
import com.backend.StockLinker.Products_Service.Dto.*;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.backend.StockLinker.Profile_Service.model.MasterProduct;
import com.backend.StockLinker.Profile_Service.model.SellerProduct;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.MasterProductRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
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
    public ComparePageResponseDto getCompareData(String masterProductId, int requestedQty) {

        List<SellerProduct> allSellers = sellerProductRepository.findActiveByMasterProductId(masterProductId);
        if (allSellers.isEmpty()) {
            // FIX: Replaced RuntimeException with your custom BaseException
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "No active sellers found for this product.");
        }

        MasterProduct masterProduct = allSellers.get(0).getMasterProduct();
        String categoryName = masterProduct.getProductSubCategory().getProductCategory().getName();

        int absoluteMinMoq = allSellers.stream().mapToInt(SellerProduct::getMinimumOrderQuantity).min().orElse(1);
        int maxAvailableStock = allSellers.stream().mapToInt(SellerProduct::getAvailableStock).max().orElse(0);
        MarketBoundariesDto boundaries = new MarketBoundariesDto(absoluteMinMoq, maxAvailableStock);

        List<SellerProduct> validSellers = allSellers.stream()
                .filter(sp -> requestedQty >= sp.getMinimumOrderQuantity() && requestedQty <= sp.getAvailableStock())
                .collect(Collectors.toList());

        if (validSellers.isEmpty()) {
            return ComparePageResponseDto.builder().marketBoundaries(boundaries).build();
        }

        BigDecimal totalMarketPriceSum = BigDecimal.ZERO;
        List<SupplierDto> supplierDtos = new ArrayList<>();

        for (SellerProduct sp : validSellers) {
            BusinessProfile bp = businessProfileRepository.findById(sp.getBusinessProfileId()).orElse(null);
            if (bp == null) continue;

            BigDecimal calculatedTotalPrice;
            BigDecimal standardTotalPrice = sp.getPrice().multiply(BigDecimal.valueOf(requestedQty));

            BigDecimal potentialBulkSavings = BigDecimal.ZERO;
            if (sp.getBulkDealQuantity() != null && sp.getBulkDealPrice() != null && sp.getBulkDealQuantity() > 0) {
                BigDecimal baseCostForBulkQty = sp.getPrice().multiply(BigDecimal.valueOf(sp.getBulkDealQuantity()));
                potentialBulkSavings = baseCostForBulkQty.subtract(sp.getBulkDealPrice());
            }

            if (sp.getBulkDealQuantity() != null && sp.getBulkDealPrice() != null && sp.getBulkDealQuantity() > 0) {
                int bulkLots = requestedQty / sp.getBulkDealQuantity();
                int remainder = requestedQty % sp.getBulkDealQuantity();
                BigDecimal bulkLotsCost = sp.getBulkDealPrice().multiply(BigDecimal.valueOf(bulkLots));
                BigDecimal remainderCost = sp.getPrice().multiply(BigDecimal.valueOf(remainder));
                calculatedTotalPrice = bulkLotsCost.add(remainderCost);
            } else {
                calculatedTotalPrice = standardTotalPrice;
            }

            totalMarketPriceSum = totalMarketPriceSum.add(calculatedTotalPrice);

            supplierDtos.add(SupplierDto.builder()
                    .id(sp.getId())
                    .businessProfileId(bp.getId())
                    .businessName(bp.getBusinessName())
                    .initials(bp.getBusinessName().substring(0, Math.min(bp.getBusinessName().length(), 2)).toUpperCase())
                    .locationDistrict(bp.getBusinessAddress() != null ? bp.getBusinessAddress().getDistrict() : "Unknown")
                    .moq(sp.getMinimumOrderQuantity())
                    .unit(sp.getUnit())
                    .basePricePerUnit(sp.getPrice())
                    .requestedQuantity(requestedQty)
                    .calculatedTotalPrice(calculatedTotalPrice)
                    .bulkQty(sp.getBulkDealQuantity())
                    .bulkTotalPrice(sp.getBulkDealPrice())
                    .bulkSavingsAmount(potentialBulkSavings)
                    .rating(bp.getRating() != null ? bp.getRating() : 0.0)
                    .trustScore(bp.getTrustScore() != null ? bp.getTrustScore() : 80)
                    .availableStock(sp.getAvailableStock())
                    .verified("VERIFIED".equalsIgnoreCase(bp.getVerificationStatus()))
                    .build());
        }

        supplierDtos.sort(Comparator.comparing(SupplierDto::getCalculatedTotalPrice));

        BigDecimal marketAverageTotal = totalMarketPriceSum.divide(BigDecimal.valueOf(supplierDtos.size()), 2, RoundingMode.HALF_UP);
        BigDecimal bestPriceTotal = supplierDtos.get(0).getCalculatedTotalPrice();

        BigDecimal totalSavings = marketAverageTotal.subtract(bestPriceTotal);
        if (totalSavings.compareTo(BigDecimal.ZERO) < 0) {
            totalSavings = BigDecimal.ZERO;
        }

        List<SupplierMatrixDto> matrixDtos = new ArrayList<>();
        int limit = Math.min(3, supplierDtos.size());

        for (int i = 0; i < limit; i++) {
            SupplierDto dto = supplierDtos.get(i);
            BigDecimal diff = dto.getCalculatedTotalPrice().subtract(marketAverageTotal);
            int pct = 0;
            String status;

            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                status = "CHEAPER";
                pct = diff.abs().multiply(BigDecimal.valueOf(100)).divide(marketAverageTotal, 0, RoundingMode.HALF_UP).intValue();
            } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                status = "HIGHER";
                pct = diff.multiply(BigDecimal.valueOf(100)).divide(marketAverageTotal, 0, RoundingMode.HALF_UP).intValue();
            } else {
                status = "NEUTRAL";
            }

            matrixDtos.add(SupplierMatrixDto.builder()
                    .businessName(dto.getBusinessName())
                    .calculatedTotalPrice(dto.getCalculatedTotalPrice())
                    .differenceFromAverage(diff.abs())
                    .percentageDifference(Math.min(100, pct))
                    .comparisonStatus(status)
                    .build());
        }

        HeaderMetricsDto headerMetrics = HeaderMetricsDto.builder()
                .masterProductId(masterProduct.getId())
                .productName(masterProduct.getProductName())
                .category(categoryName)
                .supplierCount(supplierDtos.size())
                .bestPriceTotal(bestPriceTotal)
                .marketAverageTotal(marketAverageTotal)
                .totalSavings(totalSavings)
                .top3Matrix(matrixDtos)
                .build();

        List<VolumeUpsellDto> aiDeals = generateAiVolumeDeals(allSellers, requestedQty, marketAverageTotal);

        return ComparePageResponseDto.builder()
                .headerMetrics(headerMetrics)
                .marketBoundaries(boundaries)
                .aiVolumeDeals(aiDeals)
                .suppliers(supplierDtos)
                .build();
    }


    @Transactional(readOnly = true)
    public ComparePageResponseDto getDashboardHighlight() {
        List<MasterProduct> allProducts = masterProductRepository.findAll();
        if (allProducts.isEmpty()) {
            return ComparePageResponseDto.builder().build();
        }

        List<MasterProduct> productsWithSellers = new ArrayList<>();
        for (MasterProduct mp : allProducts) {
            if (!sellerProductRepository.findActiveByMasterProductId(mp.getId()).isEmpty()) {
                productsWithSellers.add(mp);
            }
        }

        if (productsWithSellers.isEmpty()) {
            return ComparePageResponseDto.builder().build();
        }

        productsWithSellers.sort(java.util.Comparator.comparing(MasterProduct::getId));

        int dayOfYear = java.time.LocalDate.now().getDayOfYear();
        int rotationIndex = dayOfYear % productsWithSellers.size();

        MasterProduct dailyProduct = productsWithSellers.get(rotationIndex);

        List<SellerProduct> sellers = sellerProductRepository.findActiveByMasterProductId(dailyProduct.getId());
        int safeQty = sellers.stream()
                .mapToInt(SellerProduct::getMinimumOrderQuantity)
                .min()
                .orElse(1);

        return getCompareData(dailyProduct.getId(), safeQty);
    }

    @Transactional(readOnly = true)
    public List<FeaturedComparisonDto> getDailyFeaturedComparisons() {
        List<MasterProduct> allProducts = masterProductRepository.findAll();

        List<MasterProduct> productsWithSellers = new ArrayList<>();
        for (MasterProduct mp : allProducts) {
            if (!sellerProductRepository.findActiveByMasterProductId(mp.getId()).isEmpty()) {
                productsWithSellers.add(mp);
            }
        }

        if (productsWithSellers.isEmpty()) {
            return new ArrayList<>();
        }

        productsWithSellers.sort(java.util.Comparator.comparing(MasterProduct::getId));
        int dayOfYear = java.time.LocalDate.now().getDayOfYear();

        List<FeaturedComparisonDto> result = new ArrayList<>();
        int startIndex = dayOfYear % productsWithSellers.size();

        for (int i = 0; i < Math.min(4, productsWithSellers.size()); i++) {
            int currentIndex = (startIndex + i) % productsWithSellers.size();
            MasterProduct mp = productsWithSellers.get(currentIndex);
            List<SellerProduct> sellers = sellerProductRepository.findActiveByMasterProductId(mp.getId());

            sellers.sort(java.util.Comparator.comparing(SellerProduct::getPrice));

            List<FeaturedComparisonDto.FeaturedSupplierDto> supplierDtos = new ArrayList<>();
            int limit = Math.min(3, sellers.size());
            for (int j = 0; j < limit; j++) {
                SellerProduct sp = sellers.get(j);
                BusinessProfile bp = businessProfileRepository.findById(sp.getBusinessProfileId()).orElse(null);
                if (bp != null) {
                    supplierDtos.add(FeaturedComparisonDto.FeaturedSupplierDto.builder()
                            .name(bp.getBusinessName())
                            .price(sp.getPrice())
                            .isBest(j == 0)
                            .build());
                }
            }

            String brand = sellers.get(0).getBrand();
            result.add(FeaturedComparisonDto.builder()
                    .masterProductId(mp.getId())
                    .productName(mp.getProductName())
                    .brand(brand != null ? brand : "Generic")
                    .suppliers(supplierDtos)
                    .build());
        }
        return result;
    }

    private List<VolumeUpsellDto> generateAiVolumeDeals(List<SellerProduct> allSellers, int requestedQty, BigDecimal marketAverageTotal) {
        List<VolumeUpsellDto> upsells = new ArrayList<>();

        List<SellerProduct> bulkDealers = allSellers.stream()
                .filter(sp -> sp.getBulkDealQuantity() != null && sp.getBulkDealQuantity() > requestedQty)
                .collect(Collectors.toList());

        for (SellerProduct sp : bulkDealers) {
            BusinessProfile bp = businessProfileRepository.findById(sp.getBusinessProfileId()).orElse(null);
            if (bp == null) continue;

            BigDecimal extrapolatedAverage = marketAverageTotal.divide(BigDecimal.valueOf(requestedQty), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(sp.getBulkDealQuantity()));

            BigDecimal savings = extrapolatedAverage.subtract(sp.getBulkDealPrice());
            int extraQty = sp.getBulkDealQuantity() - requestedQty;

            if (savings.compareTo(BigDecimal.ZERO) > 0) {
                upsells.add(VolumeUpsellDto.builder()
                        .businessProfileId(bp.getId())
                        .businessName(bp.getBusinessName())
                        .location(bp.getBusinessAddress() != null ? bp.getBusinessAddress().getDistrict() : "")
                        .rating(bp.getRating() != null ? bp.getRating() : 0.0)
                        .requiredQuantity(sp.getBulkDealQuantity())
                        .bulkTotalPrice(sp.getBulkDealPrice())
                        .totalSavingsVsMarket(savings)
                        .extraQuantityGained(extraQty)
                        .build());
            }
        }

        upsells.sort(Comparator.comparing(VolumeUpsellDto::getTotalSavingsVsMarket).reversed());
        for (int i = 0; i < Math.min(upsells.size(), 3); i++) {
            upsells.get(i).setRank(i + 1);
        }
        return upsells.stream().limit(3).collect(Collectors.toList());
    }

    @Transactional
    public void submitEnquiry(GlobalEnquiryRequest request, String buyerId) {
        MasterProduct masterProduct = masterProductRepository.findById(request.getMasterProductId())
                // FIX: Replaced RuntimeException with your custom BaseException here as well
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));

        GlobalEnquiry enquiry = GlobalEnquiry.builder()
                .buyerId(buyerId)
                .masterProduct(masterProduct)
                .requestedQuantity(request.getRequestedQuantity())
                .targetPrice(request.getTargetPrice())
                .message(request.getMessage())
                .status("OPEN")
                .build();

        globalEnquiryRepository.save(enquiry);
    }
}