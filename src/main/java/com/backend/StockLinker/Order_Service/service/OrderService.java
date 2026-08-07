package com.backend.StockLinker.Order_Service.service;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.Exception.customExceptions.BadRequestException;
import com.backend.StockLinker.Exception.customExceptions.ForbiddenException;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.Notification_Service.enums.NotificationType;
import com.backend.StockLinker.Notification_Service.service.NotificationService;
import com.backend.StockLinker.Order_Service.dto.OrderActionDtos;
import com.backend.StockLinker.Order_Service.dto.OrderRequestDto;
import com.backend.StockLinker.Order_Service.dto.OrderResponseDto;
import com.backend.StockLinker.Order_Service.dto.ReorderSummaryDto;
import com.backend.StockLinker.Order_Service.enums.OrderStatus;
import com.backend.StockLinker.Order_Service.model.DeliveryTracking;
import com.backend.StockLinker.Order_Service.model.Invoice;
import com.backend.StockLinker.Order_Service.model.Order;
import com.backend.StockLinker.Order_Service.model.OrderItem;
import com.backend.StockLinker.Order_Service.repository.DeliveryTrackingRepository;
import com.backend.StockLinker.Order_Service.repository.OrderRepository;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.backend.StockLinker.Profile_Service.model.SellerProduct;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
    private final NotificationService notificationService;

    // Auditing
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @Transactional
    public void placeOrder(String buyerId, OrderRequestDto request, HttpServletRequest httpRequest) {
        BusinessProfile sellerProfile = businessProfileRepository.findById(request.getBusinessProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item.");
        }

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
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .originalProductId(product.getId())
                    .productName(product.getProductName())
                    .brand(product.getBrand())
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

        notificationService.saveAndSend(order.getSellerId(), buyerId, NotificationType.ORDER, savedOrder.getId(),
                "New Order Request", "You received a new order (" + savedOrder.getOrderNumber() + ").");

        webSocketService.notifyUserOrderUpdate(
                sellerProfile.getUserId(), savedOrder.getId(), OrderStatus.PENDING.name(), "NEW_ORDER_RECEIVED", Map.of()
        );

        logAudit(buyerId, AuditAction.ORDER_PLACED, "Placed order: " + savedOrder.getOrderNumber(), httpRequest);
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
    public void acceptAndScheduleOrder(String orderId, String wholesalerUserId, OrderActionDtos.ScheduleOrderDto scheduleDto, HttpServletRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getSellerId().equals(wholesalerUserId)) throw new ForbiddenException("Unauthorized action");

        order.setStatus(OrderStatus.PROCESSING);
        order.setConfirmedAt(LocalDateTime.now());
        order.setDeliveryDate(scheduleDto.getDeliveryDate());

        orderRepository.save(order);

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

        notificationService.saveAndSend(order.getBuyerId(), wholesalerUserId, NotificationType.ORDER, order.getId(),
                "Order Accepted", "Your order has been scheduled for " + scheduleDto.getDeliveryDate());

        webSocketService.notifyUserOrderUpdate(order.getBuyerId(), order.getId(), OrderStatus.PROCESSING.name(), "ORDER_ACCEPTED", Map.of());

        logAudit(wholesalerUserId, AuditAction.ORDER_ACCEPTED, "Accepted order: " + order.getOrderNumber(), request);
    }

    @Transactional
    public void rejectOrder(String orderId, String wholesalerUserId, OrderActionDtos.RejectOrderDto rejectDto, HttpServletRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getSellerId().equals(wholesalerUserId)) throw new ForbiddenException("Unauthorized");

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setRejectionReason(rejectDto.getReason());
        orderRepository.save(order);

        deliveryTrackingRepository.findByOrderId(orderId).ifPresent(deliveryTrackingRepository::delete);
        webSocketService.notifyUserOrderUpdate(order.getBuyerId(), order.getId(), OrderStatus.CANCELLED.name(), "ORDER_REJECTED", Map.of());

        logAudit(wholesalerUserId, AuditAction.ORDER_REJECTED, "Rejected order: " + order.getOrderNumber(), request);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByDeliveryDate(String wholesalerUserId, LocalDate deliveryDate) {
        List<Order> orders = orderRepository.findBySellerIdAndDeliveryDateAndStatusInOrderByDeliverySequenceNumberAsc(
                wholesalerUserId, deliveryDate, List.of(OrderStatus.PROCESSING, OrderStatus.OUT_FOR_DELIVERY));
        return orders.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void updateDeliverySequence(String sellerId, OrderActionDtos.UpdateSequenceDto sequenceDto, HttpServletRequest request) {
        List<String> orderedIds = sequenceDto.getOrderedOrderIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            Order order = orderRepository.findById(orderedIds.get(i)).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            if (!order.getSellerId().equals(sellerId)) throw new ForbiddenException("Unauthorized");

            int seq = i + 1;
            order.setDeliverySequenceNumber(seq);
            orderRepository.save(order);

            deliveryTrackingRepository.findByOrderId(order.getId()).ifPresent(dt -> {
                dt.setSequenceOrder(seq);
                deliveryTrackingRepository.save(dt);
            });
        }
        logAudit(sellerId, AuditAction.DELIVERY_SEQUENCE_UPDATED, "Updated delivery sequence for date: " + sequenceDto.getDeliveryDate(), request);
    }

    @Transactional
    public void startRouteForDate(String sellerId, LocalDate deliveryDate, HttpServletRequest request) {
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

            notificationService.saveAndSend(order.getBuyerId(), sellerId, NotificationType.ORDER, order.getId(),
                    "Out for Delivery", "Your order is on the way!");

            webSocketService.notifyUserOrderUpdate(order.getBuyerId(), order.getId(), OrderStatus.OUT_FOR_DELIVERY.name(), "ROUTE_STARTED", Map.of());
        }
        logAudit(sellerId, AuditAction.ROUTE_STARTED, "Started delivery route for date: " + deliveryDate, request);
    }

    @Transactional
    public void markAsDelivered(String orderId, String currentUserId, HttpServletRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getSellerId().equals(currentUserId)) throw new ForbiddenException("Unauthorized");

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(now);
        orderRepository.save(order);

        deliveryTrackingRepository.findByOrderId(orderId).ifPresent(dt -> {
            dt.setDeliveryStatus(OrderStatus.DELIVERED);
            dt.setDeliveredAt(now);
            deliveryTrackingRepository.save(dt);
        });

        notificationService.saveAndSend(order.getBuyerId(), currentUserId, NotificationType.ORDER, order.getId(),
                "Order Delivered", "Your order was successfully delivered.");

        webSocketService.notifyUserOrderUpdate(order.getBuyerId(), order.getId(), OrderStatus.DELIVERED.name(), "ORDER_DELIVERED", Map.of());

        logAudit(currentUserId, AuditAction.ORDER_DELIVERED, "Marked order as delivered: " + order.getOrderNumber(), request);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveDeliveryRoute(String orderId, String currentUserId) {
        Order referenceOrder = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        List<Order> routeOrders = orderRepository.findBySellerIdAndDeliveryDateAndStatusInOrderByDeliverySequenceNumberAsc(
                referenceOrder.getSellerId(), referenceOrder.getDeliveryDate(),
                List.of(OrderStatus.PROCESSING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED));

        return routeOrders.stream().map(order -> {
            String buyerName = businessProfileRepository.findByUserId(order.getBuyerId())
                    .map(BusinessProfile::getBusinessName).orElse("Retail Partner");

            if (order.getBuyerId().equals(currentUserId)) buyerName += " (You)";

            return Map.<String, Object>of(
                    "orderId", order.getId(),
                    "buyerName", buyerName,
                    "status", order.getStatus().name(),
                    "sequence", order.getDeliverySequenceNumber() != null ? order.getDeliverySequenceNumber() : 99,
                    "time", order.getOutForDeliveryAt() != null ? "En Route" : "Pending Dispatch"
            );
        }).collect(Collectors.toList());
    }

    // HIGHLY OPTIMIZED REORDER SUMMARY (Eliminated N+1 inside loop)
    @Transactional(readOnly = true)
    public List<ReorderSummaryDto> getReorderSummary(String userId) {
        List<Order> allUserOrders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(userId);

        if (allUserOrders.size() < 3) {
            return new ArrayList<>();
        }

        int limit = Math.min(8, allUserOrders.size());
        List<Order> limitedOrders = allUserOrders.subList(0, limit);

        // Fetch all product IDs efficiently outside the loop
        Set<String> productIds = limitedOrders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .map(OrderItem::getOriginalProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Batch fetch all required seller products to prevent DB queries inside the loop
        Map<String, SellerProduct> currentProductsMap = sellerProductRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(SellerProduct::getId, p -> p));

        List<ReorderSummaryDto> summaries = new ArrayList<>();

        for (Order order : limitedOrders) {
            BusinessProfile sellerProfile = businessProfileRepository.findByUserId(order.getSellerId()).orElse(null);
            if (sellerProfile == null) continue;

            BigDecimal currentTotal = BigDecimal.ZERO;
            List<String> itemNames = new ArrayList<>();
            String firstMasterProductId = null;

            for (OrderItem item : order.getOrderItems()) {
                itemNames.add(item.getProductName() + " ×" + item.getQuantity());

                SellerProduct currentProduct = null;
                if (item.getOriginalProductId() != null) {
                    currentProduct = currentProductsMap.get(item.getOriginalProductId());
                }

                if (currentProduct != null) {
                    currentTotal = currentTotal.add(currentProduct.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    if (firstMasterProductId == null && currentProduct.getMasterProduct() != null) {
                        firstMasterProductId = currentProduct.getMasterProduct().getId();
                    }
                } else {
                    currentTotal = currentTotal.add(item.getLineTotal());
                }
            }

            BigDecimal diff = currentTotal.subtract(order.getTotalAmount());

            summaries.add(ReorderSummaryDto.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .date(order.getPlacedAt() != null ? order.getPlacedAt().toLocalDate() : LocalDate.now())
                    .sellerName(sellerProfile.getBusinessName())
                    .sellerBusinessProfileId(sellerProfile.getId())
                    .masterProductId(firstMasterProductId)
                    .items(itemNames)
                    .previousPrice(order.getTotalAmount())
                    .currentPrice(currentTotal)
                    .priceDifference(diff)
                    .build());
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getDashboardOrders(String sellerId) {
        List<Order> recentOrders = orderRepository.findTop5BySellerIdOrderByCreatedAtDesc(sellerId);
        return recentOrders.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private void logAudit(String userId, AuditAction action, String details, HttpServletRequest request) {
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.ORDER)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
    }

    private OrderResponseDto mapToDto(Order o) {
        BusinessProfile buyerProfile = businessProfileRepository.findByUserId(o.getBuyerId()).orElse(null);
        String buyerName = buyerProfile != null ? buyerProfile.getBusinessName() : "Shopkeeper Partner";
        String buyerLocation = buyerProfile != null && buyerProfile.getBusinessAddress() != null
                ? buyerProfile.getBusinessAddress().getCity() + ", " + buyerProfile.getBusinessAddress().getState() : "Location Pending";

        BusinessProfile sellerProfile = businessProfileRepository.findByUserId(o.getSellerId()).orElse(null);
        String sellerName = sellerProfile != null ? sellerProfile.getBusinessName() : "Unknown Seller";
        String sellerLocation = sellerProfile != null && sellerProfile.getBusinessAddress() != null
                ? sellerProfile.getBusinessAddress().getCity() + ", " + sellerProfile.getBusinessAddress().getState() : "Location Pending";
        String sellerBusinessType = sellerProfile != null ? sellerProfile.getBusinessType() : "Wholesaler";

        return OrderResponseDto.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .status(o.getStatus())
                .sellerName(sellerName)
                .buyerName(buyerName)
                .sellerLocation(sellerLocation)
                .sellerBusinessType(sellerBusinessType)
                .buyerLocation(buyerLocation)
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
                                ? i.getOriginalProductId().substring(0, 8).toUpperCase() : "SKU-N/A")
                        .packageSize(i.getPackageSize())
                        .unit(i.getUnit())
                        .quantity(i.getQuantity())
                        .price(i.getPriceAtPurchase())
                        .lineTotal(i.getLineTotal())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}