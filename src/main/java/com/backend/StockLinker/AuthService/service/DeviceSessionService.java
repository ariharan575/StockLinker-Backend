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

    private final UserDeviceRepository repo;
    private final DeviceParserService parser;

    // =========================================================
    // ✅ GET OR CREATE DEVICE
    // =========================================================
    public UserDevice getOrCreate(User user, String deviceId, HttpServletRequest request) {

        return repo.findByDeviceId(deviceId)
                .map(existing -> update(existing, request))
                .orElseGet(() -> create(user, deviceId, request));
    }

    // =========================================================
    // 🔄 UPDATE EXISTING DEVICE
    // =========================================================
    private UserDevice update(UserDevice device, HttpServletRequest request) {

        DeviceParserService.DeviceDetails info =
                parser.parse(request.getHeader("User-Agent"));

        // 🔥 UPDATE ONLY IF BETTER DATA AVAILABLE
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
        device.setIpAddress(getIp(request));

        return repo.save(device);
    }

    // =========================================================
    // 🆕 CREATE NEW DEVICE
    // =========================================================
    private UserDevice create(User user, String deviceId, HttpServletRequest request) {

        DeviceParserService.DeviceDetails info =
                parser.parse(request.getHeader("User-Agent"));

        UserDevice device = UserDevice.builder()
                .user(user)
                .deviceId(deviceId)
                .deviceName(fallback(info.deviceName(), "New Device"))
                .deviceType(fallback(info.deviceType(), "UNKNOWN"))
                .os(fallback(info.os(), "Unknown"))
                .browser(fallback(info.browser(), "Unknown"))
                .ipAddress(getIp(request))
                .trusted(false)
                .active(true)
                .lastActivityAt(LocalDateTime.now())
                .build();

        log.info("New device created: {} for user {}", deviceId, user.getId());

        return repo.save(device);
    }

    // =========================================================
    // 🔍 HELPER: BETTER VALUE CHECK
    // =========================================================
    private boolean isBetter(String newValue, String oldValue) {
        return newValue != null
                && !newValue.equalsIgnoreCase("UNKNOWN")
                && !newValue.equalsIgnoreCase("Unknown")
                && (oldValue == null || oldValue.equalsIgnoreCase("UNKNOWN"));
    }

    // =========================================================
    // 🔄 FALLBACK HANDLER
    // =========================================================
    private String fallback(String value, String fallback) {
        if (value == null || value.equalsIgnoreCase("UNKNOWN")) {
            return fallback;
        }
        return value;
    }

    // =========================================================
    // 🌐 IP
    // =========================================================
    private String getIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0] : request.getRemoteAddr();
    }
}
