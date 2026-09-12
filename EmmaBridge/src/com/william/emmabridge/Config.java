package com.william.emmabridge;

import java.io.*;
import java.util.Properties;

public final class Config {
    public String host = "192.168.1.100";
    public int port = 8765;
    public String token = "emma1234";
    public String notebook = "Inbox";
    public int maxWidth = 800;
    public int returnPort = 8766;

    private static File file() {
        return new File(System.getProperty("user.home"), ".emmabridge.properties");
    }

    public static Config load() {
        Config c = new Config();
        File f = file();
        if (!f.exists()) return c;
        try {
            Properties p = new Properties();
            FileInputStream in = new FileInputStream(f);
            p.load(in);
            in.close();
            c.host = p.getProperty("host", c.host);
            c.port = Integer.parseInt(p.getProperty("port", String.valueOf(c.port)));
            c.token = p.getProperty("token", c.token);
            c.notebook = p.getProperty("notebook", c.notebook);
            c.maxWidth = Integer.parseInt(p.getProperty("maxWidth", String.valueOf(c.maxWidth)));
            c.returnPort = Integer.parseInt(p.getProperty("returnPort", String.valueOf(c.returnPort)));
        } catch (Exception ignored) {}
        return c;
    }

    public void save() throws IOException {
        Properties p = new Properties();
        p.setProperty("host", host);
        p.setProperty("port", String.valueOf(port));
        p.setProperty("token", token);
        p.setProperty("notebook", notebook);
        p.setProperty("maxWidth", String.valueOf(maxWidth));
        p.setProperty("returnPort", String.valueOf(returnPort));
        FileOutputStream out = new FileOutputStream(file());
        p.store(out, "EmmaBridge");
        out.close();
    }
}
