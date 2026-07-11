package com.backend.StockLinker.AuthService.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeviceInfoResponse {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String browser;
    private String browserVersion;
    private String os;
    private String osVersion;
    private String manufacturer;
    private String model;
    private String platform;
    private String ipAddress;
    private String country;
    private String city;
    private LocalDateTime lastActivityAt;
    private boolean trusted;
    private boolean active;
    private boolean currentDevice;
}