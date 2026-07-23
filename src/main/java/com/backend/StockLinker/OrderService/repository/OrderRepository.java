package com.backend.StockLinker.OrderService.repository;

import com.backend.StockLinker.OrderService.enums.OrderStatus;
import com.backend.StockLinker.OrderService.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByBuyerIdOrderByCreatedAtDesc(String buyerId);
    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(String buyerId, OrderStatus status);

    List<Order> findBySellerIdOrderByCreatedAtDesc(String sellerId);
    List<Order> findBySellerIdAndStatusOrderByCreatedAtDesc(String sellerId, OrderStatus status);

    List<Order> findBySellerIdAndDeliveryDateAndStatusInOrderByDeliverySequenceNumberAsc(
            String sellerId, LocalDate deliveryDate, List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.sellerId = :sellerId AND o.status IN ('PROCESSING', 'OUT_FOR_DELIVERY', 'DELIVERED') ORDER BY o.deliverySequenceNumber ASC")
    List<Order> findDailyRouteBySellerId(@Param("sellerId") String sellerId);
}