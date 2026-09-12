package com.william.emmaboard;

import java.io.*;
import java.util.ArrayList;

public final class AnnotationStore {
    private static final String MAGIC_V2 = "EMMAANNO2";
    private static final String MAGIC_V1 = "EMMAANNO1";

    private AnnotationStore() {}

    public static void save(File file, ArrayList<Mark> marks) throws IOException {
        DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(file))
        );

        out.writeUTF(MAGIC_V2);
        out.writeInt(marks.size());

        for (int i = 0; i < marks.size(); i++) {
            Mark m = marks.get(i);
            out.writeInt(m.type);
            out.writeInt(m.color);
            out.writeFloat(m.width);

            if (m.type == Mark.PEN) {
                out.writeInt(m.size());
                for (int p = 0; p < m.size(); p++) {
                    out.writeFloat(m.xs.get(p).floatValue());
                    out.writeFloat(m.ys.get(p).floatValue());
                }
            } else if (m.type == Mark.TEXT) {
                out.writeFloat(m.x1);
                out.writeFloat(m.y1);
                out.writeFloat(m.textSize);
                out.writeUTF(m.text == null ? "" : m.text);
            } else {
                out.writeFloat(m.x1);
                out.writeFloat(m.y1);
                out.writeFloat(m.x2);
                out.writeFloat(m.y2);
            }
        }

        out.close();
    }

    public static ArrayList<Mark> load(File file) {
        ArrayList<Mark> result = new ArrayList<Mark>();
        if (!file.exists()) return result;

        try {
            DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file))
            );

            String magic = in.readUTF();

            if (MAGIC_V1.equals(magic)) {
                int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    Mark m = new Mark(Mark.PEN, in.readInt(), in.readFloat());
                    int points = in.readInt();
                    for (int p = 0; p < points; p++) {
                        m.add(in.readFloat(), in.readFloat());
                    }
                    result.add(m);
                }
                in.close();
                return result;
            }

            if (!MAGIC_V2.equals(magic)) {
                in.close();
                return result;
            }

            int count = in.readInt();

            for (int i = 0; i < count; i++) {
                int type = in.readInt();
                Mark m = new Mark(type, in.readInt(), in.readFloat());

                if (type == Mark.PEN) {
                    int points = in.readInt();
                    for (int p = 0; p < points; p++) {
                        m.add(in.readFloat(), in.readFloat());
                    }
                } else if (type == Mark.TEXT) {
                    m.x1 = in.readFloat();
                    m.y1 = in.readFloat();
                    m.textSize = in.readFloat();
                    m.text = in.readUTF();
                } else {
                    m.x1 = in.readFloat();
                    m.y1 = in.readFloat();
                    m.x2 = in.readFloat();
                    m.y2 = in.readFloat();
                }

                result.add(m);
            }

            in.close();
        } catch (Throwable ignored) {}

        return result;
    }
}
