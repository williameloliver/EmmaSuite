package com.william.emmaboard;

public final class CaptureItem {
    public long id;
    public String title;
    public String notebook;
    public String path;
    public long createdAt;
    public String returnHost;
    public int returnPort;

    public String toString() {
        return title + "  [" + notebook + "]";
    }
}
