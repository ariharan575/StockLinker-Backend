package com.backend.StockLinker.Notification_Service.repository;

import com.backend.StockLinker.Notification_Service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findTop20ByRecipientIdOrderByCreatedAtDesc(String recipientId);

    long countByRecipientIdAndIsReadFalse(String recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    void markAllAsReadByRecipient(String recipientId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    void deleteOlderThan(LocalDateTime cutoffDate);
}