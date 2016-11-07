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
package nuclei.media;

import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.MediaController;

import nuclei.logs.Log;
import nuclei.logs.Logs;

public class MediaPlayerController implements MediaController.MediaPlayerControl {

    private static final Log LOG = Logs.newLog(MediaPlayerController.class);

    MediaId mMediaId;
    private MediaInterface.MediaInterfaceCallback mCallbacks;
    private MediaControllerCompat mMediaControls;
    private boolean mStartWhenReady;


    public MediaPlayerController(MediaId mediaId) {
        mMediaId = mediaId;
    }

    public void setMediaId(MediaId mediaId, boolean play) {
        mMediaId = mediaId;
        if (play)
            start();
        else
            mMediaControls.getTransportControls().prepareFromMediaId(mediaId.toString(), null);
    }

    protected boolean isMediaControlsSet() {
        return mCallbacks != null && mMediaControls != null;
    }

    public void setMediaControls(MediaInterface.MediaInterfaceCallback callback, MediaControllerCompat mediaControls) {
        mCallbacks = callback;
        mMediaControls = mediaControls;
        String mediaId = MediaPlayerController.getMediaId(mMediaControls);
        if (mediaId != null)
            mMediaId = MediaProvider.getInstance().getMediaId(mediaId);
        if (mStartWhenReady) {
            mStartWhenReady = false;
            start();
        }
    }

    private boolean isEquals() {
        return isEquals(mMediaControls, mMediaId);
    }

    @Override
    public void start() {
        if (mMediaId == null)
            return;
        if (mMediaControls != null) {
            if (mCallbacks != null) {
                mCallbacks.onLoading(this);
            }
            String currentMediaId = null;
            MediaMetadataCompat metadataCompat = mMediaControls.getMetadata();
            if (metadataCompat != null) {
                MediaDescriptionCompat descriptionCompat = metadataCompat.getDescription();
                if (descriptionCompat != null) {
                    currentMediaId = descriptionCompat.getMediaId();
                }
            }
            final String id = mMediaId.toString();
            if (currentMediaId != null && currentMediaId.equals(id)
                    && mMediaControls.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED) {
                mMediaControls.getTransportControls().play();
            } else {
                mMediaControls.getTransportControls().playFromMediaId(id, null);
            }
        } else {
            mStartWhenReady = true;
        }
    }

    @Override
    public void pause() {
        if (mCallbacks != null)
            mCallbacks.onPaused(this);
        if (mMediaControls != null)
            mMediaControls.getTransportControls().pause();
    }

    @Override
    public int getDuration() {
        if (isEquals()) {
            MediaMetadataCompat mediaMetadataCompat = mMediaControls.getMetadata();
            if (mediaMetadataCompat != null) {
                long duration = mediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                if (duration > 0)
                    return (int) duration;
            }
        }
        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isEquals()) {
            return (int) mMediaControls.getPlaybackState().getPosition();
        }
        return -1;
    }

    @Override
    public void seekTo(int pos) {
        if (isEquals())
            mMediaControls.getTransportControls().seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return isPlaying(mMediaControls, mMediaId);
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaControls != null) {
            int dur = getDuration();
            if (dur < 1)
                return 0;
            return (int) mMediaControls.getPlaybackState().getBufferedPosition() / getDuration();
        }
        return -1;
    }

    @Override
    public boolean canPause() {
        return isEquals();
    }

    @Override
    public boolean canSeekBackward() {
        return isEquals();
    }

    @Override
    public boolean canSeekForward() {
        return isEquals();
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Nullable
    public MediaId getMediaId() {
        return mMediaId;
    }

    public static boolean isEquals(MediaControllerCompat mediaControllerCompat, MediaId mediaId) {
        String currentId = getMediaId(mediaControllerCompat);
        if (currentId != null) {
            MediaId id = MediaProvider.getInstance().getMediaId(currentId);
            if (mediaId != null && mediaId.equals(id))
                return true;
            LOG.i("ID (" + id + ") != (" + mediaId + ")");
        } else {
            LOG.i("Media ID not set on controller");
        }
        return false;
    }

    @Nullable
    public static String getMediaId(MediaControllerCompat mediaControllerCompat) {
        if (mediaControllerCompat != null) {
            MediaMetadataCompat metadataCompat = mediaControllerCompat.getMetadata();
            if (metadataCompat != null) {
                return getMediaId(metadataCompat);
            }
        }
        return null;
    }

    @Nullable
    public static String getMediaId(MediaMetadataCompat metadataCompat) {
        MediaDescriptionCompat descriptionCompat = metadataCompat == null ? null : metadataCompat.getDescription();
        if (descriptionCompat != null)
            return descriptionCompat.getMediaId();
        return null;
    }

    public static boolean isPlaying(MediaControllerCompat mediaControllerCompat, PlaybackStateCompat playbackStateCompat, MediaId mediaId) {
        if (isEquals(mediaControllerCompat, mediaId)) {
            if (playbackStateCompat == null)
                playbackStateCompat = mediaControllerCompat.getPlaybackState();
            int state = -1;
            if (playbackStateCompat != null)
                state = playbackStateCompat.getState();
            return state == PlaybackStateCompat.STATE_BUFFERING || state == PlaybackStateCompat.STATE_PLAYING;
        }
        return false;
    }

    public static boolean isPlaying(MediaControllerCompat mediaControllerCompat, MediaId mediaId) {
        return isPlaying(mediaControllerCompat, null, mediaId);
    }

}