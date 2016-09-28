package nuclei.media;

import android.graphics.Bitmap;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.lang.ref.WeakReference;

import nuclei.media.playback.Playback;
import nuclei.media.playback.Timing;

public final class MediaMetadata {

    private MediaMetadataCompat mMetadata;
    private WeakReference<Playback.Callback> mCallback;
    private Timing mTiming;

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

    public Timing getTiming() {
        if (mTiming == null) {
            //noinspection ResourceType
            long end = mMetadata.getLong(MediaProvider.CUSTOM_METADATA_TIMING_END);
            if (end == 0)
                return null;
            //noinspection ResourceType
            long start = mMetadata.getLong(MediaProvider.CUSTOM_METADATA_TIMING_START);
            mTiming = new Timing(start, end);
        }
        return mTiming;
    }

    public void setTiming(Timing timing) {
        mTiming = timing;
        //noinspection ResourceType
        mMetadata = new MediaMetadataCompat.Builder(mMetadata)
                .putLong(MediaProvider.CUSTOM_METADATA_TIMING_START, timing.start)
                .putLong(MediaProvider.CUSTOM_METADATA_TIMING_END, timing.end)
                .build();
        onMetadataChanged();
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
