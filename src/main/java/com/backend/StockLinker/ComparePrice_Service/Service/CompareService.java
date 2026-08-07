package com.backend.StockLinker.ComparePrice_Service.Service;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.ComparePrice_Service.dto.*;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.Global_Request_Service.Dto.GlobalEnquiryRequest;
import com.backend.StockLinker.Global_Request_Service.Entity.GlobalEnquiry;
import com.backend.StockLinker.Global_Request_Service.Repository.GlobalEnquiryRepository;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.backend.StockLinker.Profile_Service.model.MasterProduct;
import com.backend.StockLinker.Profile_Service.model.SellerProduct;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.MasterProductRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompareService {

    private final SellerProductRepository sellerProductRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final MasterProductRepository masterProductRepository;
    private final GlobalEnquiryRepository globalEnquiryRepository;

    // Auditing
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @Transactional(readOnly = true)
    public ComparePageResponseDto getCompareData(String masterProductId, int requestedQty) {

        List<SellerProduct> allSellers = sellerProductRepository.findActiveByMasterProductId(masterProductId);
        if (allSellers.isEmpty()) {
            throw new ResourceNotFoundException("No active sellers found for this product.");
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

        // Bulk fetch all Business Profiles in 1 query to prevent N+1 loop
        List<String> profileIds = validSellers.stream().map(SellerProduct::getBusinessProfileId).distinct().collect(Collectors.toList());
        Map<String, BusinessProfile> profileMap = businessProfileRepository.findAllById(profileIds)
                .stream().collect(Collectors.toMap(BusinessProfile::getId, p -> p));

        BigDecimal totalMarketPriceSum = BigDecimal.ZERO;
        List<SupplierDto> supplierDtos = new ArrayList<>();

        for (SellerProduct sp : validSellers) {
            BusinessProfile bp = profileMap.get(sp.getBusinessProfileId());
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

        List<VolumeUpsellDto> aiDeals = generateAiVolumeDeals(allSellers, requestedQty, marketAverageTotal, profileMap);

        return ComparePageResponseDto.builder()
                .headerMetrics(headerMetrics)
                .marketBoundaries(boundaries)
                .aiVolumeDeals(aiDeals)
                .suppliers(supplierDtos)
                .build();
    }


    @Transactional(readOnly = true)
    public ComparePageResponseDto getDashboardHighlight() {
        long totalActiveProducts = masterProductRepository.countProductsWithActiveSellers();

        if (totalActiveProducts == 0) {
            return ComparePageResponseDto.builder().build();
        }

        int dayOfYear = java.time.LocalDate.now().getDayOfYear();
        int rotationIndex = dayOfYear % (int) totalActiveProducts;

        MasterProduct dailyProduct = masterProductRepository.findProductsWithActiveSellers(PageRequest.of(rotationIndex, 1))
                .getContent().get(0);

        List<SellerProduct> sellers = sellerProductRepository.findActiveByMasterProductId(dailyProduct.getId());
        int safeQty = sellers.stream()
                .mapToInt(SellerProduct::getMinimumOrderQuantity)
                .min()
                .orElse(1);

        return getCompareData(dailyProduct.getId(), safeQty);
    }

    @Transactional(readOnly = true)
    public List<FeaturedComparisonDto> getDailyFeaturedComparisons() {
        long totalActiveProducts = masterProductRepository.countProductsWithActiveSellers();

        if (totalActiveProducts == 0) {
            return new ArrayList<>();
        }

        int totalPages = (int) Math.ceil((double) totalActiveProducts / 4);
        int pageIndex = java.time.LocalDate.now().getDayOfYear() % totalPages;

        List<MasterProduct> dailyProducts = masterProductRepository.findProductsWithActiveSellers(PageRequest.of(pageIndex, 4)).getContent();
        List<FeaturedComparisonDto> result = new ArrayList<>();

        for (MasterProduct mp : dailyProducts) {
            List<SellerProduct> sellers = sellerProductRepository.findActiveByMasterProductId(mp.getId());
            sellers.sort(java.util.Comparator.comparing(SellerProduct::getPrice));

            List<String> profileIds = sellers.stream().limit(3).map(SellerProduct::getBusinessProfileId).collect(Collectors.toList());
            Map<String, BusinessProfile> profileMap = businessProfileRepository.findAllById(profileIds)
                    .stream().collect(Collectors.toMap(BusinessProfile::getId, p -> p));

            List<FeaturedComparisonDto.FeaturedSupplierDto> supplierDtos = new ArrayList<>();
            int limit = Math.min(3, sellers.size());
            for (int j = 0; j < limit; j++) {
                SellerProduct sp = sellers.get(j);
                BusinessProfile bp = profileMap.get(sp.getBusinessProfileId());
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

    private List<VolumeUpsellDto> generateAiVolumeDeals(List<SellerProduct> allSellers, int requestedQty, BigDecimal marketAverageTotal, Map<String, BusinessProfile> profileMap) {
        List<VolumeUpsellDto> upsells = new ArrayList<>();

        List<SellerProduct> bulkDealers = allSellers.stream()
                .filter(sp -> sp.getBulkDealQuantity() != null && sp.getBulkDealQuantity() > requestedQty)
                .collect(Collectors.toList());

        for (SellerProduct sp : bulkDealers) {
            BusinessProfile bp = profileMap.get(sp.getBusinessProfileId());
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
    public void submitEnquiry(GlobalEnquiryRequest request, String buyerId, HttpServletRequest httpRequest) {
        MasterProduct masterProduct = masterProductRepository.findById(request.getMasterProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        GlobalEnquiry enquiry = GlobalEnquiry.builder()
                .buyerId(buyerId)
                .masterProduct(masterProduct)
                .requestedQuantity(request.getRequestedQuantity())
                .targetPrice(request.getTargetPrice())
                .message(request.getMessage())
                .status("OPEN")
                .build();

        globalEnquiryRepository.save(enquiry);

        logAudit(buyerId, AuditAction.ENQUIRY_SUBMITTED, "Submitted global enquiry for product: " + masterProduct.getProductName(), httpRequest);
    }

    private void logAudit(String userId, AuditAction action, String details, HttpServletRequest request) {
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.ENQUIRY)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
    }
}