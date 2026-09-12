package com.william.emmabridge;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Main {
    public static void main(String[] args) {
        final boolean captureOnly = args != null && args.length > 0 && "--capture".equals(args[0]);
        SwingUtilities.invokeLater(() -> {
            if (captureOnly) captureOnce();
            else new BridgeFrame().setVisible(true);
        });
    }

    private static void captureOnce() {
        final Config c=Config.load();
        ScreenSelector.start(new ScreenSelector.Listener() {
            public void selected(BufferedImage image) {
                final BufferedImage small=ImageTools.resizeToMaxWidth(image,c.maxWidth);
                final String title="Captura "+new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());
                new Thread(() -> {
                    try { Uploader.send(c,small,title); Toolkit.getDefaultToolkit().beep(); }
                    catch (Exception e) { JOptionPane.showMessageDialog(null,e.getMessage(),"EmmaBridge",JOptionPane.ERROR_MESSAGE); }
                    System.exit(0);
                }).start();
            }
            public void cancelled() { System.exit(0); }
        });
    }
}
