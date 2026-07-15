package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.dto.request.AuditLogRequest;
import com.backend.StockLinker.AuthService.enums.AuditAction;
import com.backend.StockLinker.AuthService.enums.ResourceType;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.filter.DeviceParserService;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.model.UserDevice;
import com.backend.StockLinker.AuthService.repository.UserDeviceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.backend.StockLinker.AuthService.enums.AuditAction.DEVICE_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceSessionService {

    private final UserDeviceRepository userDeviceRepository;
    private final DeviceParserService parser;
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDevice getOrCreate(User user, String deviceId, HttpServletRequest request) {

        if (user == null || user.getId() == null) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "User information is required for device registration");
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Device ID is required for device registration");
        }

        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .map(existing -> updateExistingDevice(existing, request, user.getId()))
                .orElseGet(() -> createNewDevice(user, deviceId, request));

        // Force flush to ensure device is persisted
        entityManager.flush();

        log.debug("Device {} for user {} flushed and ready", deviceId, user.getId());

        return device;
    }

    private UserDevice updateExistingDevice(UserDevice device, HttpServletRequest request, String userId) {
        String userAgent = request != null ? request.getHeader(HttpHeaders.USER_AGENT) : null;
        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";

        DeviceParserService.DeviceDetails info = parser.parse(userAgent);
        String currentIp = ipAddressService.getClientIp(request);

        boolean hasChanges = updateIfBetter(device, info);

        if (!currentIp.equals(device.getIpAddress())) {
            device.setIpAddress(currentIp);
            hasChanges = true;
        }

        device.setLastActivityAt(LocalDateTime.now());
        device.setActive(true);

        device = userDeviceRepository.save(device);

        if (hasChanges) {
            log.info("User {} updated device properties for {}", userId, device.getDeviceId());

            auditService.log(AuditLogRequest.builder()
                    .userId(userId)
                    .action(DEVICE_UPDATED)
                    .resourceType(ResourceType.DEVICE)
                    .resourceId(device.getDeviceId())
                    .ipAddress(currentIp)
                    .userAgent(userAgent)
                    .deviceId(device.getDeviceId())
                    .requestUri(requestUri)
                    .status(AuditLog.Status.SUCCESS)
                    .build());
        }

        return device;
    }

    private UserDevice createNewDevice(User user, String deviceId, HttpServletRequest request) {
        String userAgent = request != null ? request.getHeader(HttpHeaders.USER_AGENT) : null;
        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";

        DeviceParserService.DeviceDetails info = parser.parse(userAgent);
        String currentIp = ipAddressService.getClientIp(request);

        UserDevice device = UserDevice.builder()
                .user(user)
                .deviceId(deviceId)
                .deviceName(fallback(info.deviceName(), "New Device"))
                .deviceType(fallback(info.deviceType(), "UNKNOWN"))
                .os(fallback(info.os(), "Unknown"))
                .osVersion(fallback(info.osVersion(), "Unknown"))
                .browser(fallback(info.browser(), "Unknown"))
                .browserVersion(fallback(info.browserVersion(), "Unknown"))
                .manufacturer(fallback(info.manufacturer(), "Unknown"))
                .model(fallback(info.model(), "Unknown"))
                .platform(fallback(info.architecture(), "Unknown"))
                .ipAddress(currentIp)
                .trusted(false)
                .active(true)
                .loginAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();

        UserDevice savedDevice = userDeviceRepository.save(device);

        log.info("User {} registered new device {}", user.getId(), deviceId);

        auditService.log(AuditLogRequest.builder()
                .userId(user.getId())
                .action(AuditAction.DEVICE_REGISTERED)
                .resourceType(ResourceType.DEVICE)
                .resourceId(deviceId)
                .ipAddress(currentIp)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .requestUri(requestUri)
                .status(AuditLog.Status.SUCCESS)
                .build());

        auditService.log(AuditLogRequest.builder()
                .userId(user.getId())
                .action(AuditAction.NEW_DEVICE_LOGIN)
                .resourceType(ResourceType.DEVICE)
                .resourceId(deviceId)
                .ipAddress(currentIp)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .requestUri(requestUri)
                .status(AuditLog.Status.SUCCESS)
                .build());

        return savedDevice;
    }

    @Transactional
    public void deactivateDevice(User user, String deviceId, HttpServletRequest request) {
        userDeviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId).ifPresent(device -> {
            device.setActive(false);
            device.setLogoutAt(LocalDateTime.now());
            userDeviceRepository.save(device);

            log.info("User {} deactivated device {}", user.getId(), deviceId);

            auditService.log(AuditLogRequest.builder()
                    .userId(user.getId())
                    .action(AuditAction.DEVICE_REMOVED)
                    .resourceType(ResourceType.DEVICE)
                    .resourceId(deviceId)
                    .ipAddress(ipAddressService.getClientIp(request))
                    .userAgent(request != null ? request.getHeader(HttpHeaders.USER_AGENT) : null)
                    .deviceId(deviceId)
                    .requestUri(request != null ? request.getRequestURI() : "UNKNOWN")
                    .status(AuditLog.Status.SUCCESS)
                    .build());
        });
    }

    private boolean updateIfBetter(UserDevice device, DeviceParserService.DeviceDetails info) {
        boolean changed = false;
        if (isBetter(info.deviceName(), device.getDeviceName())) { device.setDeviceName(info.deviceName()); changed = true; }
        if (isBetter(info.deviceType(), device.getDeviceType())) { device.setDeviceType(info.deviceType()); changed = true; }
        if (isBetter(info.os(), device.getOs())) { device.setOs(info.os()); changed = true; }
        if (isBetter(info.osVersion(), device.getOsVersion())) { device.setOsVersion(info.osVersion()); changed = true; }
        if (isBetter(info.browser(), device.getBrowser())) { device.setBrowser(info.browser()); changed = true; }
        if (isBetter(info.browserVersion(), device.getBrowserVersion())) { device.setBrowserVersion(info.browserVersion()); changed = true; }
        if (isBetter(info.manufacturer(), device.getManufacturer())) { device.setManufacturer(info.manufacturer()); changed = true; }
        if (isBetter(info.model(), device.getModel())) { device.setModel(info.model()); changed = true; }
        if (isBetter(info.architecture(), device.getPlatform())) { device.setPlatform(info.architecture()); changed = true; }
        return changed;
    }

    private boolean isBetter(String newValue, String oldValue) {
        return newValue != null
                && !newValue.isBlank()
                && !newValue.equalsIgnoreCase("UNKNOWN")
                && !newValue.equalsIgnoreCase("Unknown")
                && (oldValue == null || oldValue.isBlank() || oldValue.equalsIgnoreCase("UNKNOWN") || oldValue.equalsIgnoreCase("Unknown"));
    }

    private String fallback(String value, String fallbackValue) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("UNKNOWN") || value.equalsIgnoreCase("Unknown")) {
            return fallbackValue;
        }
        return value;
    }
}