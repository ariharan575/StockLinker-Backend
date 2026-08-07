package com.backend.StockLinker.Onboading_Service.Service;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Auth_Service.enums.AccountStatus;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Exception.customExceptions.BadRequestException;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Auth_Service.model.User;
import com.backend.StockLinker.Auth_Service.repository.UserRepository;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;

import com.backend.StockLinker.Onboading_Service.dto.request.AddressInfoRequestDto;
import com.backend.StockLinker.Onboading_Service.dto.request.BusinessInfoRequestDto;
import com.backend.StockLinker.Onboading_Service.dto.request.MarketplaceInfoRequestDto;
import com.backend.StockLinker.ProductCatagory_Service.dto.response.CategoryResponseDto;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.backend.StockLinker.Profile_Service.model.BusinessAddress;
import com.backend.StockLinker.Profile_Service.model.DeliveryConfiguration;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.BusinessAddressRepository;
import com.backend.StockLinker.Profile_Service.repository.DeliveryConfigurationRepository;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductCategoryRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAccountStatus() != AccountStatus.PENDING_ONBOARDING) {
            throw new BadRequestException("User is not in onboarding state or has already completed onboarding.");
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

        if ("SHOPKEEPER".equalsIgnoreCase(roleName)) {
            profile.setYearsInBusiness(dto.getYearsInBusiness());
        }

        profile = businessProfileRepository.save(profile);

        final BusinessProfile savedProfile = profile;

        if ("WHOLESALER".equalsIgnoreCase(roleName)) {
            DeliveryConfiguration delivery = deliveryRepository.findByBusinessProfileId(savedProfile.getId())
                    .orElseGet(() -> {
                        DeliveryConfiguration d = new DeliveryConfiguration();
                        d.setBusinessProfile(savedProfile);
                        return d;
                    });
            delivery.setCoverageRadiusKm(dto.getDeliveryRadius() != null ? dto.getDeliveryRadius() : 0);
            deliveryRepository.save(delivery);
        }

        logAudit(user.getId(), AuditAction.ONBOARDING_STARTED, profile.getId(), request);
    }

    @Transactional
    public void saveAddressInfo(AddressInfoRequestDto dto, HttpServletRequest request) {
        User user = getPendingUser();
        BusinessProfile profile = businessProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Please complete Step 1 (Business Details) first."));

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
        address.setState("Tamil Nadu");
        address.setPincode(dto.getPincode());

        businessAddressRepository.save(address);
        logAudit(user.getId(), AuditAction.BUSINESS_UPDATED, profile.getId(), request);
    }

    @Transactional
    public void saveMarketplaceInfo(MarketplaceInfoRequestDto dto, HttpServletRequest request) {
        User user = getPendingUser();
        BusinessProfile profile = businessProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Please complete previous steps first."));

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