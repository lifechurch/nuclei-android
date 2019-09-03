/**
 * Copyright 2016 YouVersion
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei3.media;

import android.os.SystemClock;
import androidx.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.MediaController;

import nuclei3.logs.Log;
import nuclei3.logs.Logs;

public class MediaPlayerController implements MediaController.MediaPlayerControl {

    private static final Log LOG = Logs.newLog(MediaPlayerController.class);

    MediaId mMediaId;
    String mMediaIdStr;
    PlaybackStateCompat mPlaybackState;
    MediaMetadataCompat mMetaData;

    private MediaInterface.MediaInterfaceCallback mCallbacks;
    private MediaControllerCompat mMediaControls;
    private boolean mStartWhenReady;

    public MediaPlayerController(MediaId mediaId) {
        mMediaId = mediaId;
        mMediaIdStr = mMediaId == null ? null : mMediaId.toString();
    }

    public void setMediaId(MediaId mediaId, boolean play) {
        mMediaId = mediaId;
        mMediaIdStr = mMediaId == null ? null : mMediaId.toString();
        mPlaybackState = null;
        mMetaData = null;
        if (play)
            start();
        else if (mMediaControls != null)
            mMediaControls.getTransportControls().prepareFromMediaId(mediaId.toString(), null);
    }

    protected boolean isMediaControlsSet() {
        return mCallbacks != null && mMediaControls != null;
    }

    public void setCallbacks(MediaInterface.MediaInterfaceCallback callback) {
        mCallbacks = callback;
    }

    public void setMediaControls(MediaInterface.MediaInterfaceCallback callback, MediaControllerCompat mediaControls) {
        mCallbacks = callback;
        mMediaControls = mediaControls;
        String mediaId = MediaInterface.getMediaId(mMediaControls);
        if (mediaId != null) {
            mMediaId = MediaProvider.getInstance().getMediaId(mediaId);
            mMediaIdStr = mMediaId == null ? null : mMediaId.toString();
        }
        if (mStartWhenReady) {
            mStartWhenReady = false;
            start();
        }
    }

    public void skipToNext() {
        if (mCallbacks != null && mMediaControls != null
                && mCallbacks.onSkipNext(this, mMediaControls.getTransportControls()))
            return;
        if (mMediaControls != null)
            mMediaControls.getTransportControls().skipToNext();
    }

    public void skipToPrevious() {
        if (mCallbacks != null && mMediaControls != null
                && mCallbacks.onSkipPrevious(this, mMediaControls.getTransportControls()))
            return;
        if (mMediaControls != null)
            mMediaControls.getTransportControls().skipToPrevious();
    }

    public void fastForward() {
        if (mCallbacks != null && mMediaControls != null
                && mCallbacks.onFastForward(this, mMediaControls.getTransportControls()))
            return;
        if (mMediaControls != null)
            mMediaControls.getTransportControls().fastForward();
    }

    public void rewind() {
        if (mCallbacks != null && mMediaControls != null
                && mCallbacks.onRewind(this, mMediaControls.getTransportControls()))
            return;
        if (mMediaControls != null)
            mMediaControls.getTransportControls().rewind();
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
            final String id = mMediaIdStr;
            if (mCallbacks != null && mCallbacks.onPlay(currentMediaId, id, this, mMediaControls.getTransportControls()))
                return;
            if (mCallbacks != null)
                mCallbacks.onPlaying(this);
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
        if (mCallbacks != null && mMediaControls != null
                && mCallbacks.onPause(this, mMediaControls.getTransportControls()))
            return;
        if (mMediaControls != null) {
            if (mCallbacks != null)
                mCallbacks.onPaused(this);
            mMediaControls.getTransportControls().pause();
        }
    }

    private MediaMetadataCompat getMetaData() {
        if (mMetaData == null && mMediaControls != null) {
            mMetaData = mMediaControls.getMetadata();
            String id = MediaInterface.getMediaId(mMetaData);
            if (id == null || !id.equals(mMediaIdStr))
                mMetaData = null;
        }
        return mMetaData;
    }

    @Override
    public int getDuration() {
        MediaMetadataCompat metadataCompat = getMetaData();
        if (metadataCompat != null) {
            long duration = metadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            if (duration > 0)
                return (int) duration;
        }
        return -1;
    }

    private PlaybackStateCompat getPlaybackState() {
        if (mPlaybackState == null && mMediaControls != null && getMetaData() != null) {
            mPlaybackState = mMediaControls.getPlaybackState();
        }
        return mPlaybackState;
    }

    @Override
    public int getCurrentPosition() {
        PlaybackStateCompat playbackStateCompat = getPlaybackState();
        if (playbackStateCompat != null) {
            // KJB: NOTE: Pulled from media compat library
            //            For some reason, it seems, that only API 21 doesn't do something equivalent
            //            of this.  weird.
            //            Just to be safe, since this is important, doing it here.
            //            It's a little redundant... but, I don't see how this would hurt anything
            //            if it were executed twice (so long as getLastPositionUpdateTime is correct)
            //      TODO: revisit this after support library 25.1.1
            try {
                if ((playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING
                        || playbackStateCompat.getState() == PlaybackStateCompat.STATE_FAST_FORWARDING
                        || playbackStateCompat.getState() == PlaybackStateCompat.STATE_REWINDING)) {
                    final long updateTime = playbackStateCompat.getLastPositionUpdateTime();
                    final long currentTime = SystemClock.elapsedRealtime();
                    if (updateTime > 0) {
                        final float speed = playbackStateCompat.getPlaybackSpeed();
                        final long duration = getDuration();
                        long position = (long) (speed * (currentTime - updateTime)) + playbackStateCompat.getPosition();
                        if (duration >= 0 && position > duration) {
                            position = duration;
                        } else if (position < 0) {
                            position = 0;
                        }
                        return (int) position;
                    }
                }
            } catch (Exception err) { // because weird things happen sometimes :(
                LOG.e("Error calculating latest position", err);
            }
            return (int) playbackStateCompat.getPosition();
        }
        return -1;
    }

    @Override
    public void seekTo(int pos) {
        if (mMediaControls != null)
            mMediaControls.getTransportControls().seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return MediaInterface.isPlaying(getPlaybackState());
    }

    @Override
    public int getBufferPercentage() {
        PlaybackStateCompat playbackStateCompat = getPlaybackState();
        if (playbackStateCompat == null)
            return -1;
        int dur = getDuration();
        if (dur < 1)
            return 0;
        return (int) playbackStateCompat.getBufferedPosition() / getDuration();
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Nullable
    public MediaId getMediaId() {
        return mMediaId;
    }

}