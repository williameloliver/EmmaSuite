package com.william.emmabridge;

import java.net.*;

public final class NetworkUtil {
    private NetworkUtil() {}

    public static String localAddressFor(String remoteHost, int remotePort) {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
            socket.connect(
                InetAddress.getByName(remoteHost),
                remotePort
            );

            InetAddress local = socket.getLocalAddress();

            if (local != null) {
                String ip = local.getHostAddress();
                if (ip != null && ip.length() > 0) return ip;
            }

        } catch (Exception ignored) {
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }
}
