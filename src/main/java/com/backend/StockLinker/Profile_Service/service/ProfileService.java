package com.backend.StockLinker.Profile_Service.service;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.model.User;
import com.backend.StockLinker.Auth_Service.repository.UserRepository;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductSubCategory;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductSubCategoryRepository;
import com.backend.StockLinker.Profile_Service.Dto.ProfileDTO;
import com.backend.StockLinker.Profile_Service.Dto.ProfileDTO.*;
import com.backend.StockLinker.Profile_Service.model.*;
import com.backend.StockLinker.Profile_Service.repository.BusinessAddressRepository;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.DeliveryConfigurationRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final BusinessProfileRepository profileRepository;
    private final BusinessAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final DeliveryConfigurationRepository deliveryConfigRepository;
    private final ProductSubCategoryRepository productSubCategoryRepository;
    private final SellerProductRepository sellerProductRepository;

    // Injected for Audit Logging
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @Transactional(readOnly = true)
    public FullProfileResponse getProfile(String userId) {
        BusinessProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user ID: " + userId));

        BusinessAddress address = addressRepository.findByBusinessProfileId(profile.getId()).orElse(new BusinessAddress());
        DeliveryConfiguration delivery = deliveryConfigRepository.findByBusinessProfileId(profile.getId()).orElse(new DeliveryConfiguration());

        User userUniqueId = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User identity not found"));

        List<ProductSubCategory> subCats = new ArrayList<>();
        if (profile.getCategoryIds() != null && !profile.getCategoryIds().isEmpty()) {
            List<String> catIds = Arrays.asList(profile.getCategoryIds().split(","));
            subCats = productSubCategoryRepository.findByProductCategoryIdIn(catIds);
        }

        List<SubCategoryDto> subCategoryDtos = subCats.stream()
                .map(sc -> SubCategoryDto.builder()
                        .id(sc.getId())
                        .name(sc.getName())
                        .imageName(sc.getImageName())
                        .build())
                .collect(Collectors.toList());

        long totalProducts = sellerProductRepository.countByBusinessProfileId(profile.getId());
        long lowStockCount = sellerProductRepository.countByBusinessProfileIdAndAvailableStockLessThan(profile.getId(), 20);

        // Fetch user-defined insights from database (fallback to N/A if null)
        String fastMovingCategory = profile.getFastMovingCategory() != null ? profile.getFastMovingCategory() : "N/A";
        String bestSellingProduct = profile.getBestSellingProduct() != null ? profile.getBestSellingProduct() : "N/A";

        return FullProfileResponse.builder()
                .businessProfileId(profile.getId())
                .userId(userUniqueId.getUniqueId() != null ? userUniqueId.getUniqueId() : profile.getUserId())
                .ownerName(profile.getOwnerName())
                .businessName(profile.getBusinessName())
                .mobileNumber(profile.getMobileNumber())
                .businessEmail(profile.getBusinessEmail())
                .alternateMobileNumber(profile.getAlternateMobileNumber())
                .businessType(profile.getBusinessType())
                .gstNumber(profile.getGstNumber())
                .yearsInBusiness(profile.getYearsInBusiness())
                .openingTime(profile.getOpeningTime())
                .closingTime(profile.getClosingTime())
                .verificationStatus(profile.getVerificationStatus() != null ? profile.getVerificationStatus() : "PENDING")
                .trustScore(profile.getTrustScore() != null ? profile.getTrustScore() : 0)
                .marketplaceRank(profile.getMarketplaceRank() != null ? profile.getMarketplaceRank() : 0)
                .addressLine1(address.getAddress())
                .addressLine2(address.getAlternate_address())
                .city(address.getCity())
                .district(address.getDistrict())
                .state(address.getState())
                .pincode(address.getPincode())
                .landmark(address.getLandmark())
                .coverageRadiusKm(delivery.getCoverageRadiusKm())
                .minimumOrderValue(delivery.getMinimumOrderValue())
                .deliveryCharge(delivery.getDeliveryCharge())
                .operatingDays(delivery.getOperatingDays())
                .routeSchedule(delivery.getRouteSchedule())
                .subCategories(subCategoryDtos)
                .totalProducts(totalProducts)
                .lowStockCount(lowStockCount)
                .bestSellingProduct(bestSellingProduct)
                .fastMovingCategory(fastMovingCategory)
                .build();
    }

    @Transactional
    public void updateAccount(String userId, AccountUpdateRequest req, HttpServletRequest request) {
        BusinessProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        profile.setOwnerName(req.getOwnerName());
        profile.setMobileNumber(req.getMobileNumber());
        profile.setBusinessEmail(req.getBusinessEmail());
        profileRepository.save(profile);

        logAudit(userId, AuditAction.PROFILE_UPDATED, "Account details updated", request);
    }

    @Transactional
    public void updateBusiness(String userId, ProfileDTO.BusinessUpdateRequest req, HttpServletRequest request) {
        BusinessProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        if (req.getBusinessName() != null) profile.setBusinessName(req.getBusinessName());
        if (req.getBusinessType() != null) profile.setBusinessType(req.getBusinessType());
        if (req.getGstNumber() != null) profile.setGstNumber(req.getGstNumber());
        if (req.getYearsInBusiness() != null) profile.setYearsInBusiness(req.getYearsInBusiness());
        if (req.getOpeningTime() != null) profile.setOpeningTime(req.getOpeningTime());
        if (req.getClosingTime() != null) profile.setClosingTime(req.getClosingTime());
        if (req.getAlternateMobileNumber() != null) profile.setAlternateMobileNumber(req.getAlternateMobileNumber());
        if (req.getBusinessDescription() != null) profile.setBusinessDescription(req.getBusinessDescription());
        profileRepository.save(profile);

        logAudit(userId, AuditAction.BUSINESS_UPDATED, "Business corporate details updated", request);
    }

    @Transactional
    public void updateStore(String userId, StoreUpdateRequest req, HttpServletRequest request) {
        BusinessProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        BusinessAddress address = addressRepository.findByBusinessProfileId(profile.getId()).orElse(new BusinessAddress());
        address.setBusinessProfile(profile);
        address.setAddress(req.getAddressLine1());
        address.setAlternate_address(req.getAddressLine2());
        address.setCity(req.getCity());
        address.setDistrict(req.getDistrict());
        address.setState(req.getState());
        address.setPincode(req.getPincode());
        address.setLandmark(req.getLandmark());
        addressRepository.save(address);

        logAudit(userId, AuditAction.STORE_UPDATED, "Store address/location updated", request);
    }

    @Transactional
    public void updateDeliveryAndInsights(String userId, DeliveryInsightsUpdateRequest req, HttpServletRequest request) {
        BusinessProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        // Save Editable Insights
        profile.setBestSellingProduct(req.getBestSellingProduct());
        profile.setFastMovingCategory(req.getFastMovingCategory());
        profileRepository.save(profile);

        // Save Delivery Config
        DeliveryConfiguration delivery = deliveryConfigRepository.findByBusinessProfileId(profile.getId()).orElse(new DeliveryConfiguration());
        delivery.setBusinessProfile(profile);
        delivery.setCoverageRadiusKm(req.getCoverageRadiusKm());
        delivery.setMinimumOrderValue(req.getMinimumOrderValue());
        delivery.setDeliveryCharge(req.getDeliveryCharge());
        delivery.setOperatingDays(req.getOperatingDays());
        delivery.setRouteSchedule(req.getRouteSchedule());
        deliveryConfigRepository.save(delivery);

        logAudit(userId, AuditAction.DELIVERY_INSIGHTS_UPDATED, "Delivery configuration and product insights updated", request);
    }

    // Helper method for standardized audit logging
    private void logAudit(String userId, AuditAction action, String details, HttpServletRequest request) {
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.PROFILE)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
    }
}