package com.william.emmaboard;

import android.graphics.Bitmap;
import java.io.*;
import java.net.*;

public final class ReturnClient {
    private ReturnClient() {}

    public static String send(
        String host,
        int port,
        String token,
        String title,
        Bitmap bitmap
    ) throws IOException {

        if (host == null || host.trim().length() == 0) {
            throw new IOException("Esta captura no tiene IP de retorno.");
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, png);
        byte[] data = png.toByteArray();

        URL url = new URL("http://" + host + ":" + port + "/return");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();

        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        conn.setRequestProperty("Content-Type", "image/png");
        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
        conn.setRequestProperty("X-Token", token == null ? "" : token);
        conn.setRequestProperty(
            "X-Title",
            URLEncoder.encode(title == null ? "EmmaBoard" : title, "UTF-8")
        );

        OutputStream out = conn.getOutputStream();
        out.write(data);
        out.flush();
        out.close();

        int code = conn.getResponseCode();

        if (code < 200 || code >= 300) {
            throw new IOException("PC respondio HTTP " + code);
        }

        conn.disconnect();
        return "OK";
    }
}
