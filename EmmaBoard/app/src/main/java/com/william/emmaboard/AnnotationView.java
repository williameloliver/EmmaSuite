package com.william.emmaboard;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

public final class AnnotationView extends View {
    public static final int TOOL_PEN = 0;
    public static final int TOOL_ERASER = 1;
    public static final int TOOL_RECT = 2;
    public static final int TOOL_CIRCLE = 3;
    public static final int TOOL_ARROW = 4;
    public static final int TOOL_TEXT = 5;
    public static final int TOOL_PAN = 6;

    private final Bitmap base;
    private final Paint paint = new Paint();
    private final ArrayList<Mark> marks;
    private final Stack<EditAction> undo = new Stack<EditAction>();
    private final Stack<EditAction> redo = new Stack<EditAction>();

    private Mark current;

    private int tool = TOOL_PEN;
    private int color = Color.RED;
    private float width = 4f;

    private String pendingText = "";
    private float pendingTextSize = 26f;

    private float fitScale = 1f;
    private float zoom = 1f;
    private float baseOffX = 0f;
    private float baseOffY = 0f;
    private float panX = 0f;
    private float panY = 0f;

    private float lastScreenX;
    private float lastScreenY;

    public AnnotationView(Context context, Bitmap base, ArrayList<Mark> marks) {
        super(context);
        this.base = base;
        this.marks = marks;

        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);

