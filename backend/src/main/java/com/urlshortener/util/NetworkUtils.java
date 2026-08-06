package com.urlshortener.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetworkUtils {

    private static volatile String cachedLanIp = null;

    public static String getLocalLanIp() {
        if (cachedLanIp != null) {
            return cachedLanIp;
        }
        synchronized (NetworkUtils.class) {
            if (cachedLanIp != null) {
                return cachedLanIp;
            }
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces != null && interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
                        continue;
                    }
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress() && addr.getHostAddress().indexOf(':') < 0) {
                            cachedLanIp = addr.getHostAddress();
                            log.info("Detected LAN IP address for scannable QR codes: {}", cachedLanIp);
                            return cachedLanIp;
                        }
                    }
                }
                InetAddress localHost = InetAddress.getLocalHost();
                if (!localHost.isLoopbackAddress()) {
                    cachedLanIp = localHost.getHostAddress();
                    return cachedLanIp;
                }
            } catch (Exception e) {
                log.warn("Could not determine local LAN IP address, falling back to 127.0.0.1", e);
            }
            cachedLanIp = "127.0.0.1";
            return cachedLanIp;
        }
    }

    public static void clearCache() {
        cachedLanIp = null;
    }
}
