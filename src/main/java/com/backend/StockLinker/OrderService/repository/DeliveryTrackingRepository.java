package com.backend.StockLinker.OrderService.repository;

import com.backend.StockLinker.OrderService.model.DeliveryTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, String> {
    List<DeliveryTracking> findBySellerIdAndScheduledDateOrderBySequenceOrderAsc(String sellerId, LocalDate scheduledDate);
    Optional<DeliveryTracking> findByOrderId(String orderId);
    void deleteByOrderId(String orderId);
}