        setBackgroundColor(Color.DKGRAY);
    }

    public void setPenColor(int c) {
        color = c;
        tool = TOOL_PEN;
    }

    public void setWidth(float w) {
        width = w;
    }

    public void setTool(int value) {
        tool = value;
    }

    public void setTextTool(String text, float size) {
        pendingText = text == null ? "" : text;
        pendingTextSize = size < 10f ? 10f : size;
        tool = TOOL_TEXT;
    }

    public ArrayList<Mark> getMarks() {
        return marks;
    }

    public void zoomIn() {
        zoom *= 1.25f;
        if (zoom > 4f) zoom = 4f;
        invalidate();
    }

    public void zoomOut() {
        zoom /= 1.25f;
        if (zoom < 0.5f) zoom = 0.5f;
        invalidate();
    }

    public void fit() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
        invalidate();
    }

    public void panBy(float dx, float dy) {
        panX += dx;
        panY += dy;
        invalidate();
    }

    public void clearAll() {
        marks.clear();
        current = null;
        undo.clear();
        redo.clear();
        invalidate();
    }

    protected void onDraw(Canvas c) {
        super.onDraw(c);

        computeTransform(getWidth(), getHeight());

        c.save();
        c.translate(baseOffX + panX, baseOffY + panY);
        c.scale(fitScale * zoom, fitScale * zoom);

        c.drawBitmap(base, 0, 0, null);

        for (int i = 0; i < marks.size(); i++) {
            drawMark(c, marks.get(i));
        }

        if (current != null) drawMark(c, current);

        c.restore();
    }

    private void drawMark(Canvas c, Mark m) {
        paint.setColor(m.color);
        paint.setStrokeWidth(m.width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        if (m.type == Mark.PEN) {
            paint.setStyle(Paint.Style.STROKE);

            if (m.size() == 0) return;

            Path path = new Path();
            path.moveTo(
                m.xs.get(0).floatValue(),
                m.ys.get(0).floatValue()
            );

            for (int i = 1; i < m.size(); i++) {
                path.lineTo(
                    m.xs.get(i).floatValue(),
                    m.ys.get(i).floatValue()
                );
            }

            c.drawPath(path, paint);
            return;
        }

        if (m.type == Mark.TEXT) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(m.textSize);
            c.drawText(m.text == null ? "" : m.text, m.x1, m.y1, paint);
            paint.setStyle(Paint.Style.STROKE);
            return;
        }

        paint.setStyle(Paint.Style.STROKE);

        if (m.type == Mark.RECT) {
            c.drawRect(
                Math.min(m.x1, m.x2),
                Math.min(m.y1, m.y2),
                Math.max(m.x1, m.x2),
                Math.max(m.y1, m.y2),
                paint
            );
            return;
        }

        if (m.type == Mark.CIRCLE) {
            RectF r = new RectF(
                Math.min(m.x1, m.x2),
                Math.min(m.y1, m.y2),
                Math.max(m.x1, m.x2),
                Math.max(m.y1, m.y2)
            );
            c.drawOval(r, paint);
            return;
        }

        if (m.type == Mark.ARROW) {
            drawArrow(c, m);
        }
    }

    private void drawArrow(Canvas c, Mark m) {
        c.drawLine(m.x1, m.y1, m.x2, m.y2, paint);

        float dx = m.x2 - m.x1;
        float dy = m.y2 - m.y1;
        double angle = Math.atan2(dy, dx);

        float head = 16f + m.width * 2f;

        double a1 = angle + Math.PI * 0.82;
        double a2 = angle - Math.PI * 0.82;

        float x3 = m.x2 + (float)Math.cos(a1) * head;
        float y3 = m.y2 + (float)Math.sin(a1) * head;
        float x4 = m.x2 + (float)Math.cos(a2) * head;
        float y4 = m.y2 + (float)Math.sin(a2) * head;

        c.drawLine(m.x2, m.y2, x3, y3, paint);
        c.drawLine(m.x2, m.y2, x4, y4, paint);
    }

    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getAction() & 0xff;

        if (tool == TOOL_PAN) {
            if (action == MotionEvent.ACTION_DOWN) {
                lastScreenX = e.getX();
                lastScreenY = e.getY();
                return true;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                float dx = e.getX() - lastScreenX;
                float dy = e.getY() - lastScreenY;

                panBy(dx, dy);

                lastScreenX = e.getX();
                lastScreenY = e.getY();
                return true;
            }

            return true;
        }

        float totalScale = fitScale * zoom;
        float x = (e.getX() - baseOffX - panX) / totalScale;
        float y = (e.getY() - baseOffY - panY) / totalScale;

        if (x < 0 || y < 0 || x >= base.getWidth() || y >= base.getHeight()) {
            return true;
        }

        if (tool == TOOL_ERASER) {
            if (action == MotionEvent.ACTION_DOWN) eraseNearest(x, y);
            return true;
        }

        if (tool == TOOL_TEXT) {
            if (action == MotionEvent.ACTION_DOWN && pendingText.length() > 0) {
                Mark m = new Mark(Mark.TEXT, color, width);
                m.x1 = x;
                m.y1 = y;
                m.text = pendingText;
                m.textSize = pendingTextSize;

                marks.add(m);
                undo.push(new EditAction(true, m, marks.size() - 1));
                redo.clear();

                invalidate();
                tool = TOOL_PEN;
            }

            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            if (tool == TOOL_PEN) {
                current = new Mark(Mark.PEN, color, width);
                current.add(x, y);
            } else {
                int type =
                    tool == TOOL_RECT ? Mark.RECT :
                    tool == TOOL_CIRCLE ? Mark.CIRCLE :
                    Mark.ARROW;

                current = new Mark(type, color, width);
                current.x1 = x;
                current.y1 = y;
                current.x2 = x;
                current.y2 = y;
            }

            invalidate();
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE && current != null) {
            if (current.type == Mark.PEN) {
                current.add(x, y);
            } else {
                current.x2 = x;
                current.y2 = y;
            }

            invalidate();
            return true;
        }

        if (action == MotionEvent.ACTION_UP && current != null) {
            if (current.type == Mark.PEN) {
                current.add(x, y);
            } else {
                current.x2 = x;
                current.y2 = y;
            }

            marks.add(current);
            undo.push(new EditAction(true, current, marks.size() - 1));
            redo.clear();

            current = null;
            invalidate();
            return true;
        }

        return true;
    }

    private boolean hit(Mark m, float x, float y, float radius) {
        if (m.type == Mark.PEN) {
            for (int p = 0; p < m.size(); p++) {
                float dx = m.xs.get(p).floatValue() - x;
                float dy = m.ys.get(p).floatValue() - y;

                if (dx * dx + dy * dy <= radius * radius) return true;
            }

            return false;
        }

        if (m.type == Mark.TEXT) {
            float textWidth = Math.max(
                40f,
                (m.text == null ? 0 : m.text.length()) * m.textSize * 0.55f
            );

            return
                x >= m.x1 - radius &&
                x <= m.x1 + textWidth + radius &&
                y >= m.y1 - m.textSize - radius &&
                y <= m.y1 + radius;
        }

        float left = Math.min(m.x1, m.x2) - radius;
        float top = Math.min(m.y1, m.y2) - radius;
        float right = Math.max(m.x1, m.x2) + radius;
        float bottom = Math.max(m.y1, m.y2) + radius;

        return x >= left && x <= right && y >= top && y <= bottom;
    }

    private void eraseNearest(float x, float y) {
        float radius = 26f / Math.max(0.5f, fitScale * zoom);

        for (int i = marks.size() - 1; i >= 0; i--) {
            Mark m = marks.get(i);

            if (hit(m, x, y, radius)) {
                marks.remove(i);
                undo.push(new EditAction(false, m, i));
                redo.clear();
                invalidate();
                return;
            }
        }
    }

    public void undo() {
        if (undo.empty()) return;

        EditAction a = undo.pop();

        if (a.added) {
            marks.remove(a.mark);
        } else {
            marks.add(Math.min(a.index, marks.size()), a.mark);
        }

        redo.push(a);
        invalidate();
    }

    public void redo() {
        if (redo.empty()) return;

        EditAction a = redo.pop();

        if (a.added) {
            marks.add(Math.min(a.index, marks.size()), a.mark);
        } else {
            marks.remove(a.mark);
        }

        undo.push(a);
        invalidate();
    }

    public Bitmap renderMerged() {
        Bitmap out = Bitmap.createBitmap(
            base.getWidth(),
            base.getHeight(),
            Bitmap.Config.ARGB_8888
        );

        Canvas c = new Canvas(out);
        c.drawBitmap(base, 0, 0, null);

        for (int i = 0; i < marks.size(); i++) {
            drawMark(c, marks.get(i));
        }

        return out;
    }

    private void computeTransform(int w, int h) {
        if (w <= 0 || h <= 0) return;

        float sx = w / (float)base.getWidth();
        float sy = h / (float)base.getHeight();

        fitScale = Math.min(sx, sy);

        baseOffX = (w - base.getWidth() * fitScale) / 2f;
        baseOffY = (h - base.getHeight() * fitScale) / 2f;
    }

    private static final class EditAction {
        final boolean added;
        final Mark mark;
        final int index;

        EditAction(boolean added, Mark mark, int index) {
            this.added = added;
            this.mark = mark;
            this.index = index;
        }
    }
}
