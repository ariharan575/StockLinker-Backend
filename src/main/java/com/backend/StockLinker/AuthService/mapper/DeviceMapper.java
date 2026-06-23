package com.backend.StockLinker.AuthService.mapper;

import com.backend.StockLinker.AuthService.dto.response.DeviceInfoResponse;
import com.backend.StockLinker.AuthService.model.UserDevice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeviceMapper {

    public DeviceInfoResponse toDeviceInfoResponse(UserDevice device) {

        if (device == null) {
            return null;
        }

        return DeviceInfoResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
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

    public List<DeviceInfoResponse> toDeviceInfoResponseList(
            List<UserDevice> devices
    ) {

        if (devices == null) {
            return List.of();
        }

        return devices.stream()
                .map(this::toDeviceInfoResponse)
                .collect(Collectors.toList());
    }
}