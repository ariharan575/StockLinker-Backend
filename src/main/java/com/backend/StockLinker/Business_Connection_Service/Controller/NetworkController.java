package com.backend.StockLinker.Business_Connection_Service.Controller;

import com.backend.StockLinker.Onboading_Service.dto.response.ApiResponse;
import com.backend.StockLinker.Business_Connection_Service.Dto.NetworkDTO.NetworkMemberResponse;
import com.backend.StockLinker.Business_Connection_Service.Services.NetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

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
            @RequestParam(required = false) String categoryId,  // Now uses exact DB ID
            @RequestParam(required = false, defaultValue = "NEARBY") String scope, // NEARBY or ALL
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer deliveryRadius, // Checks delivery range
            @RequestParam(required = false) String responseTime) {  // Hierarchical checks
        return ResponseEntity.ok(ApiResponse.success("Network fetched",
                networkService.getNearbyNetwork(getUserId(principal), search, categoryId, scope, minRating, deliveryRadius, responseTime)));
    }

    @GetMapping("/nearby/dashboard")
    public ResponseEntity<ApiResponse<List<NetworkMemberResponse>>> getDashboardNearby(Principal principal) {
        List<NetworkMemberResponse> fullList = networkService.getNearbyNetwork(getUserId(principal), null, null, "NEARBY", null, null, null);
        List<NetworkMemberResponse> top10 = fullList.stream().limit(10).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Dashboard nearby network fetched", top10));
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

    @PostMapping("/announce")
    public ResponseEntity<ApiResponse<String>> announceArrival(Principal principal) {
        networkService.announceArrivalToDistrict(getUserId(principal));
        return ResponseEntity.ok(ApiResponse.success("Arrival broadcasted to district", null));
    }
}