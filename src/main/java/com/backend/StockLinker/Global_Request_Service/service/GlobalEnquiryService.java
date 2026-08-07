package com.backend.StockLinker.Global_Request_Service.service;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.Exception.customExceptions.BadRequestException;
import com.backend.StockLinker.Exception.customExceptions.ForbiddenException;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.Global_Request_Service.Dto.EnquiryResponseDto;
import com.backend.StockLinker.Global_Request_Service.Entity.GlobalEnquiry;
import com.backend.StockLinker.Global_Request_Service.Repository.GlobalEnquiryRepository;
import com.backend.StockLinker.Notification_Service.enums.NotificationType;
import com.backend.StockLinker.Notification_Service.service.NotificationService;
import com.backend.StockLinker.Order_Service.enums.OrderStatus;
import com.backend.StockLinker.Order_Service.model.Invoice;
import com.backend.StockLinker.Order_Service.model.Order;
import com.backend.StockLinker.Order_Service.model.OrderItem;
import com.backend.StockLinker.Order_Service.repository.OrderRepository;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.backend.StockLinker.Profile_Service.model.SellerProduct;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import com.backend.StockLinker.Profile_Service.repository.SellerProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalEnquiryService {

    private final GlobalEnquiryRepository globalEnquiryRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final SellerProductRepository sellerProductRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    // Auditing
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @Transactional(readOnly = true)
    public List<EnquiryResponseDto> getRelevantEnquiries(String userId) {
        BusinessProfile wholesaler = businessProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wholesaler profile not found"));

        List<GlobalEnquiry> enquiries = globalEnquiryRepository.findRelevantEnquiriesForWholesaler(
                wholesaler.getId(), PageRequest.of(0, 10));

        // 🔥 OPTIMIZATION: Prevent N+1 Query. Fetch all buyers in ONE batch!
        Set<String> buyerUserIds = enquiries.stream()
                .map(GlobalEnquiry::getBuyerId)
                .collect(Collectors.toSet());

        // Fetch all needed profiles in a single round trip
        List<BusinessProfile> buyerProfiles = businessProfileRepository.findAllById(buyerUserIds);
        Map<String, BusinessProfile> buyerMap = buyerProfiles.stream()
                .collect(Collectors.toMap(BusinessProfile::getUserId, profile -> profile));

        return enquiries.stream()
                .map(enq -> mapToDto(enq, buyerMap.get(enq.getBuyerId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void acceptEnquiryAsOrder(String enquiryId, String sellerUserId, HttpServletRequest request) {
        GlobalEnquiry enquiry = globalEnquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));

        if (!"OPEN".equals(enquiry.getStatus())) {
            throw new BadRequestException("This request has already been fulfilled or closed.");
        }

        BusinessProfile sellerProfile = businessProfileRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        // Verify the seller actually has this product actively listed
        SellerProduct sellerProduct = sellerProductRepository.findActiveByMasterProductId(enquiry.getMasterProduct().getId())
                .stream()
                .filter(sp -> sp.getBusinessProfileId().equals(sellerProfile.getId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("This product is not active in your catalog."));

        BigDecimal lineTotal = enquiry.getTargetPrice().multiply(BigDecimal.valueOf(enquiry.getRequestedQuantity()));

        // 1. Create the Order directly in PROCESSING state
        Order order = Order.builder()
                .orderNumber("SL-REQ-" + System.currentTimeMillis())
                .buyerId(enquiry.getBuyerId())
                .sellerId(sellerUserId)
                .status(OrderStatus.PROCESSING)
                .placedAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now())
                .totalAmount(lineTotal)
                .build();

        // 2. Create the Order Item
        OrderItem item = OrderItem.builder()
                .originalProductId(sellerProduct.getId())
                .productName(enquiry.getMasterProduct().getProductName())
                .brand(sellerProduct.getBrand())
                .unit(sellerProduct.getUnit())
                .priceAtPurchase(enquiry.getTargetPrice())
                .quantity(enquiry.getRequestedQuantity())
                .lineTotal(lineTotal)
                .build();

        order.addOrderItem(item);

        // 3. Generate Invoice
        Invoice invoice = Invoice.builder()
                .order(order)
                .invoiceNumber("INV-REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sellerBusinessName(sellerProfile.getBusinessName())
                .sellerGstin(sellerProfile.getGstNumber() != null ? sellerProfile.getGstNumber() : "N/A")
                .subtotal(lineTotal)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(lineTotal)
                .build();

        order.setInvoice(invoice);
        orderRepository.save(order);

        // 4. Mark Enquiry as Fulfilled
        enquiry.setStatus("FULFILLED");
        globalEnquiryRepository.save(enquiry);

        notificationService.saveAndSend(enquiry.getBuyerId(), sellerProfile.getBusinessName(), NotificationType.ENQUIRY, enquiry.getId(),
                "Enquiry Accepted", "Your target price request was accepted! An order has been created.");

        logAudit(sellerUserId, AuditAction.ENQUIRY_ACCEPTED, "Accepted enquiry ID: " + enquiryId + " resulting in order: " + order.getOrderNumber(), request);
    }

    private EnquiryResponseDto mapToDto(GlobalEnquiry enquiry, BusinessProfile buyer) {
        String buyerName = buyer != null ? buyer.getBusinessName() : "Unknown Buyer";
        String avatar = buyerName.substring(0, Math.min(buyerName.length(), 2)).toUpperCase();
        boolean verified = buyer != null && "VERIFIED".equalsIgnoreCase(buyer.getVerificationStatus());
        String location = buyer != null && buyer.getBusinessAddress() != null ? buyer.getBusinessAddress().getDistrict() : "Local Area";

        List<EnquiryResponseDto.ChipDto> chips = new ArrayList<>();
        chips.add(new EnquiryResponseDto.ChipDto("📦", enquiry.getRequestedQuantity() + " Units"));
        chips.add(new EnquiryResponseDto.ChipDto("₹", enquiry.getTargetPrice() + " Target"));

        return EnquiryResponseDto.builder()
                .id(enquiry.getId())
                .buyer(buyerName)
                .buyerProfileId(buyer != null ? buyer.getId() : null)
                .masterProductId(enquiry.getMasterProduct().getId())
                .avatar(avatar)
                .isVerified(verified)
                .status("Order Request")
                .title(enquiry.getMasterProduct().getProductName())
                .chips(chips)
                .message(enquiry.getMessage() != null ? enquiry.getMessage() : "Please provide your best quote for this requirement.")
                .location(location)
                .distance("Nearby")
                .time(calculateTimeAgo(enquiry.getCreatedAt()))
                .targetPrice(enquiry.getTargetPrice())
                .requestedQuantity(enquiry.getRequestedQuantity())
                .build();
    }

    private String calculateTimeAgo(LocalDateTime createdAt) {
        if (createdAt == null) return "Just now";
        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hrs ago";
        return (hours / 24) + " days ago";
    }

    private void logAudit(String userId, AuditAction action, String details, HttpServletRequest request) {
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.ENQUIRY)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
    }
}