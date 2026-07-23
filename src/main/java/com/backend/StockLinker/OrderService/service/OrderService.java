package com.backend.StockLinker.OrderService.service;

import com.backend.StockLinker.OrderService.dto.OrderActionDtos;
import com.backend.StockLinker.OrderService.dto.OrderRequestDto;
import com.backend.StockLinker.OrderService.dto.OrderResponseDto;
import com.backend.StockLinker.OrderService.enums.OrderStatus;
import com.backend.StockLinker.OrderService.model.DeliveryTracking;
import com.backend.StockLinker.OrderService.model.Invoice;
import com.backend.StockLinker.OrderService.model.Order;
import com.backend.StockLinker.OrderService.model.OrderItem;
import com.backend.StockLinker.OrderService.repository.DeliveryTrackingRepository;
import com.backend.StockLinker.OrderService.repository.OrderRepository;
import com.backend.StockLinker.ProfileService.model.BusinessProfile;
import com.backend.StockLinker.ProfileService.model.SellerProduct;
import com.backend.StockLinker.ProfileService.repository.postgres.BusinessProfileRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.SellerProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DeliveryTrackingRepository deliveryTrackingRepository;
    private final SellerProductRepository sellerProductRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final OrderWebSocketService webSocketService;

    @Transactional
    public void placeOrder(String buyerId, OrderRequestDto request) {
        BusinessProfile sellerProfile = businessProfileRepository.findById(request.getBusinessProfileId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        Order order = Order.builder()
                .orderNumber("SL-" + System.currentTimeMillis())
                .buyerId(buyerId)
                .sellerId(sellerProfile.getUserId())
                .status(OrderStatus.PENDING)
                .placedAt(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderRequestDto.OrderItemRequest itemReq : request.getItems()) {
            SellerProduct product = sellerProductRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .originalProductId(product.getId())
                    .productName(product.getProductName())
                    .brand(product.getBrand())
                    .packageSize(product.getPackageSize())
                    .unit(product.getUnit())
                    .priceAtPurchase(product.getPrice())
                    .quantity(itemReq.getQuantity())
                    .lineTotal(lineTotal)
                    .build();

            order.addOrderItem(orderItem);
        }

        order.setTotalAmount(subtotal);

        Invoice invoice = Invoice.builder()
                .order(order)
                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sellerBusinessName(sellerProfile.getBusinessName())
                .sellerGstin(sellerProfile.getGstNumber() != null ? sellerProfile.getGstNumber() : "N/A")
                .subtotal(subtotal)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(subtotal)
                .build();

        order.setInvoice(invoice);
        Order savedOrder = orderRepository.save(order);

        // Instant Real-time WebSocket Push to Seller
        webSocketService.notifyUserOrderUpdate(
                sellerProfile.getUserId(),
                savedOrder.getId(),
                OrderStatus.PENDING.name(),
                "NEW_ORDER_RECEIVED",
                Map.of("orderNumber", savedOrder.getOrderNumber(), "totalAmount", savedOrder.getTotalAmount())
        );
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersForUser(String userId, String userRole, String status) {
        List<Order> orders;
        boolean isWholesaler = "WHOLESALER".equalsIgnoreCase(userRole);

        if (status == null || status.equalsIgnoreCase("all")) {
            orders = isWholesaler ? orderRepository.findBySellerIdOrderByCreatedAtDesc(userId)
                    : orderRepository.findByBuyerIdOrderByCreatedAtDesc(userId);
        } else {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            orders = isWholesaler ? orderRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(userId, orderStatus)
                    : orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(userId, orderStatus);
        }

        return orders.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void acceptAndScheduleOrder(String orderId, String wholesalerUserId, OrderActionDtos.ScheduleOrderDto scheduleDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getSellerId().equals(wholesalerUserId)) {
            throw new RuntimeException("Unauthorized action");
        }

        order.setStatus(OrderStatus.PROCESSING);
        order.setConfirmedAt(LocalDateTime.now());
        order.setDeliveryDate(scheduleDto.getDeliveryDate());

        orderRepository.save(order);

        // Create or update tracking entry
        DeliveryTracking tracking = deliveryTrackingRepository.findByOrderId(orderId)
                .orElse(DeliveryTracking.builder()
                        .orderId(order.getId())
                        .sellerId(order.getSellerId())
                        .buyerId(order.getBuyerId())
                        .build());

        tracking.setScheduledDate(scheduleDto.getDeliveryDate());
        tracking.setSequenceOrder(order.getDeliverySequenceNumber() != null ? order.getDeliverySequenceNumber() : 99);
        tracking.setDeliveryStatus(OrderStatus.PROCESSING);
        deliveryTrackingRepository.save(tracking);

        // Notify Buyer
        webSocketService.notifyUserOrderUpdate(
                order.getBuyerId(),
                order.getId(),
                OrderStatus.PROCESSING.name(),
                "ORDER_ACCEPTED",
                Map.of("deliveryDate", scheduleDto.getDeliveryDate().toString())
        );
    }

    @Transactional
    public void rejectOrder(String orderId, String wholesalerUserId, OrderActionDtos.RejectOrderDto rejectDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getSellerId().equals(wholesalerUserId)) {
            throw new RuntimeException("Unauthorized action");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setRejectionReason(rejectDto.getReason());

        orderRepository.save(order);

        // Clean tracking if exists
        deliveryTrackingRepository.findByOrderId(orderId).ifPresent(deliveryTrackingRepository::delete);

        // Notify Buyer
        webSocketService.notifyUserOrderUpdate(
                order.getBuyerId(),
                order.getId(),
                OrderStatus.CANCELLED.name(),
                "ORDER_REJECTED",
                Map.of("reason", rejectDto.getReason())
        );
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByDeliveryDate(String wholesalerUserId, LocalDate deliveryDate) {
        List<Order> orders = orderRepository.findBySellerIdAndDeliveryDateAndStatusInOrderByDeliverySequenceNumberAsc(
                wholesalerUserId,
                deliveryDate,
                List.of(OrderStatus.PROCESSING, OrderStatus.OUT_FOR_DELIVERY)
        );
        return orders.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void updateDeliverySequence(String sellerId, OrderActionDtos.UpdateSequenceDto sequenceDto) {
        List<String> orderedIds = sequenceDto.getOrderedOrderIds();

        for (int i = 0; i < orderedIds.size(); i++) {
            String orderId = orderedIds.get(i);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            if (!order.getSellerId().equals(sellerId)) {
                throw new RuntimeException("Unauthorized action for seller");
            }

            int seq = i + 1;
            order.setDeliverySequenceNumber(seq);
            orderRepository.save(order);

            DeliveryTracking tracking = deliveryTrackingRepository.findByOrderId(orderId)
                    .orElse(DeliveryTracking.builder()
                            .orderId(order.getId())
                            .sellerId(order.getSellerId())
                            .buyerId(order.getBuyerId())
                            .scheduledDate(sequenceDto.getDeliveryDate())
                            .deliveryStatus(order.getStatus())
                            .build());

            tracking.setSequenceOrder(seq);
            deliveryTrackingRepository.save(tracking);

            // Notify each buyer about updated route position
            webSocketService.notifyUserOrderUpdate(
                    order.getBuyerId(),
                    order.getId(),
                    order.getStatus().name(),
                    "ROUTE_SEQUENCE_UPDATED",
                    Map.of("sequence", seq)
            );
        }
    }

    @Transactional
    public void startRouteForDate(String sellerId, LocalDate deliveryDate) {
        List<Order> orders = orderRepository.findBySellerIdAndDeliveryDateAndStatusInOrderByDeliverySequenceNumberAsc(
                sellerId, deliveryDate, List.of(OrderStatus.PROCESSING));

        LocalDateTime now = LocalDateTime.now();
        for (Order order : orders) {
            order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
            order.setOutForDeliveryAt(now);
            orderRepository.save(order);

            deliveryTrackingRepository.findByOrderId(order.getId()).ifPresent(dt -> {
                dt.setDeliveryStatus(OrderStatus.OUT_FOR_DELIVERY);
                deliveryTrackingRepository.save(dt);
            });

            webSocketService.notifyUserOrderUpdate(
                    order.getBuyerId(),
                    order.getId(),
                    OrderStatus.OUT_FOR_DELIVERY.name(),
                    "ROUTE_STARTED",
                    Map.of("message", "Delivery driver is en route!")
            );
        }
    }

    @Transactional
    public void markAsDelivered(String orderId, String currentUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getSellerId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized action");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(now);
        orderRepository.save(order);

        deliveryTrackingRepository.findByOrderId(orderId).ifPresent(dt -> {
            dt.setDeliveryStatus(OrderStatus.DELIVERED);
            dt.setDeliveredAt(now);
            deliveryTrackingRepository.save(dt);
        });

        // Broadcast to Buyer
        webSocketService.notifyUserOrderUpdate(
                order.getBuyerId(),
                order.getId(),
                OrderStatus.DELIVERED.name(),
                "ORDER_DELIVERED",
                Map.of("deliveredAt", now.toString())
        );

        // Broadcast to Wholesaler (Self sync across tabs)
        webSocketService.notifyUserOrderUpdate(
                order.getSellerId(),
                order.getId(),
                OrderStatus.DELIVERED.name(),
                "ORDER_DELIVERED_SELLER_SYNC",
                Map.of("deliveredAt", now.toString())
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveDeliveryRoute(String orderId, String currentUserId) {
        Order referenceOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<Order> activeOrders = orderRepository.findDailyRouteBySellerId(referenceOrder.getSellerId());

        return activeOrders.stream().map(order -> {
            String buyerName = businessProfileRepository.findByUserId(order.getBuyerId())
                    .map(BusinessProfile::getBusinessName)
                    .orElse("Retail Partner");

            if (order.getBuyerId().equals(currentUserId)) {
                buyerName += " (You)";
            }

            return Map.<String, Object>of(
                    "orderId", order.getId(),
                    "buyerName", buyerName,
                    "status", order.getStatus().name(),
                    "sequence", order.getDeliverySequenceNumber() != null ? order.getDeliverySequenceNumber() : 99,
                    "time", order.getOutForDeliveryAt() != null ? "En Route" : "Pending Dispatch",
                    "deliveryDate", order.getDeliveryDate() != null ? order.getDeliveryDate().toString() : "TBD"
            );
        }).collect(Collectors.toList());
    }

    private OrderResponseDto mapToDto(Order o) {
        String buyerName = businessProfileRepository.findByUserId(o.getBuyerId())
                .map(BusinessProfile::getBusinessName)
                .orElse("Shopkeeper Partner");

        return OrderResponseDto.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus())
                .sellerName(o.getInvoice() != null ? o.getInvoice().getSellerBusinessName() : "Unknown Seller")
                .buyerName(buyerName)
                .sellerLocation("Standard Location")
                .totalAmount(o.getTotalAmount())
                .totalItems(o.getOrderItems().size())
                .deliveryDate(o.getDeliveryDate())
                .deliverySequenceNumber(o.getDeliverySequenceNumber())
                .rejectionReason(o.getRejectionReason())
                .placedAt(o.getPlacedAt())
                .confirmedAt(o.getConfirmedAt())
                .outForDeliveryAt(o.getOutForDeliveryAt())
                .deliveredAt(o.getDeliveredAt())
                .cancelledAt(o.getCancelledAt())
                .invoice(o.getInvoice() != null ? OrderResponseDto.InvoiceDto.builder()
                        .invoiceNumber(o.getInvoice().getInvoiceNumber())
                        .sellerGstin(o.getInvoice().getSellerGstin())
                        .subtotal(o.getInvoice().getSubtotal())
                        .tax(o.getInvoice().getTaxAmount())
                        .discount(o.getInvoice().getDiscountAmount())
                        .finalAmount(o.getInvoice().getFinalAmount())
                        .build() : null)
                .items(o.getOrderItems().stream().map(i -> OrderResponseDto.OrderItemDto.builder()
                        .productName(i.getProductName())
                        .sku(i.getOriginalProductId() != null && i.getOriginalProductId().length() >= 8
                                ? i.getOriginalProductId().substring(0, 8) : "SKU-N/A")
                        .packageSize(i.getPackageSize())
                        .unit(i.getUnit())
                        .quantity(i.getQuantity())
                        .price(i.getPriceAtPurchase())
                        .lineTotal(i.getLineTotal())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}