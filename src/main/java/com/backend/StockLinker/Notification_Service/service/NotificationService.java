package com.backend.StockLinker.Notification_Service.service;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.Notification_Service.entity.Notification;
import com.backend.StockLinker.Notification_Service.enums.NotificationType;
import com.backend.StockLinker.Notification_Service.repository.NotificationRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

    // Auditing
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @Transactional
    public void saveAndSend(String recipientId, String senderName, NotificationType type, String referenceId, String title, String message) {
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
    public void markAsRead(String notificationId, String userId, HttpServletRequest request) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipientId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
                logAudit(userId, AuditAction.NOTIFICATION_READ, "Read notification: " + notificationId, request);
            }
        });
    }

    @Transactional
    public void markAllAsRead(String userId, HttpServletRequest request) {
        notificationRepository.markAllAsReadByRecipient(userId);
        logAudit(userId, AuditAction.NOTIFICATIONS_CLEARED, "Marked all notifications as read", request);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(5);
        notificationRepository.deleteOlderThan(cutoff);
        log.info("Cleaned up notifications older than 5 days.");
    }

    private void logAudit(String userId, AuditAction action, String details, HttpServletRequest request) {
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.NOTIFICATION)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
    }
}