package com.william.emmaboard;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.ArrayList;

public final class EmmaDb extends SQLiteOpenHelper {
    private static final String NAME = "emma.db";
    private static final int VERSION = 2;

    public EmmaDb(Context c) {
        super(c, NAME, null, VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notebooks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL)");
        db.execSQL("CREATE TABLE captures (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, notebook TEXT NOT NULL, path TEXT NOT NULL, created_at INTEGER NOT NULL, return_host TEXT, return_port INTEGER DEFAULT 8766)");
        db.execSQL("INSERT INTO notebooks(name) VALUES('Inbox')");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE captures ADD COLUMN return_host TEXT"); } catch (Throwable ignored) {}
            try { db.execSQL("ALTER TABLE captures ADD COLUMN return_port INTEGER DEFAULT 8766"); } catch (Throwable ignored) {}
        }
    }

    public synchronized void ensureNotebook(String name) {
        if (name == null || name.trim().length() == 0) name = "Inbox";
        name = name.trim();

        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery(
            "SELECT id FROM notebooks WHERE name=? LIMIT 1",
            new String[]{name}
        );

        boolean exists = false;
        try {
            exists = c.moveToFirst();
        } finally {
            c.close();
        }

        if (!exists) {
            ContentValues v = new ContentValues();
            v.put("name", name);
            db.insert("notebooks", null, v);
        }
    }

    public synchronized long addCapture(
        String title,
        String notebook,
        String path,
        long createdAt,
        String returnHost,
        int returnPort
    ) {
        ensureNotebook(notebook);

        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("notebook", notebook);
        v.put("path", path);
        v.put("created_at", createdAt);
        v.put("return_host", returnHost);
        v.put("return_port", returnPort);

        return getWritableDatabase().insert("captures", null, v);
    }

    public synchronized ArrayList<String> listNotebooks() {
        ArrayList<String> out = new ArrayList<String>();
        out.add("Todos");

        Cursor c = getReadableDatabase().rawQuery(
            "SELECT name FROM notebooks ORDER BY CASE WHEN name='Inbox' THEN 0 ELSE 1 END, name COLLATE NOCASE",
            null
        );

        try {
            while (c.moveToNext()) out.add(c.getString(0));
        } finally {
            c.close();
        }

        return out;
    }

    private CaptureItem fromCursor(Cursor c) {
        CaptureItem i = new CaptureItem();
        i.id = c.getLong(0);
        i.title = c.getString(1);
        i.notebook = c.getString(2);
        i.path = c.getString(3);
        i.createdAt = c.getLong(4);
        i.returnHost = c.isNull(5) ? "" : c.getString(5);
        i.returnPort = c.isNull(6) ? 8766 : c.getInt(6);
        if (i.returnPort <= 0) i.returnPort = 8766;
        return i;
    }

    public synchronized ArrayList<CaptureItem> listCaptures(String notebook) {
        ArrayList<CaptureItem> out = new ArrayList<CaptureItem>();
        Cursor c;
        String cols = "id,title,notebook,path,created_at,return_host,return_port";

        if (notebook == null || "Todos".equals(notebook)) {
            c = getReadableDatabase().rawQuery(
                "SELECT " + cols + " FROM captures ORDER BY created_at DESC",
                null
            );
        } else {
            c = getReadableDatabase().rawQuery(
                "SELECT " + cols + " FROM captures WHERE notebook=? ORDER BY created_at DESC",
                new String[]{notebook}
            );
        }

        try {
            while (c.moveToNext()) out.add(fromCursor(c));
        } finally {
            c.close();
        }

        return out;
    }

    public synchronized CaptureItem getCapture(long id) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT id,title,notebook,path,created_at,return_host,return_port FROM captures WHERE id=? LIMIT 1",
            new String[]{String.valueOf(id)}
        );

        try {
            if (c.moveToFirst()) return fromCursor(c);
        } finally {
            c.close();
        }

        return null;
    }

    public synchronized void moveCapture(long id, String notebook) {
        ensureNotebook(notebook);
        ContentValues v = new ContentValues();
        v.put("notebook", notebook);
        getWritableDatabase().update("captures", v, "id=?", new String[]{String.valueOf(id)});
    }

    public synchronized void renameCapture(long id, String title) {
        if (title == null || title.trim().length() == 0) return;
        ContentValues v = new ContentValues();
        v.put("title", title.trim());
        getWritableDatabase().update("captures", v, "id=?", new String[]{String.valueOf(id)});
    }
}
