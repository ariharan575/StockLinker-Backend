package com.backend.StockLinker.Order_Service.repository;

import com.backend.StockLinker.Order_Service.model.DeliveryTracking;
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