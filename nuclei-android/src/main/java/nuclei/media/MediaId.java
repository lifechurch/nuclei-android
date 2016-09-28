package nuclei.media;

import android.net.Uri;

public abstract class MediaId {

    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_AUDIO = 2;

    public final Uri uri;
    public final int type;
    public final boolean queue;

    public MediaId(Uri uri) {
        this.uri = uri;
        this.queue = Boolean.parseBoolean(uri.getQueryParameter("_queue"));
        this.type = getInt("_type", -1);
    }

    protected int getInt(String name, int defaultValue) {
        try {
            return Integer.parseInt(uri.getQueryParameter(name));
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    protected long getLong(String name, long defaultValue) {
        try {
            return Long.parseLong(uri.getQueryParameter(name));
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MediaId)
            return uri.compareTo(((MediaId) o).uri) == 0;
        return false;
    }

    @Override
    public String toString() {
        return uri.toString();
    }

}
