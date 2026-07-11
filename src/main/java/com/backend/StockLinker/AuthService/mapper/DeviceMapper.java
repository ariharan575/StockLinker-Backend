package com.backend.StockLinker.AuthService.mapper;

import com.backend.StockLinker.AuthService.dto.response.DeviceInfoResponse;
import com.backend.StockLinker.AuthService.model.UserDevice;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeviceMapper {

    public DeviceInfoResponse toDeviceInfoResponse(UserDevice device) {
        if (device == null) {
            return null;
        }

        return DeviceInfoResponse.builder()
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .browser(device.getBrowser())
                .browserVersion(device.getBrowserVersion())
                .os(device.getOs())
                .osVersion(device.getOsVersion())
                .manufacturer(device.getManufacturer())
                .model(device.getModel())
                .platform(device.getPlatform())
                .ipAddress(device.getIpAddress())
                .country(device.getCountry())
                .city(device.getCity())
                .lastActivityAt(device.getLastActivityAt())
                .trusted(device.isTrusted())
                .active(device.isActive())
                .currentDevice(device.isCurrentDevice())
                .build();
    }

    public List<DeviceInfoResponse> toDeviceInfoResponseList(List<UserDevice> devices) {
        if (devices == null || devices.isEmpty()) {
            return Collections.emptyList();
        }

        return devices.stream()
                .map(this::toDeviceInfoResponse)
                .collect(Collectors.toList());
    }
}