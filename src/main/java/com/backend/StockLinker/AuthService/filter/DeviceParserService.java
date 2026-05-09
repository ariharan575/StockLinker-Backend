package com.backend.StockLinker.AuthService.filter;

import org.springframework.stereotype.Service;

@Service
public class DeviceParserService {

    public DeviceDetails parse(String userAgent) {

        if (userAgent == null || userAgent.isBlank()) {
            return defaultDevice();
        }

        String ua = userAgent.toLowerCase();

        String os = detectOS(ua);
        String browser = detectBrowser(ua);
        String type = detectType(ua);
        String name = buildName(os, browser);

        return new DeviceDetails(name, type, os, browser);
    }

    // ================= OS =================
    private String detectOS(String ua) {

        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os") || ua.contains("macintosh")) return "MacOS";
        if (ua.contains("linux")) return "Linux";

        return "Unknown";
    }

    // ================= BROWSER =================
    private String detectBrowser(String ua) {

        if (ua.contains("chrome") && !ua.contains("edg")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("edg")) return "Edge";

        return "Unknown";
    }

    // ================= TYPE =================
    private String detectType(String ua) {

        if (ua.contains("mobile")) return "MOBILE";
        if (ua.contains("tablet") || ua.contains("ipad")) return "TABLET";

        return "DESKTOP";
    }

    // ================= NAME =================
    private String buildName(String os, String browser) {

        if ("Unknown".equals(os) && "Unknown".equals(browser)) {
            return "Generic Device";
        }

        return os + " - " + browser;
    }

    private DeviceDetails defaultDevice() {
        return new DeviceDetails("Generic Device", "UNKNOWN", "Unknown", "Unknown");
    }

    public record DeviceDetails(
            String deviceName,
            String deviceType,
            String os,
            String browser
    ) {}
}