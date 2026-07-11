package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.dto.request.AuditLogRequest;
import com.backend.StockLinker.AuthService.filter.DeviceParserService;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.model.UserDevice;
import com.backend.StockLinker.AuthService.repository.UserDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeviceSessionService {

    private final UserDeviceRepository userDeviceRepository;
    private final DeviceParserService parser;
    private final AuditService auditService;

    public UserDevice getOrCreate(User user, String deviceId, HttpServletRequest request) {
        return userDeviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .map(existing -> updateExistingDevice(existing, request))
                .orElseGet(() -> createNewDevice(user, deviceId, request));
    }

    private UserDevice updateExistingDevice(UserDevice device, HttpServletRequest request) {
        DeviceParserService.DeviceDetails info = parser.parse(request.getHeader("User-Agent"));

        updateIfBetter(device, info);

        device.setLastActivityAt(LocalDateTime.now());
        device.setIpAddress(getClientIp(request));
        device.setActive(true);

        UserDevice savedDevice = userDeviceRepository.save(device);

        log.info("User {} updated device {}", device.getUser().getId(), device.getDeviceId());

        return savedDevice;
    }

    private UserDevice createNewDevice(User user, String deviceId, HttpServletRequest request) {
        DeviceParserService.DeviceDetails info = parser.parse(request.getHeader("User-Agent"));

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
                .ipAddress(getClientIp(request))
                .trusted(false)
                .active(true)
                .loginAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();

        UserDevice savedDevice = userDeviceRepository.save(device);

        log.info("User {} registered device {}", user.getId(), deviceId);

        return savedDevice;
    }

    public void deactivateDevice(User user, String deviceId) {
        userDeviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId).ifPresent(device -> {
            device.setActive(false);
            device.setLogoutAt(LocalDateTime.now());
            userDeviceRepository.save(device);

            log.info("User {} deactivated device {}", user.getId(), deviceId);

        });
    }

    private void updateIfBetter(UserDevice device, DeviceParserService.DeviceDetails info) {
        if (isBetter(info.deviceName(), device.getDeviceName())) device.setDeviceName(info.deviceName());
        if (isBetter(info.deviceType(), device.getDeviceType())) device.setDeviceType(info.deviceType());
        if (isBetter(info.os(), device.getOs())) device.setOs(info.os());
        if (isBetter(info.osVersion(), device.getOsVersion())) device.setOsVersion(info.osVersion());
        if (isBetter(info.browser(), device.getBrowser())) device.setBrowser(info.browser());
        if (isBetter(info.browserVersion(), device.getBrowserVersion())) device.setBrowserVersion(info.browserVersion());
        if (isBetter(info.manufacturer(), device.getManufacturer())) device.setManufacturer(info.manufacturer());
        if (isBetter(info.model(), device.getModel())) device.setModel(info.model());
        if (isBetter(info.architecture(), device.getPlatform())) device.setPlatform(info.architecture());
    }

    private boolean isBetter(String newValue, String oldValue) {
        return newValue != null
                && !newValue.equalsIgnoreCase("UNKNOWN")
                && !newValue.equalsIgnoreCase("Unknown")
                && (oldValue == null || oldValue.equalsIgnoreCase("UNKNOWN") || oldValue.equalsIgnoreCase("Unknown"));
    }

    private String fallback(String value, String fallbackValue) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("UNKNOWN") || value.equalsIgnoreCase("Unknown")) {
            return fallbackValue;
        }
        return value;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}