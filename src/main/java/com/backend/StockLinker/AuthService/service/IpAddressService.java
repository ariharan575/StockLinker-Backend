package com.backend.StockLinker.AuthService.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class IpAddressService {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        for (String header : IP_HEADERS) {
            String ipList = request.getHeader(header);

            if (ipList != null && !ipList.isBlank() && !"unknown".equalsIgnoreCase(ipList)) {
                // Return the first IP in the list (the original client)
                String ip = ipList.split(",")[0].trim();
                return normalizeLocalhost(ip);
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null && !remoteAddr.isBlank() ? normalizeLocalhost(remoteAddr) : "Unknown";
    }

    private String normalizeLocalhost(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}