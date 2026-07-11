package com.backend.StockLinker.AuthService.filter;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeviceParserService {

    private final UserAgentAnalyzer analyzer;

    public DeviceParserService() {
        this.analyzer = UserAgentAnalyzer.newBuilder()
                .hideMatcherLoadStats()
                .withCache(10000)
                .build();
    }

    public DeviceDetails parse(String userAgentString) {
        if (userAgentString == null || userAgentString.isBlank()) {
            return defaultDevice();
        }

        UserAgent agent = analyzer.parse(userAgentString);

        return new DeviceDetails(
                extractValue(agent, UserAgent.DEVICE_NAME, "Unknown Device"),
                extractValue(agent, UserAgent.DEVICE_CLASS, "UNKNOWN"),
                extractValue(agent, UserAgent.OPERATING_SYSTEM_NAME, "Unknown"),
                extractValue(agent, UserAgent.OPERATING_SYSTEM_VERSION, "Unknown"),
                extractValue(agent, UserAgent.AGENT_NAME, "Unknown"),
                extractValue(agent, UserAgent.AGENT_VERSION, "Unknown"),
                extractValue(agent, UserAgent.DEVICE_BRAND, "Unknown"),
                extractValue(agent, UserAgent.DEVICE_NAME, "Unknown"),
                extractValue(agent, "DeviceCpu", "Unknown") // YAUAA fallback for architecture
        );
    }

    private String extractValue(UserAgent agent, String fieldName, String fallback) {
        return Optional.ofNullable(agent.getValue(fieldName))
                .filter(val -> !val.equalsIgnoreCase("Unknown"))
                .orElse(fallback);
    }

    private DeviceDetails defaultDevice() {
        return new DeviceDetails(
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
    }

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