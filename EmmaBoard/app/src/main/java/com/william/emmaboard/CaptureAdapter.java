package com.william.emmaboard;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.ArrayList;

public final class CaptureAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<CaptureItem> items;

    public CaptureAdapter(Context context, ArrayList<CaptureItem> items) {
        this.context = context;
        this.items = items;
    }

    public int getCount() { return items.size(); }
    public Object getItem(int position) { return items.get(position); }
    public long getItemId(int position) { return items.get(position).id; }

    public CaptureItem itemAt(int position) {
        return items.get(position);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;

        if (convertView == null) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(6, 5, 6, 5);

            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            TextView text = new TextView(context);
            text.setTextSize(17f);
            text.setPadding(10, 4, 4, 4);
            text.setGravity(Gravity.CENTER_VERTICAL);

            row.addView(image, new LinearLayout.LayoutParams(180, 110));
            row.addView(text, new LinearLayout.LayoutParams(0, 110, 1f));

            h = new Holder();
            h.image = image;
            h.text = text;
            row.setTag(h);
            convertView = row;
        } else {
            h = (Holder)convertView.getTag();
        }

        CaptureItem item = items.get(position);
        h.text.setText(item.title + "\n" + item.notebook);

        try {
            ThumbnailUtil.ensure(item.path);
            File thumb = ThumbnailUtil.thumbFor(item.path);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap b = BitmapFactory.decodeFile(thumb.getAbsolutePath(), o);
            h.image.setImageBitmap(b);
        } catch (Throwable e) {
            h.image.setImageDrawable(null);
        }

        return convertView;
    }

    private static final class Holder {
        ImageView image;
        TextView text;
    }
}
