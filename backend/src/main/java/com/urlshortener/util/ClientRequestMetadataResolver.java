package com.urlshortener.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientRequestMetadataResolver {

    public ClientRequestMetadata resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ipAddress = forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        String userAgent = request.getHeader("User-Agent");
        String browser = detectBrowser(userAgent);
        String operatingSystem = detectOperatingSystem(userAgent);
        String deviceType = detectDeviceType(userAgent);
        String country = resolveCountry(request, ipAddress);
        String referrer = request.getHeader("Referer");
        return new ClientRequestMetadata(ipAddress, userAgent, browser, operatingSystem, deviceType, country, referrer);
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        if (userAgent.contains("Edg")) {
            return "Edge";
        }
        if (userAgent.contains("Chrome")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari")) {
            return "Safari";
        }
        return "Other";
    }

    private String detectOperatingSystem(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Mac OS")) {
            return "macOS";
        }
        if (userAgent.contains("Android")) {
            return "Android";
        }
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "Other";
    }

    private String detectDeviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        String normalized = userAgent.toLowerCase();
        if (normalized.contains("bot") || normalized.contains("crawler") || normalized.contains("spider")) {
            return "Bot";
        }
        if (normalized.contains("ipad") || normalized.contains("tablet")) {
            return "Tablet";
        }
        if (normalized.contains("mobile") || normalized.contains("android") || normalized.contains("iphone")) {
            return "Mobile";
        }
        return "Desktop";
    }

    private String resolveCountry(HttpServletRequest request, String ipAddress) {
        String[] headerCandidates = {"CF-IPCountry", "X-AppEngine-Country", "X-Country-Code"};
        for (String header : headerCandidates) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        if (ipAddress == null) {
            return "Unknown";
        }
        if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.") || ipAddress.startsWith("127.") || ipAddress.startsWith("172.")) {
            return "Local";
        }
        return "Unknown";
    }
}
