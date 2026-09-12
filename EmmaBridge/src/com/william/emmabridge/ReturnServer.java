package com.william.emmabridge;

import java.io.*;
import java.net.*;
import java.util.*;

public final class ReturnServer {
    public interface Listener {
        void saved(File file);
        void error(String message);
    }

    private final int port;
    private final String token;
    private final Listener listener;

    private volatile boolean running;
    private ServerSocket socket;
    private Thread thread;

    public ReturnServer(int port, String token, Listener listener) {
        this.port = port;
        this.token = token == null ? "" : token;
        this.listener = listener;
    }

    public synchronized void start() {
        if (running) return;

        running = true;

        thread = new Thread(new Runnable() {
            public void run() {
                serve();
            }
        }, "emma-pc-return");

        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;

        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }

    public File folder() {
        File dir = new File(
            System.getProperty("user.home"),
            "EmmaBoard_Recibidas"
        );

        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void serve() {
        try {
            socket = new ServerSocket(port);
            socket.setReuseAddress(true);

            while (running) {
                Socket client = socket.accept();
                handle(client);
            }

        } catch (Exception e) {
            if (running && listener != null) {
                listener.error(e.getMessage());
            }
        }
    }

    private void handle(Socket client) {
        InputStream in = null;
        OutputStream out = null;

        try {
            client.setSoTimeout(10000);

            in = new BufferedInputStream(client.getInputStream());
            out = new BufferedOutputStream(client.getOutputStream());

            String first = readLine(in);
            if (first == null) return;

            String[] parts = first.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "";

            HashMap<String,String> headers = new HashMap<String,String>();

            while (true) {
                String line = readLine(in);
                if (line == null || line.length() == 0) break;

                int colon = line.indexOf(':');

                if (colon > 0) {
                    headers.put(
                        line.substring(0, colon).trim().toLowerCase(Locale.US),
                        line.substring(colon + 1).trim()
                    );
                }
            }

            if (!"POST".equals(method) || !"/return".equals(path)) {
                reply(out, 404, "Not found");
                return;
            }

            if (!token.equals(headers.get("x-token"))) {
                reply(out, 403, "Forbidden");
                return;
            }

            int length = parseInt(headers.get("content-length"), -1);

            if (length < 1 || length > 20 * 1024 * 1024) {
                reply(out, 400, "Bad length");
                return;
            }

            String title = decode(headers.get("x-title"));
            if (title.length() == 0) title = "EmmaBoard";

            File dir = folder();
            String stamp = new java.text.SimpleDateFormat(
                "yyyyMMdd_HHmmss"
            ).format(new java.util.Date());

            File file = new File(
                dir,
                safe(title) + "_" + stamp + ".png"
            );

            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[8192];
            int left = length;

            while (left > 0) {
                int n = in.read(
                    buf,
                    0,
                    Math.min(buf.length, left)
                );

                if (n < 0) throw new EOFException("Imagen incompleta");

                fos.write(buf, 0, n);
                left -= n;
            }

            fos.flush();
            fos.close();

            reply(out, 200, "Saved");

            if (listener != null) listener.saved(file);

        } catch (Exception e) {
            try {
                if (out != null) reply(out, 500, e.toString());
            } catch (Exception ignored) {}

            if (listener != null) listener.error(e.getMessage());

        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private static String safe(String s) {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (
                Character.isLetterOrDigit(c) ||
                c == '-' ||
                c == '_' ||
                c == ' '
            ) {
                b.append(c);
            } else {
                b.append('_');
            }
        }

        String out = b.toString().trim();
        return out.length() == 0 ? "EmmaBoard" : out;
    }

    private static int parseInt(String s, int d) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return d; }
    }

    private static String decode(String s) {
        if (s == null) return "";

        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
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

                int len =
                    raw.length > 0 && raw[raw.length - 1] == '\r'
                        ? raw.length - 1
                        : raw.length;

                return new String(
                    raw,
                    0,
                    len,
                    "ISO-8859-1"
                );
            }

            b.write(c);
            prev = c;

            if (b.size() > 8192) {
                throw new IOException("Header demasiado largo");
            }
        }
    }

    private static void reply(
        OutputStream out,
        int code,
        String body
    ) throws IOException {

        byte[] data = body.getBytes("UTF-8");

        String status =
            code == 200 ? "OK" :
            code == 403 ? "Forbidden" :
            code == 404 ? "Not Found" :
            "Error";

        String h =
            "HTTP/1.1 " + code + " " + status + "\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n" +
            "Content-Length: " + data.length + "\r\n" +
            "Connection: close\r\n\r\n";

        out.write(h.getBytes("ISO-8859-1"));
        out.write(data);
        out.flush();
    }
}
