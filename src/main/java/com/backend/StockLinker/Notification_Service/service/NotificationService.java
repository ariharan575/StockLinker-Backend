package com.backend.StockLinker.Notification_Service.service;

import com.backend.StockLinker.Notification_Service.entity.Notification;
import com.backend.StockLinker.Notification_Service.enums.NotificationType;
import com.backend.StockLinker.Notification_Service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void saveAndSend(String recipientId, String senderName, NotificationType type, String referenceId, String title, String message) {
        // 1. Save to Database
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .senderName(senderName)
                .type(type)
                .referenceId(referenceId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        // 2. Push to WebSocket in real-time
        messagingTemplate.convertAndSendToUser(
                recipientId,
                "/queue/notifications",
                notification
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserNotifications(String userId) {
        List<Notification> notifications = notificationRepository.findTop20ByRecipientIdOrderByCreatedAtDesc(userId);
        long unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(userId);
        return Map.of("notifications", notifications, "unreadCount", unreadCount);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipientId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByRecipient(userId);
    }

    // AUTO-CLEANUP: Runs every night at Midnight to delete notifications older than 5 days
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(5);
        notificationRepository.deleteOlderThan(cutoff);
        log.info("Cleaned up notifications older than 5 days.");
    }
}
