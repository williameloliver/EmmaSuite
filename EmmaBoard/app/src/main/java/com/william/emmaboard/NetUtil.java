package com.william.emmaboard;

import java.net.*;
import java.util.Enumeration;

public final class NetUtil {
    public static String localIp() {
        try {
            Enumeration<NetworkInterface> ns = NetworkInterface.getNetworkInterfaces();
            while (ns != null && ns.hasMoreElements()) {
                NetworkInterface n = ns.nextElement();
                Enumeration<InetAddress> as = n.getInetAddresses();
                while (as.hasMoreElements()) {
                    InetAddress a = as.nextElement();
                    if (!a.isLoopbackAddress() && a instanceof Inet4Address)
                        return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "sin Wi-Fi";
    }
}
