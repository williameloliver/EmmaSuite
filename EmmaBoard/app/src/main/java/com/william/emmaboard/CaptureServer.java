package com.william.emmaboard;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.util.*;

public final class CaptureServer {
    public interface Listener {
        void onCaptureReceived(String notebook, String title);
        void onServerError(String message);
    }

    private final Context context;
    private final EmmaDb db;
    private final int port;
    private final String token;
    private final Listener listener;

    private volatile boolean running;
    private ServerSocket socket;
    private Thread thread;

    public CaptureServer(Context c, EmmaDb db, int port, String token, Listener listener) {
        this.context = c.getApplicationContext();
        this.db = db;
        this.port = port;
        this.token = token == null ? "" : token;
        this.listener = listener;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(new Runnable() {
            public void run() { serve(); }
        }, "emma-http");
        thread.start();
    }

    public synchronized void stopAndWait() {
        running = false;
        try { if (socket != null) socket.close(); } catch (Throwable ignored) {}

        Thread t = thread;
        if (t != null && t != Thread.currentThread()) {
            try { t.join(1500); } catch (InterruptedException ignored) {}
        }

        thread = null;
        socket = null;
    }

    private void serve() {
        try {
            ServerSocket s = new ServerSocket();
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(port));
            socket = s;

            while (running) {
                Socket client = s.accept();
                handle(client);
            }
        } catch (Throwable e) {
            if (running && listener != null) {
                try { listener.onServerError(String.valueOf(e)); } catch (Throwable ignored) {}
            }
        } finally {
            try { if (socket != null) socket.close(); } catch (Throwable ignored) {}
            socket = null;
            running = false;
        }
    }

    private void handle(Socket client) {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        try {
            client.setSoTimeout(10000);
            in = new BufferedInputStream(client.getInputStream());
            out = new BufferedOutputStream(client.getOutputStream());

            String firstLine = readLine(in);
            if (firstLine == null) return;

            String[] first = firstLine.split(" ");
            String method = first.length > 0 ? first[0] : "";
            String path = first.length > 1 ? first[1] : "";

            HashMap<String,String> h = new HashMap<String,String>();

            while (true) {
                String line = readLine(in);
                if (line == null || line.length() == 0) break;

                int p = line.indexOf(':');
                if (p > 0) {
                    h.put(
                        line.substring(0, p).trim().toLowerCase(Locale.US),
                        line.substring(p + 1).trim()
                    );
                }
            }

            if (!token.equals(h.get("x-token"))) {
                reply(out, 403, "Forbidden");
                return;
            }

            if ("GET".equals(method) && "/ping".equals(path)) {
                reply(out, 200, "EmmaBoard OK");
                return;
            }

            if (!"POST".equals(method) || !"/capture".equals(path)) {
                reply(out, 404, "Not found");
                return;
            }

            int length = parseInt(h.get("content-length"), -1);
            if (length < 1 || length > 10 * 1024 * 1024) {
                reply(out, 400, "Invalid Content-Length");
                return;
            }

            String title = decode(h.get("x-title"));
            String notebook = decode(h.get("x-notebook"));
            String returnHost = h.get("x-return-host");
            int returnPort = parseInt(h.get("x-return-port"), 8766);

            if (title.length() == 0) title = "Captura";
            if (notebook.length() == 0) notebook = "Inbox";
            if (returnHost == null) returnHost = "";

            File dir = new File(context.getFilesDir(), "captures");
            if (!dir.exists()) dir.mkdirs();

            long now = System.currentTimeMillis();
            File file = new File(dir, "capture_" + now + ".png");

            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[8192];
            int left = length;

            while (left > 0) {
                int n = in.read(buf, 0, Math.min(buf.length, left));
                if (n < 0) throw new EOFException("Captura incompleta");
                fos.write(buf, 0, n);
                left -= n;
            }

            fos.flush();
            fos.close();

            ThumbnailUtil.ensure(file.getAbsolutePath());

            db.addCapture(
                title,
                notebook,
                file.getAbsolutePath(),
                now,
                returnHost,
                returnPort
            );

            reply(out, 200, "Saved");

            if (listener != null) {
                try { listener.onCaptureReceived(notebook, title); } catch (Throwable ignored) {}
            }

        } catch (Throwable e) {
            try {
                if (out != null) reply(out, 500, String.valueOf(e));
            } catch (Throwable ignored) {}

            if (listener != null) {
                try { listener.onServerError("Recepcion: " + String.valueOf(e)); } catch (Throwable ignored) {}
            }
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
            try { client.close(); } catch (Throwable ignored) {}
        }
    }

    private static int parseInt(String s, int d) {
        try { return Integer.parseInt(s); } catch (Throwable e) { return d; }
    }

    private static String decode(String s) {
        if (s == null) return "";
        try { return URLDecoder.decode(s, "UTF-8"); } catch (Throwable e) { return s; }
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int prev = -1;

        while (true) {
            int c = in.read();

            if (c < 0) {
                if (b.size() == 0) return null;
                return new String(b.toByteArray(), "ISO-8859-1");
            }

            if (prev == '\r' && c == '\n') {
                byte[] raw = b.toByteArray();
                int len = raw.length > 0 && raw[raw.length - 1] == '\r'
                    ? raw.length - 1
                    : raw.length;

                return new String(raw, 0, len, "ISO-8859-1");
            }

            b.write(c);
            prev = c;

            if (b.size() > 8192) throw new IOException("Header too long");
        }
    }

    private static void reply(OutputStream out, int code, String body) throws IOException {
        byte[] data = body.getBytes("UTF-8");

        String status =
            code == 200 ? "OK" :
            code == 403 ? "Forbidden" :
            code == 404 ? "Not Found" :
            "Error";

        String headers =
            "HTTP/1.1 " + code + " " + status + "\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n" +
            "Content-Length: " + data.length + "\r\n" +
            "Connection: close\r\n\r\n";

        out.write(headers.getBytes("ISO-8859-1"));
        out.write(data);
        out.flush();
    }
}
