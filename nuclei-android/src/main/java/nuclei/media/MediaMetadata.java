package nuclei.media;

import android.graphics.Bitmap;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.lang.ref.WeakReference;

import nuclei.media.playback.Playback;

public final class MediaMetadata {

    private MediaMetadataCompat mMetadata;
    private WeakReference<Playback.Callback> mCallback;

    public MediaMetadata(MediaMetadataCompat metadata) {
        mMetadata = metadata;
    }

    public String getMediaId() {
        return mMetadata.getDescription().getMediaId();
    }

    public void setCallback(Playback.Callback callback) {
        mCallback = new WeakReference<>(callback);
    }

    public void setSession(MediaSessionCompat session) {
        if (session != null)
            session.setMetadata(mMetadata);
    }

    public MediaDescriptionCompat getDescription() {
        return mMetadata.getDescription();
    }

    public long getLong(String key) {
        return mMetadata.getLong(key);
    }

    public String getString(String key) {
        return mMetadata.getString(key);
    }

    public void setDuration(long duration) {
        mMetadata = new MediaMetadataCompat.Builder(mMetadata)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .build();
        onMetadataChanged();
    }

    public void setAlbumArt(Bitmap albumArt) {
        mMetadata = new MediaMetadataCompat.Builder(mMetadata)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .build();
        onMetadataChanged();
    }

    public void setDisplayIcon(Bitmap icon) {
        mMetadata = new MediaMetadataCompat.Builder(mMetadata)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)
                .build();
        onMetadataChanged();
    }

    private void onMetadataChanged() {
        if (mCallback == null)
            return;
        Playback.Callback callback = mCallback.get();
        if (callback != null)
            callback.onMetadataChanged(this);
    }
}
