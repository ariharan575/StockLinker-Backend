package com.backend.StockLinker.Business_Connection_Service.Services;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.Business_Connection_Service.Entity.BusinessConnection;
import com.backend.StockLinker.Business_Connection_Service.Repository.BusinessConnectionRepository;
import com.backend.StockLinker.Business_Connection_Service.Dto.NetworkDTO.*;
import com.backend.StockLinker.Exception.customExceptions.ConflictException;
import com.backend.StockLinker.Exception.customExceptions.ForbiddenException;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.ProductCatagory_Service.Entity.ProductSubCategory;
import com.backend.StockLinker.ProductCatagory_Service.repository.ProductSubCategoryRepository;
import com.backend.StockLinker.Profile_Service.model.*;
import com.backend.StockLinker.Profile_Service.repository.BusinessAddressRepository;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.DeliveryConfigurationRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NetworkService {

    private final BusinessProfileRepository profileRepository;
    private final BusinessConnectionRepository connectionRepository;
    private final BusinessAddressRepository addressRepository;
    private final DeliveryConfigurationRepository deliveryRepository;
    private final SellerProductRepository sellerProductRepository;
    private final ProductSubCategoryRepository subCategoryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Auditing
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    private int getResponseRank(String rt) {
        if (rt == null || rt.isEmpty()) return 99;
        String lower = rt.toLowerCase().trim();
        if (lower.contains("< 1 hr") || lower.contains("1 hour")) return 1;
        if (lower.contains("< 24 hrs") || lower.contains("24 hours")) return 2;
        if (lower.contains("1-2 days")) return 3;
        return 4;
    }

    private List<String> getAllowedResponseTimes(String rt) {
        if (rt == null || rt.trim().isEmpty()) return new ArrayList<>();
        int rank = getResponseRank(rt);
        List<String> allowed = new ArrayList<>();
        if (rank >= 1) { allowed.add("< 1 hr"); allowed.add("1 hour"); }
        if (rank >= 2) { allowed.add("< 24 hrs"); allowed.add("24 hours"); }
        if (rank >= 3) { allowed.add("1-2 days"); }
        return allowed;
    }

    @Transactional(readOnly = true)
    public Page<NetworkMemberResponse> getNearbyNetwork(
            String userId, String search, String categoryId, String scope,
            Double minRating, Integer deliveryRadius, String responseTime, int page, int size) {

        BusinessProfile currentUser = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        BusinessAddress currentUserAddress = addressRepository.findByBusinessProfileId(currentUser.getId()).orElse(new BusinessAddress());

        String targetRole = currentUser.getBusinessType().equalsIgnoreCase("WHOLESALER") ? "SHOPKEEPER" : "WHOLESALER";
        String userDistrict = currentUserAddress.getDistrict() != null ? currentUserAddress.getDistrict().trim() : "";

        List<String> allowedResponseTimes = getAllowedResponseTimes(responseTime);
        boolean filterResponseTime = !allowedResponseTimes.isEmpty() && getResponseRank(responseTime) < 4;

        String searchParam = (search != null && !search.trim().isEmpty()) ? "%" + search.toLowerCase() + "%" : null;
        String categoryIdParam = (categoryId != null && !categoryId.trim().isEmpty()) ? "%" + categoryId + "%" : null;
        List<String> safeResponseTimes = allowedResponseTimes.isEmpty() ? List.of("DUMMY_VALUE") : allowedResponseTimes;

        Pageable pageLimit = PageRequest.of(page, size);

        // Fetch paginated chunk from database
        Page<BusinessProfile> filteredUsersPage = profileRepository.findNetworkWithFilters(
                currentUser.getId(),
                targetRole,
                scope != null ? scope.toUpperCase() : "NEARBY",
                userDistrict,
                searchParam,
                minRating,
                deliveryRadius,
                categoryIdParam,
                filterResponseTime,
                safeResponseTimes,
                pageLimit
        );

        boolean isScopeAll = "ALL".equalsIgnoreCase(scope);

        List<NetworkMemberResponse> content = filteredUsersPage.stream()
                .map(p -> mapToDto(p, currentUser, isScopeAll ? "Statewide" : "In " + userDistrict, null))
                .collect(Collectors.toList());

        // Keep the mock user sorting for the current page chunk
        content.sort((p1, p2) -> {
            boolean isMock1 = p1.getUserId().startsWith("mock");
            boolean isMock2 = p2.getUserId().startsWith("mock");
            if (isMock1 && !isMock2) return 1;
            if (!isMock1 && isMock2) return -1;
            return 0;
        });

        // Return the properly sliced page
        return new PageImpl<>(content, pageLimit, filteredUsersPage.getTotalElements());
    }

    @Transactional
    public void announceArrivalToDistrict(String userId, HttpServletRequest request) {
        BusinessProfile newProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        BusinessAddress address = addressRepository.findByBusinessProfileId(newProfile.getId()).orElse(null);
        if (address == null || address.getDistrict() == null) return;

        String targetRole = newProfile.getBusinessType().equalsIgnoreCase("WHOLESALER") ? "SHOPKEEPER" : "WHOLESALER";
        List<BusinessProfile> peersInDistrict = profileRepository.findNearbyInSameDistrict(address.getDistrict(), targetRole, newProfile.getId());

        for (BusinessProfile peer : peersInDistrict) {
            NetworkMemberResponse payload = mapToDto(newProfile, peer, "Just Arrived", null);
            WsNotification notification = new WsNotification("NEW_NEARBY_USER", newProfile.getBusinessName() + " just joined in your district!", payload);
            messagingTemplate.convertAndSendToUser(peer.getUserId(), "/queue/notifications", notification);
        }

        logAudit(userId, AuditAction.ARRIVAL_ANNOUNCED, "Broadcasted arrival to district: " + address.getDistrict(), request);
    }

    @Transactional
    public void requestConnection(String userId, String partnerProfileId, HttpServletRequest request) {
        BusinessProfile requester = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        BusinessProfile receiver = profileRepository.findById(partnerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));

        if (connectionRepository.existsByRequesterAndReceiver(requester, receiver) || connectionRepository.existsByRequesterAndReceiver(receiver, requester)) {
            throw new ConflictException("Connection request already exists or is connected.");
        }

        BusinessConnection connection = BusinessConnection.builder()
                .requester(requester).receiver(receiver)
                .status("PENDING").connectedAt(LocalDateTime.now()).build();
        connectionRepository.save(connection);

        NetworkMemberResponse payload = mapToDto(requester, receiver, "In District", connection.getId());
        WsNotification notification = new WsNotification("NEW_REQUEST", "New connection request from " + requester.getBusinessName(), payload);
        messagingTemplate.convertAndSendToUser(receiver.getUserId(), "/queue/notifications", notification);

        logAudit(userId, AuditAction.CONNECTION_REQUESTED, "Requested connection with: " + receiver.getId(), request);
    }

    @Transactional
    public void acceptConnection(String userId, String connectionId, HttpServletRequest request) {
        BusinessProfile currentUser = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        BusinessConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));

        if (!connection.getReceiver().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Unauthorized to accept this connection.");
        }

        connection.setStatus("CONNECTED");
        connection.setConnectedAt(LocalDateTime.now());
        connectionRepository.save(connection);

        NetworkMemberResponse payload = mapToDto(currentUser, connection.getRequester(), "Connected Partner", connection.getId());
        WsNotification notification = new WsNotification("ACCEPTED", currentUser.getBusinessName() + " accepted your request!", payload);
        messagingTemplate.convertAndSendToUser(connection.getRequester().getUserId(), "/queue/notifications", notification);

        logAudit(userId, AuditAction.CONNECTION_ACCEPTED, "Accepted connection from: " + connection.getRequester().getId(), request);
    }

    @Transactional(readOnly = true)
    public List<NetworkMemberResponse> getPendingRequests(String userId) {
        BusinessProfile currentUser = profileRepository.findByUserId(userId).orElseThrow();
        return connectionRepository.findByReceiverAndStatus(currentUser, "PENDING").stream()
                .map(conn -> mapToDto(conn.getRequester(), currentUser, "In District", conn.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NetworkMemberResponse> getConnectedNetwork(String userId) {
        BusinessProfile currentUser = profileRepository.findByUserId(userId).orElseThrow();
        List<BusinessConnection> connections = connectionRepository.findByRequesterAndStatus(currentUser, "CONNECTED");
        connections.addAll(connectionRepository.findByReceiverAndStatus(currentUser, "CONNECTED"));

        return connections.stream().map(conn -> {
            BusinessProfile partner = conn.getRequester().getId().equals(currentUser.getId()) ? conn.getReceiver() : conn.getRequester();
            return mapToDto(partner, currentUser, "Connected Partner", conn.getId());
        }).collect(Collectors.toList());
    }

    private void logAudit(String userId, AuditAction action, String details, HttpServletRequest request) {
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.NETWORK)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
    }

    // Keep mapToDto exactly as you had it
    private NetworkMemberResponse mapToDto(BusinessProfile profile, BusinessProfile currentUser, String distanceLabel, String connectionId) {
        BusinessAddress address = profile.getBusinessAddress() != null ? profile.getBusinessAddress() :
                addressRepository.findByBusinessProfileId(profile.getId()).orElse(new BusinessAddress());

        DeliveryConfiguration delivery = profile.getDeliveryConfiguration() != null ? profile.getDeliveryConfiguration() :
                deliveryRepository.findByBusinessProfileId(profile.getId()).orElse(new DeliveryConfiguration());

        String mainCategory = "General Business";
        List<ProductSubCategory> subCats = new ArrayList<>();
        if (profile.getCategoryIds() != null && !profile.getCategoryIds().isEmpty()) {
            subCats = subCategoryRepository.findByProductCategoryIdIn(Arrays.asList(profile.getCategoryIds().split(",")));
            if (!subCats.isEmpty()) mainCategory = subCats.get(0).getName();
        }

        List<SubCategoryMiniDto> subCategoryDtos = subCats.stream().limit(4)
                .map(sc -> SubCategoryMiniDto.builder().name(sc.getName())
                        .image(sc.getImageName() != null ? sc.getImageName() : "https://picsum.photos/seed/" + sc.getId() + "/100").build())
                .collect(Collectors.toList());

        List<SellerProduct> sellerProducts = sellerProductRepository.findByBusinessProfileId(profile.getId());
        boolean readyStock = sellerProducts.stream().anyMatch(sp -> sp.getAvailableStock() > 0);

        List<String> verificationBadges = new ArrayList<>();
        if ("VERIFIED".equalsIgnoreCase(profile.getVerificationStatus())) verificationBadges.add("Business Verified");
        if (profile.getGstNumber() != null && !profile.getGstNumber().isEmpty()) verificationBadges.add("GST Verified");

        String avatarUrl = "https://ui-avatars.com/api/?name=" + profile.getBusinessName().replace(" ", "+") + "&background=F43F5E&color=fff";

        String connStatus = "NONE";
        if (connectionId == null) {
            Optional<BusinessConnection> fwd = connectionRepository.findByRequesterAndReceiver(currentUser, profile);
            Optional<BusinessConnection> bwd = connectionRepository.findByRequesterAndReceiver(profile, currentUser);
            if (fwd.isPresent()) { connStatus = fwd.get().getStatus(); connectionId = fwd.get().getId(); }
            else if (bwd.isPresent()) { connStatus = bwd.get().getStatus(); connectionId = bwd.get().getId(); }
        } else {
            connStatus = connectionRepository.findById(connectionId).map(BusinessConnection::getStatus).orElse("NONE");
        }

        String storeTiming = "Not Set";
        if (profile.getOpeningTime() != null && profile.getClosingTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
            storeTiming = profile.getOpeningTime().format(formatter) + " - " + profile.getClosingTime().format(formatter);
        }
        String storeSize = profile.getStoreSize() != null ? profile.getStoreSize().name() : "Not Set";

        return NetworkMemberResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .connectionId(connectionId)
                .name(profile.getBusinessName())
                .category(mainCategory)
                .location(address.getCity() != null ? address.getCity() + ", " + address.getState() : "Location Hidden")
                .distance(distanceLabel)
                .rating(profile.getRating() != null && profile.getRating() > 0 ? profile.getRating() : 0.0)
                .reviews(profile.getReviewCount() != null ? profile.getReviewCount() : 0)
                .verification(verificationBadges)
                .experience(profile.getYearsInBusiness() != null ? profile.getYearsInBusiness() + " Yrs" : "New")
                .orders(profile.getTotalOrdersFulfilled() != null ? profile.getTotalOrdersFulfilled() + "+" : "New")
                .responseTime(profile.getResponseTime() != null ? profile.getResponseTime() : "< 1 hr")
                .status("Active")
                .avatar(avatarUrl)
                .readyStock(readyStock)
                .deliveryRadius(delivery.getCoverageRadiusKm() != null ? delivery.getCoverageRadiusKm() + " km" : "Not Set")
                .deliveryEstimate(delivery.getOperatingDays() != null ? delivery.getOperatingDays() : "Standard")
                .storeSize(storeSize)
                .storeTiming(storeTiming)
                .subCategories(subCategoryDtos)
                .totalSubCategories((long) subCats.size())
                .connectionStatus(connStatus)
                .build();
    }
}