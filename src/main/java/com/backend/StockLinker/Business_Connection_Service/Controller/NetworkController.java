package com.backend.StockLinker.Business_Connection_Service.Controller;

import com.backend.StockLinker.Onboading_Service.dto.response.ApiResponse;
import com.backend.StockLinker.Business_Connection_Service.Dto.NetworkDTO.NetworkMemberResponse;
import com.backend.StockLinker.Business_Connection_Service.Services.NetworkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<ApiResponse<Page<NetworkMemberResponse>>> getNearby(
            Principal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false, defaultValue = "NEARBY") String scope,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer deliveryRadius,
            @RequestParam(required = false) String responseTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Now properly returning a Page for Frontend slicing!
        return ResponseEntity.ok(ApiResponse.success("Network fetched",
                networkService.getNearbyNetwork(getUserId(principal), search, categoryId, scope, minRating, deliveryRadius, responseTime, page, size)));
    }

    @GetMapping("/nearby/dashboard")
    public ResponseEntity<ApiResponse<List<NetworkMemberResponse>>> getDashboardNearby(Principal principal) {
        Page<NetworkMemberResponse> top10Page = networkService.getNearbyNetwork(getUserId(principal), null, null, "NEARBY", null, null, null, 0, 10);
        return ResponseEntity.ok(ApiResponse.success("Dashboard nearby network fetched", top10Page.getContent()));
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
    public ResponseEntity<ApiResponse<String>> requestConnection(Principal principal, @PathVariable String partnerId, HttpServletRequest request) {
        networkService.requestConnection(getUserId(principal), partnerId, request);
        return ResponseEntity.ok(ApiResponse.success("Connection request sent", null));
    }

    @PostMapping("/connect/accept/{connectionId}")
    public ResponseEntity<ApiResponse<String>> acceptConnection(Principal principal, @PathVariable String connectionId, HttpServletRequest request) {
        networkService.acceptConnection(getUserId(principal), connectionId, request);
        return ResponseEntity.ok(ApiResponse.success("Connection accepted", null));
    }

    @PostMapping("/announce")
    public ResponseEntity<ApiResponse<String>> announceArrival(Principal principal, HttpServletRequest request) {
        networkService.announceArrivalToDistrict(getUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success("Arrival broadcasted to district", null));
    }
}