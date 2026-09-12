package com.william.emmabridge;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class ImageTools {
    private ImageTools() {}

    public static BufferedImage resizeToMaxWidth(BufferedImage src, int maxWidth) {
        if (maxWidth <= 0 || src.getWidth() <= maxWidth) return src;
        int w = maxWidth;
        int h = Math.max(1, (int)Math.round(src.getHeight() * (w / (double)src.getWidth())));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
