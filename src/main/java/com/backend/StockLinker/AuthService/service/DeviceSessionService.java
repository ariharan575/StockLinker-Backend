package com.backend.StockLinker.AuthService.service;

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

    // =========================================================
    // ✅ GET OR CREATE DEVICE
    // =========================================================
    public UserDevice getOrCreate(User user, String deviceId, HttpServletRequest request) {
        return userDeviceRepository.findByDeviceId(deviceId)
                .map(existing -> updateExistingDevice(existing, request))
                .orElseGet(() -> createNewDevice(user, deviceId, request));
    }

    // =========================================================
    // 🔄 UPDATE EXISTING DEVICE
    // =========================================================
    private UserDevice updateExistingDevice(UserDevice device, HttpServletRequest request) {
        DeviceParserService.DeviceDetails info = parser.parse(request.getHeader("User-Agent"));

        // Update only if better data is available
        if (isBetter(info.deviceName(), device.getDeviceName())) {
            device.setDeviceName(info.deviceName());
        }
        if (isBetter(info.deviceType(), device.getDeviceType())) {
            device.setDeviceType(info.deviceType());
        }
        if (isBetter(info.os(), device.getOs())) {
            device.setOs(info.os());
        }
        if (isBetter(info.browser(), device.getBrowser())) {
            device.setBrowser(info.browser());
        }

        device.setLastActivityAt(LocalDateTime.now());
        device.setIpAddress(getClientIp(request));
        device.setActive(true);

        return userDeviceRepository.save(device);
    }

    // =========================================================
    // 🆕 CREATE NEW DEVICE
    // =========================================================
    private UserDevice createNewDevice(User user, String deviceId, HttpServletRequest request) {
        DeviceParserService.DeviceDetails info = parser.parse(request.getHeader("User-Agent"));

        UserDevice device = UserDevice.builder()
                .user(user)
                .deviceId(deviceId)
                .deviceName(fallback(info.deviceName(), "New Device"))
                .deviceType(fallback(info.deviceType(), "UNKNOWN"))
                .os(fallback(info.os(), "Unknown"))
                .browser(fallback(info.browser(), "Unknown"))
                .ipAddress(getClientIp(request))
                .trusted(false)
                .active(true)
                .lastActivityAt(LocalDateTime.now())
                .build();

        log.info("New device created: {} for user {}", deviceId, user.getId());
        return userDeviceRepository.save(device);
    }

    // =========================================================
    // 🔍 CHECK IF NEW VALUE IS BETTER THAN OLD
    // =========================================================
    private boolean isBetter(String newValue, String oldValue) {
        return newValue != null
                && !newValue.equalsIgnoreCase("UNKNOWN")
                && !newValue.equalsIgnoreCase("Unknown")
                && (oldValue == null || oldValue.equalsIgnoreCase("UNKNOWN") || oldValue.equalsIgnoreCase("Unknown"));
    }

    // =========================================================
    // 🔄 FALLBACK VALUE
    // =========================================================
    private String fallback(String value, String fallbackValue) {
        if (value == null || value.equalsIgnoreCase("UNKNOWN") || value.equalsIgnoreCase("Unknown")) {
            return fallbackValue;
        }
        return value;
    }

    // =========================================================
    // 🗑️ DEACTIVATE DEVICE
    // =========================================================
    public void deactivateDevice(String deviceId) {
        userDeviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setActive(false);
            userDeviceRepository.save(device);
            log.info("Device deactivated: {}", deviceId);
        });
    }

    // =========================================================
    // 🔒 TRUST/UNTRUST DEVICE
    // =========================================================
    public void setDeviceTrusted(String deviceId, boolean trusted) {
        userDeviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setTrusted(trusted);
            userDeviceRepository.save(device);
            log.info("Device {} trust set to: {}", deviceId, trusted);
        });
    }

    // =========================================================
    // 🌐 IP HELPER
    // =========================================================
    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}