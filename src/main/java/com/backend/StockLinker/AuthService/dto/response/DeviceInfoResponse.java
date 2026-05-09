package com.backend.StockLinker.AuthService.dto.response;

import com.backend.StockLinker.AuthService.model.UserDevice;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeviceInfoResponse {
    private String id;
    private String deviceName;
    private String deviceType;
    private String os;
    private String browser;
    private String ipAddress;
    private String location;
    private LocalDateTime lastActivityAt;
    private boolean trusted;
    private boolean active;

    public static DeviceInfoResponse fromDevice(UserDevice device) {
        if (device == null) return null;

        return DeviceInfoResponse.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .os(device.getOs())
                .browser(device.getBrowser())
                .ipAddress(device.getIpAddress())
                .lastActivityAt(device.getLastActivityAt())
                .trusted(device.isTrusted())
                .active(device.isActive())
                .build();
    }
}