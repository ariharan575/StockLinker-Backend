package com.backend.StockLinker.Order_Service.controller;

import com.backend.StockLinker.Order_Service.dto.*;
import com.backend.StockLinker.Order_Service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequestDto request, Authentication auth) {
        orderService.placeOrder(auth.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Order placed successfully"));
    }

    @GetMapping
    public ResponseEntity<OrderListResponseDto> getMyOrders(
            @RequestParam(required = false, defaultValue = "all") String status,
            Authentication auth) {

        // 1. Extract role from JWT authorities statelessly
        String userRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_SHOPKEEPER")
                .replace("ROLE_", "");

        // 2. Fetch the actual orders
        List<OrderResponseDto> orders = orderService.getOrdersForUser(auth.getName(), userRole, status);

        // 3. Package both into the existing OrderListResponseDto
        OrderListResponseDto responseDto = OrderListResponseDto.builder()
                .userRole(userRole)
                .orders(orders)
                .build();

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<?> acceptAndSchedule(
            @PathVariable String orderId,
            @RequestBody OrderActionDtos.ScheduleOrderDto scheduleDto,
            Authentication auth) {
        orderService.acceptAndScheduleOrder(orderId, auth.getName(), scheduleDto);
        return ResponseEntity.ok(Map.of("message", "Order accepted and delivery scheduled"));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<?> rejectOrder(
            @PathVariable String orderId,
            @RequestBody OrderActionDtos.RejectOrderDto rejectDto,
            Authentication auth) {
        orderService.rejectOrder(orderId, auth.getName(), rejectDto);
        return ResponseEntity.ok(Map.of("message", "Order rejected successfully"));
    }

    @GetMapping("/by-date")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication auth) {
        return ResponseEntity.ok(orderService.getOrdersByDeliveryDate(auth.getName(), date));
    }

    @PutMapping("/route/sequence")
    public ResponseEntity<?> updateRouteSequence(
            @RequestBody OrderActionDtos.UpdateSequenceDto sequenceDto,
            Authentication auth) {
        orderService.updateDeliverySequence(auth.getName(), sequenceDto);
        return ResponseEntity.ok(Map.of("message", "Delivery route sequence saved"));
    }

    @PostMapping("/route/start")
    public ResponseEntity<?> startRouteForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication auth) {
        orderService.startRouteForDate(auth.getName(), date);
        return ResponseEntity.ok(Map.of("message", "Route started successfully"));
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<?> markAsDelivered(@PathVariable String orderId, Authentication auth) {
        orderService.markAsDelivered(orderId, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Order marked as delivered"));
    }

    @GetMapping("/{orderId}/route")
    public ResponseEntity<List<Map<String, Object>>> getDeliveryRoute(@PathVariable String orderId, Authentication auth) {
        return ResponseEntity.ok(orderService.getActiveDeliveryRoute(orderId, auth.getName()));
    }

    @GetMapping("/reorder-summary")
    public ResponseEntity<List<ReorderSummaryDto>> getReorderSummary(Authentication auth) {
        return ResponseEntity.ok(orderService.getReorderSummary(auth.getName()));
    }

    @GetMapping("/dashboard-orders")
    public ResponseEntity<List<OrderResponseDto>> getDashboardOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getDashboardOrders(auth.getName()));
    }
}