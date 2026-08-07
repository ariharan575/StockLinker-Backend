package com.backend.StockLinker.Security;

import org.springframework.stereotype.Service;

@Service
public class DeviceParserService {

    private static final DeviceDetails DEFAULT_DEVICE = new DeviceDetails(
            "Generic Device",
            "UNKNOWN",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown",
            "Unknown"
    );

    public DeviceDetails parse(String userAgentString) {
        if (userAgentString == null || userAgentString.isBlank()) {
            return DEFAULT_DEVICE;
        }

        String os = parseOS(userAgentString);
        String browser = parseBrowser(userAgentString);
        String deviceType = parseDeviceType(userAgentString);
        String architecture = parseArchitecture(userAgentString);
        String manufacturer = parseManufacturer(userAgentString, os);

        // Derive a generic model based on type if manufacturer is known
        String model = deviceType.equals("Desktop") ? "PC" : "Mobile Device";
        String deviceName = manufacturer.equals("Unknown") ? deviceType : manufacturer + " " + deviceType;

        return new DeviceDetails(
                deviceName,
                deviceType,
                os,
                "Unknown", // Deep OS versioning requires heavy regex, skipped to save RAM
                browser,
                "Unknown", // Deep Browser versioning requires heavy regex, skipped to save RAM
                manufacturer,
                model,
                architecture
        );
    }

    private String parseOS(String ua) {
        if (ua.contains("Windows NT 10.0") || ua.contains("Windows NT 11.0")) return "Windows 10/11";
        if (ua.contains("Windows NT")) return "Windows";
        if (ua.contains("Mac OS X")) return "Mac OS";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Linux")) return "Linux";
        return "Unknown OS";
    }

    private String parseBrowser(String ua) {
        // Order matters here since Edge/Opera also contain "Chrome" and "Safari" in their User-Agents
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/") && !ua.contains("Chrome")) return "Safari";
        return "Unknown Browser";
    }

    private String parseDeviceType(String ua) {
        if (ua.contains("Mobi") || ua.contains("iPhone") || ua.contains("Android")) {
            if (ua.contains("iPad") || (ua.contains("Android") && !ua.contains("Mobi"))) {
                return "Tablet";
            }
            return "Mobile";
        }
        return "Desktop";
    }

    private String parseArchitecture(String ua) {
        if (ua.contains("x64") || ua.contains("Win64") || ua.contains("x86_64")) return "x64";
        if (ua.contains("arm64") || ua.contains("aarch64")) return "ARM64";
        return "Unknown";
    }

    private String parseManufacturer(String ua, String os) {
        if (os.equals("Mac OS") || os.equals("iOS")) return "Apple";
        if (ua.contains("Samsung") || ua.contains("SM-")) return "Samsung";
        if (ua.contains("Pixel")) return "Google";
        return "Unknown";
    }

    // This record remains exactly the same so your DeviceSessionService does not break
    public record DeviceDetails(
            String deviceName,
            String deviceType,
            String os,
            String osVersion,
            String browser,
            String browserVersion,
            String manufacturer,
            String model,
            String architecture
    ) {}
}