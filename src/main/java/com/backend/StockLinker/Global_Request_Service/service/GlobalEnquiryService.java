package com.backend.StockLinker.Global_Request_Service.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<EnquiryResponseDto> getRelevantEnquiries(String userId) {
        BusinessProfile wholesaler = businessProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wholesaler profile not found"));

        List<GlobalEnquiry> enquiries = globalEnquiryRepository.findRelevantEnquiriesForWholesaler(
                wholesaler.getId(), PageRequest.of(0, 10));

        return enquiries.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void acceptEnquiryAsOrder(String enquiryId, String sellerUserId) {
        GlobalEnquiry enquiry = globalEnquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new RuntimeException("Enquiry not found"));

        if (!"OPEN".equals(enquiry.getStatus())) {
            throw new RuntimeException("This request has already been fulfilled or closed.");
        }

        BusinessProfile sellerProfile = businessProfileRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        // Verify the seller actually has this product actively listed
        SellerProduct sellerProduct = sellerProductRepository.findActiveByMasterProductId(enquiry.getMasterProduct().getId())
                .stream()
                .filter(sp -> sp.getBusinessProfileId().equals(sellerProfile.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("This product is not active in your catalog."));

        BigDecimal lineTotal = enquiry.getTargetPrice().multiply(BigDecimal.valueOf(enquiry.getRequestedQuantity()));

        // 1. Create the Order directly in PROCESSING state (Skipping PENDING since seller is actively accepting)
        Order order = Order.builder()
                .orderNumber("SL-REQ-" + System.currentTimeMillis())
                .buyerId(enquiry.getBuyerId())
                .sellerId(sellerUserId)
                .status(OrderStatus.PROCESSING)
                .placedAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now())
                .totalAmount(lineTotal)
                .build();

        // 2. Create the Order Item using the agreed Target Price
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

        // 4. Mark Enquiry as Fulfilled so it disappears from the feed
        enquiry.setStatus("FULFILLED");
        globalEnquiryRepository.save(enquiry);

        notificationService.saveAndSend(enquiry.getBuyerId(), sellerProfile.getBusinessName(), NotificationType.ENQUIRY, enquiry.getId(),
                "Enquiry Accepted", "Your target price request was accepted! An order has been created.");
    }

    private EnquiryResponseDto mapToDto(GlobalEnquiry enquiry) {
        BusinessProfile buyer = businessProfileRepository.findByUserId(enquiry.getBuyerId()).orElse(null);

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
}