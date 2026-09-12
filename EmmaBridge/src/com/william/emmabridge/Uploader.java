package com.william.emmabridge;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

public final class Uploader {
    private Uploader() {}

    public static String ping(Config c) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)new URL(
            "http://" + c.host + ":" + c.port + "/ping").openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Token", c.token);
        int code = conn.getResponseCode();
        String text = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        conn.disconnect();
        return code + " " + text;
    }

    public static void send(Config c, BufferedImage image, String title) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        byte[] data = png.toByteArray();

        HttpURLConnection conn = (HttpURLConnection)new URL(
            "http://" + c.host + ":" + c.port + "/capture").openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "image/png");
        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
        conn.setRequestProperty("X-Token", c.token);
        conn.setRequestProperty("X-Title", URLEncoder.encode(title, "UTF-8"));
        conn.setRequestProperty("X-Notebook", URLEncoder.encode(c.notebook, "UTF-8"));
        conn.setRequestProperty("X-Return-Host", NetworkUtil.localAddressFor(c.host, c.port));
        conn.setRequestProperty("X-Return-Port", String.valueOf(c.returnPort));

        OutputStream out = conn.getOutputStream();
        out.write(data);
        out.flush();
        out.close();

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String err = readAll(conn.getErrorStream());
            conn.disconnect();
            throw new IOException("HTTP " + code + ": " + err);
        }
        conn.disconnect();
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder b = new StringBuilder();
        String s;
        while ((s = r.readLine()) != null) b.append(s).append('\n');
        r.close();
        return b.toString().trim();
    }
}
