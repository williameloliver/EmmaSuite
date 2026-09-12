package com.william.emmaboard;

import android.graphics.*;
import java.io.*;

public final class ThumbnailUtil {
    private ThumbnailUtil() {}

    public static File thumbFor(String originalPath) {
        return new File(originalPath + ".thumb.jpg");
    }

    public static void ensure(String originalPath) {
        File thumb = thumbFor(originalPath);
        if (thumb.exists()) return;

        Bitmap source = null;
        Bitmap scaled = null;

        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(originalPath, bounds);

            int sample = 1;
            while ((bounds.outWidth / sample) > 320 || (bounds.outHeight / sample) > 240) {
                sample *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            source = BitmapFactory.decodeFile(originalPath, options);
            if (source == null) return;

            float sx = 180f / source.getWidth();
            float sy = 110f / source.getHeight();
            float s = Math.min(sx, sy);
            if (s > 1f) s = 1f;

            int w = Math.max(1, Math.round(source.getWidth() * s));
            int h = Math.max(1, Math.round(source.getHeight() * s));

            scaled = Bitmap.createScaledBitmap(source, w, h, true);

            FileOutputStream out = new FileOutputStream(thumb);
            scaled.compress(Bitmap.CompressFormat.JPEG, 65, out);
            out.flush();
            out.close();

        } catch (Throwable ignored) {
        } finally {
            if (scaled != null && scaled != source) {
                try { scaled.recycle(); } catch (Throwable ignored) {}
            }
            if (source != null) {
                try { source.recycle(); } catch (Throwable ignored) {}
            }
        }
    }
}
