package com.backend.StockLinker.ProfileService.controller;

import com.backend.StockLinker.ProfileService.dto.response.ApiResponse;
import com.backend.StockLinker.ProfileService.dto.response.NetworkDTO.NetworkMemberResponse;
import com.backend.StockLinker.ProfileService.service.NetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/network")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkService networkService;

    private String getUserId(Principal principal) {
        return principal != null ? principal.getName() : "test-user-id";
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NetworkMemberResponse>>> getNearby(
            Principal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer maxDistance,
            @RequestParam(required = false) String responseTime) {
        return ResponseEntity.ok(ApiResponse.success("Nearby network fetched",
                networkService.getNearbyNetwork(getUserId(principal), search, category, verified, minRating, maxDistance, responseTime)));
    }

    @GetMapping("/connected")
    public ResponseEntity<ApiResponse<List<NetworkMemberResponse>>> getConnected(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Connected network fetched", networkService.getConnectedNetwork(getUserId(principal))));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<NetworkMemberResponse>>> getPendingRequests(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Pending requests fetched", networkService.getPendingRequests(getUserId(principal))));
    }

    @PostMapping("/connect/{partnerId}")
    public ResponseEntity<ApiResponse<String>> requestConnection(Principal principal, @PathVariable String partnerId) {
        networkService.requestConnection(getUserId(principal), partnerId);
        return ResponseEntity.ok(ApiResponse.success("Connection request sent", null));
    }

    @PostMapping("/connect/accept/{connectionId}")
    public ResponseEntity<ApiResponse<String>> acceptConnection(Principal principal, @PathVariable String connectionId) {
        networkService.acceptConnection(getUserId(principal), connectionId);
        return ResponseEntity.ok(ApiResponse.success("Connection accepted", null));
    }

    // 🚀 NEW ENDPOINT TO TRIGGER LIVE RADAR:
    // Call this API when a user completes their profile to instantly show them on others' maps
    @PostMapping("/announce")
    public ResponseEntity<ApiResponse<String>> announceArrival(Principal principal) {
        networkService.announceArrivalToDistrict(getUserId(principal));
        return ResponseEntity.ok(ApiResponse.success("Arrival broadcasted to district", null));
    }
}