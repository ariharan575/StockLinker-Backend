package com.backend.StockLinker.ProfileService.service;

import com.backend.StockLinker.AuthService.dto.request.AuditLogRequest;
import com.backend.StockLinker.AuthService.enums.AccountStatus;
import com.backend.StockLinker.AuthService.enums.AuditAction;
import com.backend.StockLinker.AuthService.enums.ResourceType;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.service.AuditService;
import com.backend.StockLinker.AuthService.service.IpAddressService;

import com.backend.StockLinker.ProfileService.dto.request.AddressInfoRequestDto;
import com.backend.StockLinker.ProfileService.dto.request.BusinessInfoRequestDto;
import com.backend.StockLinker.ProfileService.dto.request.MarketplaceInfoRequestDto;
import com.backend.StockLinker.ProfileService.dto.response.CategoryResponseDto;
import com.backend.StockLinker.ProfileService.model.BusinessProfile;
import com.backend.StockLinker.ProfileService.model.BusinessAddress;
import com.backend.StockLinker.ProfileService.model.DeliveryConfiguration;
import com.backend.StockLinker.ProfileService.repository.postgres.BusinessProfileRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.BusinessAddressRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.DeliveryConfigurationRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.ProductCategoryRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final BusinessAddressRepository businessAddressRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final DeliveryConfigurationRepository deliveryRepository;
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    public List<CategoryResponseDto> getActiveCategories() {
        return productCategoryRepository.findByActiveTrue().stream()
                .map(cat -> new CategoryResponseDto(cat.getId(), cat.getName()))
                .collect(Collectors.toList());
    }

    private User getPendingUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (user.getAccountStatus() != AccountStatus.PENDING_ONBOARDING) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "User is not in onboarding state or has already completed onboarding.");
        }
        return user;
    }

    private String getUserRole(User user) {
        return user.getRole() != null ? user.getRole().getName().toUpperCase() : "UNKNOWN";
    }

    @Transactional
    public void saveBusinessInfo(BusinessInfoRequestDto dto, HttpServletRequest request) {
        User user = getPendingUser();
        String roleName = getUserRole(user);

        BusinessProfile profile = businessProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    BusinessProfile p = new BusinessProfile();
                    p.setUserId(user.getId());
                    p.setStatus("ONBOARDING");
                    p.setBusinessType(roleName);
                    p.setDeliverySupported(false);
                    p.setVerificationStatus("PENDING");
                    p.setTrustScore(0);
                    p.setMarketplaceRank(0);
                    p.setRating(0.0);
                    p.setReviewCount(0);
                    return p;
                });

        profile.setOwnerName(dto.getOwnerName());
        profile.setBusinessName(dto.getBusinessName());
        profile.setMobileNumber(dto.getMobile());
        profile.setBusinessEmail(dto.getBusinessEmail());
        profile.setGstNumber(dto.getGstNumber());

        profile = businessProfileRepository.save(profile);

        final BusinessProfile savedProfile = profile;

        // 🚀 Save Delivery Radius in Step 1
        DeliveryConfiguration delivery = deliveryRepository.findByBusinessProfileId(savedProfile.getId())
                .orElseGet(() -> {
                    DeliveryConfiguration d = new DeliveryConfiguration();
                    d.setBusinessProfile(savedProfile); // Use the final copy here
                    return d;
                });
        delivery.setCoverageRadiusKm(dto.getDeliveryRadius());
        deliveryRepository.save(delivery);

        logAudit(user.getId(), AuditAction.ONBOARDING_STARTED, profile.getId(), request);
    }

    @Transactional
    public void saveAddressInfo(AddressInfoRequestDto dto, HttpServletRequest request) {
        User user = getPendingUser();
        BusinessProfile profile = businessProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.BAD_REQUEST, "Please complete Step 1 (Business Details) first."));

        BusinessAddress address = businessAddressRepository.findByBusinessProfileId(profile.getId())
                .orElseGet(() -> {
                    BusinessAddress a = new BusinessAddress();
                    a.setBusinessProfile(profile);
                    return a;
                });

        address.setAddress(dto.getAddressLine1());
        address.setAlternate_address(dto.getAddressLine2());
        address.setArea(dto.getArea());
        address.setCity(dto.getCityOrTown());
        address.setDistrict(dto.getDistrict());
        address.setState("Tamil Nadu"); // Force Tamil Nadu automatically
        address.setPincode(dto.getPincode());

        businessAddressRepository.save(address);
        logAudit(user.getId(), AuditAction.BUSINESS_UPDATED, profile.getId(), request);
    }

    @Transactional
    public void saveMarketplaceInfo(MarketplaceInfoRequestDto dto, HttpServletRequest request) {
        User user = getPendingUser();
        BusinessProfile profile = businessProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.BAD_REQUEST, "Please complete previous steps first."));

        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            String joinedIds = String.join(",", dto.getCategoryIds());
            profile.setCategoryIds(joinedIds);
        }

        profile.setDeliverySupported(dto.getDeliveryAvailable() != null ? dto.getDeliveryAvailable() : false);
        profile.setStoreSize(dto.getStoreSize() != null ? dto.getStoreSize() : null);
        profile.setStatus("ACTIVE");
        profile.setVerificationStatus("VERIFIED");

        businessProfileRepository.save(profile);

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        logAudit(user.getId(), AuditAction.ONBOARDING_COMPLETED, profile.getId(), request);
    }

    private void logAudit(String userId, AuditAction action, String profileId, HttpServletRequest request) {
        auditService.log(AuditLogRequest.builder()
                .userId(userId).action(action).resourceType(ResourceType.BUSINESS)
                .resourceId(profileId).ipAddress(ipAddressService.getClientIp(request))
                .userAgent(request != null ? request.getHeader(HttpHeaders.USER_AGENT) : null)
                .deviceId(request != null ? (String) request.getAttribute("deviceId") : null)
                .requestUri(request != null ? request.getRequestURI() : "UNKNOWN")
                .status(AuditLog.Status.SUCCESS).build());
    }
}