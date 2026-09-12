package com.william.emmaboard;

import java.util.ArrayList;

public final class Mark {
    public static final int PEN = 0;
    public static final int RECT = 1;
    public static final int CIRCLE = 2;
    public static final int ARROW = 3;
    public static final int TEXT = 4;

    public int type;
    public int color;
    public float width;
    public final ArrayList<Float> xs = new ArrayList<Float>();
    public final ArrayList<Float> ys = new ArrayList<Float>();
    public float x1;
    public float y1;
    public float x2;
    public float y2;
    public String text = "";
    public float textSize = 24f;

    public Mark(int type, int color, float width) {
        this.type = type;
        this.color = color;
        this.width = width;
    }

    public void add(float x, float y) {
        xs.add(Float.valueOf(x));
        ys.add(Float.valueOf(y));
    }

    public int size() {
        return xs.size();
    }
}
