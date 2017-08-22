/**
 * Copyright 2016 YouVersion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei3.media;

import android.graphics.Bitmap;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.lang.ref.WeakReference;

import nuclei3.media.playback.Playback;
import nuclei3.media.playback.Timing;

public final class MediaMetadata {

    private MediaMetadataCompat mMetadata;
    private WeakReference<Playback.Callback> mCallback;
    private Timing mTiming;
    private boolean mTimingSeeked;

    public MediaMetadata(MediaMetadataCompat metadata) {
        mMetadata = metadata;
    }

    public boolean isEqual(MediaId id) {
        return mMetadata.getDescription().getMediaId().equals(id.toString());
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

    public boolean isTimingSeeked() {
        return mTimingSeeked;
    }

    public void setTimingSeeked(boolean timingSeeked) {
        mTimingSeeked = timingSeeked;
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
            mTimingSeeked = false;
        }
        return mTiming;
    }

    public void setTiming(Timing timing) {
        mTiming = timing;
        mTimingSeeked = false;
        //noinspection ResourceType
        mMetadata = new MediaMetadataCompat.Builder(mMetadata)
                .putLong(MediaProvider.CUSTOM_METADATA_TIMING_START, timing.start)
                .putLong(MediaProvider.CUSTOM_METADATA_TIMING_END, timing.end)
                .build();
        onMetadataChanged();
    }

    public long getDuration() {
        return mMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
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
