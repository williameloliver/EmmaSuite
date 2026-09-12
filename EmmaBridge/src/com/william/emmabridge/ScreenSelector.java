package com.william.emmabridge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public final class ScreenSelector {
    public interface Listener {
        void selected(BufferedImage image);
        void cancelled();
    }

    public static void start(final Listener listener) {
        try {
            final Rectangle bounds = virtualBounds();
            final BufferedImage shot = new Robot().createScreenCapture(bounds);
            final JWindow win = new JWindow();
            win.setAlwaysOnTop(true);
            win.setBounds(bounds);

            JPanel panel = new JPanel() {
                Point start, current;

                {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    MouseAdapter m = new MouseAdapter() {
                        public void mousePressed(MouseEvent e) {
                            start = e.getPoint(); current = start; repaint();
                        }
                        public void mouseDragged(MouseEvent e) {
                            current = e.getPoint(); repaint();
                        }
                        public void mouseReleased(MouseEvent e) {
                            current = e.getPoint();
                            Rectangle r = rect();
                            win.dispose();
                            if (r.width < 4 || r.height < 4) { listener.cancelled(); return; }
                            BufferedImage crop = shot.getSubimage(r.x, r.y, r.width, r.height);
                            BufferedImage copy = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
                            Graphics2D g = copy.createGraphics();
                            g.drawImage(crop, 0, 0, null);
                            g.dispose();
                            listener.selected(copy);
                        }
                        private Rectangle rect() {
                            int x = Math.min(start.x, current.x);
                            int y = Math.min(start.y, current.y);
                            return new Rectangle(x, y, Math.abs(start.x-current.x), Math.abs(start.y-current.y));
                        }
                    };
                    addMouseListener(m);
                    addMouseMotionListener(m);
                    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"cancel");
                    getActionMap().put("cancel", new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            win.dispose(); listener.cancelled();
                        }
                    });
                }

                protected void paintComponent(Graphics gg) {
                    Graphics2D g = (Graphics2D)gg.create();
                    g.drawImage(shot,0,0,null);
                    g.setColor(new Color(0,0,0,110));
                    g.fillRect(0,0,getWidth(),getHeight());
                    if (start != null && current != null) {
                        int x=Math.min(start.x,current.x), y=Math.min(start.y,current.y);
                        int w=Math.abs(start.x-current.x), h=Math.abs(start.y-current.y);
                        Shape old=g.getClip();
                        g.setClip(x,y,w,h);
                        g.drawImage(shot,0,0,null);
                        g.setClip(old);
                        g.setColor(Color.WHITE);
                        g.setStroke(new BasicStroke(2f));
                        g.drawRect(x,y,w,h);
                    }
                    g.dispose();
                }
            };
            win.setContentPane(panel);
            win.setVisible(true);
            win.requestFocus();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,e.toString(),"EmmaBridge",JOptionPane.ERROR_MESSAGE);
            listener.cancelled();
        }
    }

    private static Rectangle virtualBounds() {
        Rectangle all = new Rectangle();
        GraphicsDevice[] ds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for (int i=0;i<ds.length;i++) {
            GraphicsConfiguration[] cs = ds[i].getConfigurations();
            for (int j=0;j<cs.length;j++) all = all.union(cs[j].getBounds());
        }
        return all;
    }
}
