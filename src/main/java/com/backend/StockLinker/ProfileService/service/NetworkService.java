package com.backend.StockLinker.ProfileService.service;

import com.backend.StockLinker.ProfileService.dto.response.NetworkDTO.*;
import com.backend.StockLinker.ProfileService.model.*;
import com.backend.StockLinker.ProfileService.repository.postgres.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Transactional(readOnly = true)
    public List<NetworkMemberResponse> getNearbyNetwork(
            String userId, String search, String category, Boolean verified,
            Double minRating, Integer maxDistance, String responseTime) {

        BusinessProfile currentUser = profileRepository.findByUserId(userId).orElseThrow();
        BusinessAddress currentUserAddress = addressRepository.findByBusinessProfileId(currentUser.getId()).orElse(new BusinessAddress());

        String targetRole = currentUser.getBusinessType().equalsIgnoreCase("WHOLESALER") ? "SHOPKEEPER" : "WHOLESALER";
        String userDistrict = currentUserAddress.getDistrict() != null ? currentUserAddress.getDistrict().trim() : "";

        if (userDistrict.isEmpty()) return new ArrayList<>();

        // 1. Fetch Real Users + Universal Mocks (Case-Insensitive handling)
        List<BusinessProfile> targetProfiles = profileRepository.findAll().stream().filter(p -> {
            if (p.getId().equals(currentUser.getId())) return false;

            BusinessAddress addr = addressRepository.findByBusinessProfileId(p.getId()).orElse(null);
            if (addr == null || addr.getDistrict() == null) return false;

            // 🚀 UNIVERSAL FALLBACK + CASE INSENSITIVE DISTRICT MATCHING
            boolean matchDistrict = addr.getDistrict().trim().equalsIgnoreCase(userDistrict) || addr.getDistrict().equalsIgnoreCase("Universal");
            boolean matchRole = p.getBusinessType().equalsIgnoreCase(targetRole);

            return matchDistrict && matchRole;
        }).collect(Collectors.toList());

        Stream<BusinessProfile> profileStream = targetProfiles.stream();

        // Apply Filters
        if (search != null && !search.trim().isEmpty()) {
            String s = search.toLowerCase();
            profileStream = profileStream.filter(p -> p.getBusinessName().toLowerCase().contains(s));
        }
        if (verified != null && verified) profileStream = profileStream.filter(p -> "VERIFIED".equalsIgnoreCase(p.getVerificationStatus()));
        if (minRating != null) profileStream = profileStream.filter(p -> p.getRating() != null && p.getRating() >= minRating);
        if (responseTime != null && !responseTime.isEmpty()) profileStream = profileStream.filter(p -> p.getResponseTime() != null && p.getResponseTime().equalsIgnoreCase(responseTime));

        List<BusinessProfile> filteredUsers = profileStream.collect(Collectors.toList());

        // 2. STRICT SORTING: Newest Users first, Mock Data dynamically pushed to bottom
        filteredUsers.sort((p1, p2) -> {
            boolean isMock1 = p1.getUserId().startsWith("mock");
            boolean isMock2 = p2.getUserId().startsWith("mock");

            // Mocks go to the bottom
            if (isMock1 && !isMock2) return 1;
            if (!isMock1 && isMock2) return -1;

            // Real users sorted by newest first
            if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                return p2.getCreatedAt().compareTo(p1.getCreatedAt()); // Descending
            }
            return 0;
        });

        return filteredUsers.stream()
                .map(p -> mapToDto(p, currentUser, "In " + userDistrict, null))
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public void announceArrivalToDistrict(String userId) {
        BusinessProfile newProfile = profileRepository.findByUserId(userId).orElseThrow();
        BusinessAddress address = addressRepository.findByBusinessProfileId(newProfile.getId()).orElse(null);
        if (address == null || address.getDistrict() == null) return;

        String targetRole = newProfile.getBusinessType().equalsIgnoreCase("WHOLESALER") ? "SHOPKEEPER" : "WHOLESALER";
        List<BusinessProfile> peersInDistrict = profileRepository.findNearbyInSameDistrict(address.getDistrict(), targetRole, newProfile.getId());

        for (BusinessProfile peer : peersInDistrict) {
            NetworkMemberResponse payload = mapToDto(newProfile, peer, "Just Arrived", null);
            WsNotification notification = new WsNotification("NEW_NEARBY_USER", newProfile.getBusinessName() + " just joined in your district!", payload);
            messagingTemplate.convertAndSendToUser(peer.getUserId(), "/queue/notifications", notification);
        }
    }

    @Transactional
    public void requestConnection(String userId, String partnerProfileId) {
        BusinessProfile requester = profileRepository.findByUserId(userId).orElseThrow();
        BusinessProfile receiver = profileRepository.findById(partnerProfileId).orElseThrow();

        if (connectionRepository.existsByRequesterAndReceiver(requester, receiver) || connectionRepository.existsByRequesterAndReceiver(receiver, requester)) {
            throw new RuntimeException("Connection request already exists.");
        }

        BusinessConnection connection = BusinessConnection.builder()
                .requester(requester).receiver(receiver)
                .status("PENDING").connectedAt(LocalDateTime.now()).build();
        connectionRepository.save(connection);

        NetworkMemberResponse payload = mapToDto(requester, receiver, "In District", connection.getId());
        WsNotification notification = new WsNotification("NEW_REQUEST", "New connection request from " + requester.getBusinessName(), payload);
        messagingTemplate.convertAndSendToUser(receiver.getUserId(), "/queue/notifications", notification);
    }

    @Transactional
    public void acceptConnection(String userId, String connectionId) {
        BusinessProfile currentUser = profileRepository.findByUserId(userId).orElseThrow();
        BusinessConnection connection = connectionRepository.findById(connectionId).orElseThrow();

        if (!connection.getReceiver().getId().equals(currentUser.getId())) throw new RuntimeException("Unauthorized");

        connection.setStatus("CONNECTED");
        connection.setConnectedAt(LocalDateTime.now());
        connectionRepository.save(connection);

        NetworkMemberResponse payload = mapToDto(currentUser, connection.getRequester(), "Connected Partner", connection.getId());
        WsNotification notification = new WsNotification("ACCEPTED", currentUser.getBusinessName() + " accepted your request!", payload);
        messagingTemplate.convertAndSendToUser(connection.getRequester().getUserId(), "/queue/notifications", notification);
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

    private NetworkMemberResponse mapToDto(BusinessProfile profile, BusinessProfile currentUser, String distanceLabel, String connectionId) {
        BusinessAddress address = addressRepository.findByBusinessProfileId(profile.getId()).orElse(new BusinessAddress());
        DeliveryConfiguration delivery = deliveryRepository.findByBusinessProfileId(profile.getId()).orElse(new DeliveryConfiguration());

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
                .subCategories(subCategoryDtos)
                .totalSubCategories((long) subCats.size())
                .connectionStatus(connStatus)
                .build();
    }
}