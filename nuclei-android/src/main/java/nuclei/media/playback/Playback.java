/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.media.playback;

import android.media.PlaybackParams;
import android.view.Surface;

import nuclei.media.MediaId;
import nuclei.media.MediaMetadata;

public interface Playback {

    void start();

    void stop(boolean notifyListeners);

    void temporaryStop();

    void setState(int state);

    int getState();

    boolean isConnected();

    boolean isPlaying();

    Timing getTiming();

    long getStartStreamPosition();

    long getCurrentStreamPosition();

    void setCurrentStreamPosition(long pos);

    long getDuration();

    void updateLastKnownStreamPosition();

    void play(MediaMetadata metadata);

    void prepare(MediaMetadata metadata);

    void pause();

    void seekTo(long position);

    void setCurrentMediaMetadata(MediaId mediaId, MediaMetadata metadata);

    MediaId getCurrentMediaId();

    MediaMetadata getCurrentMetadata();

    long getSurfaceId();

    Surface getSurface();

    void setSurface(long surfaceId, Surface surface);

    void setPlaybackParams(PlaybackParams playbackParams);

    interface Callback {
        /**
         * On current music completed.
         */
        void onCompletion();

        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * @param error to be added to the PlaybackState
         */
        void onError(String error);

        void onMetadataChanged(MediaMetadata metadata);
    }

    /**
     * @param callback to be called
     */
    void setCallback(Callback callback);
}
