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
package nuclei3.media.playback;

import android.net.Uri;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.Surface;

import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;

import org.json.JSONException;
import org.json.JSONObject;

import nuclei3.logs.Log;
import nuclei3.logs.Logs;
import nuclei3.media.MediaId;
import nuclei3.media.MediaMetadata;
import nuclei3.media.MediaProvider;

/**
 * An implementation of Playback that talks to Cast.
 */
public class CastPlayback extends BasePlayback implements Playback {

    private static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";
    private static final String MIME_TYPE_VIDEO_MPEG = "video/mpeg";
    private static final String ITEM_ID = "itemId";

    private static final Log LOG = Logs.newLog(CastPlayback.class);

    private final VideoCastConsumerImpl mCastConsumer = new VideoCastConsumerImpl() {

        @Override
        public void onApplicationStatusChanged(String appStatus) {
            updatePlaybackState();
        }

        @Override
        public void onRemoteMediaPlayerMetadataUpdated() {
            setMetadataFromRemote();
        }

        @Override
        public void onRemoteMediaPlayerStatusUpdated() {
            updatePlaybackState();
        }

        @Override
        public void onApplicationConnectionFailed(int errorCode) {
            updatePlaybackState();
        }

        @Override
        public void onFailed(int resourceId, int statusCode) {
            updatePlaybackState();
        }

        @Override
        public void onDataMessageSendFailed(int errorCode) {
            updatePlaybackState();
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            updatePlaybackState();
        }

        @Override
        public void onApplicationStopFailed(int errorCode) {
            updatePlaybackState();
        }

    };

    private MediaMetadata mMediaMetadata;
    private int mState;
    private Callback mCallback;
    private volatile long mCurrentPosition;
    private volatile MediaId mCurrentMediaId;
    private Surface mSurface;
    private long mSurfaceId;

    public CastPlayback() {
    }

    @Override
    public long getSurfaceId() {
        return mSurfaceId;
    }

    @Override
    public Surface getSurface() {
        return mSurface;
    }

    @Override
    public void setSurface(long surfaceId, Surface surface) {
        mSurfaceId = surfaceId;
        mSurface = surface;
    }

    @Override
    public void start() {
        VideoCastManager.getInstance().removeVideoCastConsumer(mCastConsumer);
        VideoCastManager.getInstance().addVideoCastConsumer(mCastConsumer);
    }

    @Override
    public void stop(boolean notifyListeners) {
        if (mMediaMetadata != null)
            mMediaMetadata.setTimingSeeked(false);
        try {
            VideoCastManager.getInstance().stop();
        } catch (IllegalStateException | CastException | TransientNetworkDisconnectionException | NoConnectionException e) {
            LOG.e("Error stopping", e);
        }
        if (notifyListeners)
            VideoCastManager.getInstance().removeVideoCastConsumer(mCastConsumer);
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    @Override
    public void temporaryStop() {
        if (mMediaMetadata != null)
            mMediaMetadata.setTimingSeeked(false);
        try {
            VideoCastManager.getInstance().pause();
        } catch (IllegalStateException | CastException | TransientNetworkDisconnectionException | NoConnectionException e) {
            LOG.e("Error pausing", e);
        }
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    @Override
    public void setState(int state) {
        mState = state;
    }

    @Override
    protected long internalGetCurrentStreamPosition() {
        if (!VideoCastManager.getInstance().isConnected()) {
            return mCurrentPosition;
        }
        try {
            return VideoCastManager.getInstance().getCurrentMediaPosition();
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {

        }
        return -1;
    }

    @Override
    protected long internalGetDuration() {
        if (!VideoCastManager.getInstance().isConnected()) {
            return -1;
        }
        try {
            return VideoCastManager.getInstance().getMediaDuration();
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {

        }
        return -1;
    }

    @Override
    public void setCurrentStreamPosition(long pos) {
        this.mCurrentPosition = pos;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        mCurrentPosition = getCurrentStreamPosition();
    }

    @Override
    protected void internalPlay(MediaMetadata metadataCompat, Timing timing, boolean seek) {
        try {
            mCurrentMediaId = MediaProvider.getInstance().getMediaId(metadataCompat.getDescription().getMediaId());
            mMediaMetadata = metadataCompat;
            mMediaMetadata.setCallback(mCallback);
            if (timing != null && seek)
                mCurrentPosition = timing.start;
            loadMedia(metadataCompat, true);
            mState = PlaybackStateCompat.STATE_BUFFERING;
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException
                | JSONException | IllegalArgumentException e) {
            if (mCallback != null) {
                mCallback.onError(e, true);
            }
        }
    }

    @Override
    protected void internalPrepare(MediaMetadata metadataCompat, Timing timing) {
        boolean mediaHasChanged = mCurrentMediaId == null
                || !TextUtils.equals(metadataCompat.getDescription().getMediaId(), mCurrentMediaId.toString());
        if (mediaHasChanged) {
            mCurrentPosition = getStartStreamPosition();
            mMediaMetadata = metadataCompat;
            mMediaMetadata.setCallback(mCallback);
            mCurrentMediaId = MediaProvider.getInstance().getMediaId(metadataCompat.getDescription().getMediaId());
            if (mCallback != null) {
                mCallback.onMetadataChanged(mMediaMetadata);
                mCallback.onPlaybackStatusChanged(mState);
            }
            if (timing != null)
                internalSeekTo(timing.start);
        }
    }

    @Override
    public void pause() {
        try {
            VideoCastManager manager = VideoCastManager.getInstance();
            if (manager.isRemoteMediaLoaded()) {
                manager.pause();
                mCurrentPosition = (int) manager.getCurrentMediaPosition();
            } else {
                loadMedia(mMediaMetadata, false);
            }
        } catch (JSONException | CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalArgumentException e) {
            if (mCallback != null) {
                mCallback.onError(e, false);
            }
        }
    }

    @Override
    protected void internalSeekTo(long position) {
        if (mCurrentMediaId == null) {
            if (mCallback != null) {
                mCallback.onError(new Exception("seekTo cannot be calling in the absence of mediaId."), true);
            }
            return;
        }
        try {
            if (VideoCastManager.getInstance().isRemoteMediaLoaded()) {
                VideoCastManager.getInstance().seek((int) position);
                mCurrentPosition = position;
            } else {
                mCurrentPosition = position;
                loadMedia(mMediaMetadata, false);
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException | JSONException | IllegalArgumentException e) {
            if (mCallback != null) {
                mCallback.onError(e, true);
            }
        }
    }

    @Override
    protected void internalSetCurrentMediaMetadata(MediaId mediaId, MediaMetadata metadata) {
        mCurrentMediaId = mediaId;
        mMediaMetadata = metadata;
    }

    @Override
    public MediaId getCurrentMediaId() {
        return mCurrentMediaId;
    }

    @Override
    public MediaMetadata getCurrentMetadata() {
        return mMediaMetadata;
    }

    @Override
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public boolean isConnected() {
        return VideoCastManager.getInstance().isConnected();
    }

    @Override
    public boolean isPlaying() {
        try {
            return VideoCastManager.getInstance().isConnected()
                    && VideoCastManager.getInstance().isRemoteMediaPlaying();
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {

        }
        return false;
    }

    @Override
    public int getState() {
        return mState;
    }

    private void loadMedia(MediaMetadata metadataCompat, boolean autoPlay) throws
            TransientNetworkDisconnectionException, NoConnectionException, JSONException {
        if (metadataCompat == null || metadataCompat.getDescription() == null)
            return;
        updatePlaybackState();
        if (mCurrentMediaId == null || !TextUtils.equals(metadataCompat.getDescription().getMediaId(), mCurrentMediaId.toString())) {
            mCurrentMediaId = MediaProvider.getInstance().getMediaId(metadataCompat.getDescription().getMediaId());
            mCurrentPosition = getStartStreamPosition();
        }
        JSONObject customData = new JSONObject();
        customData.put(ITEM_ID, metadataCompat.getDescription().getMediaId());
        MediaInfo media = toCastMediaMetadata(metadataCompat, customData);
        VideoCastManager.getInstance().loadMedia(media, autoPlay, (int) mCurrentPosition, customData);
    }

    /**
     * Helper method to convert a {@link android.media.MediaMetadata} to a
     * {@link MediaInfo} used for sending media to the receiver app.
     *
     * @param track {@link MediaMetadata}
     * @param customData custom data specifies the local mediaId used by the player.
     * @return mediaInfo {@link MediaInfo}
     */
    private static MediaInfo toCastMediaMetadata(MediaMetadata track,
                                                 JSONObject customData) {
        com.google.android.gms.cast.MediaMetadata mediaMetadata
                = new com.google.android.gms.cast.MediaMetadata(com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE,
                track.getDescription().getTitle() == null ? "" : track.getDescription().getTitle().toString());
        mediaMetadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_SUBTITLE,
                track.getDescription().getSubtitle() == null ? "" : track.getDescription().getSubtitle().toString());
        mediaMetadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_ARTIST, track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST));
        mediaMetadata.putString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_TITLE, track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
        WebImage image = new WebImage(
                new Uri.Builder().encodedPath(
                        track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                        .build());
        // First image is used by the receiver for showing the audio album art.
        mediaMetadata.addImage(image);
        // Second image is used by Cast Companion Library on the full screen activity that is shown
        // when the cast dialog is clicked.
        mediaMetadata.addImage(image);

        MediaId id = MediaProvider.getInstance().getMediaId(track.getDescription().getMediaId());

        //noinspection ResourceType
        return new MediaInfo.Builder(track.getString(MediaProvider.CUSTOM_METADATA_TRACK_SOURCE))
                .setContentType(id.type == MediaId.TYPE_AUDIO ? MIME_TYPE_AUDIO_MPEG : MIME_TYPE_VIDEO_MPEG)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .setCustomData(customData)
                .build();
    }

    void setMetadataFromRemote() {
        // Sync: We get the customData from the remote media information and update the local
        // metadata if it happens to be different from the one we are currently using.
        // This can happen when the app was either restarted/disconnected + connected, or if the
        // app joins an existing session while the Chromecast was playing a queue.
        try {
            MediaInfo mediaInfo = VideoCastManager.getInstance().getRemoteMediaInformation();
            if (mediaInfo == null) {
                return;
            }
            JSONObject customData = mediaInfo.getCustomData();
            if (customData != null && customData.has(ITEM_ID)) {
                String remoteMediaId = customData.getString(ITEM_ID);
                if (remoteMediaId != null) {
                    if (mCurrentMediaId == null
                            || !TextUtils.equals(mCurrentMediaId.toString(), remoteMediaId)
                            || (mMediaMetadata != null && mMediaMetadata.getDuration() != getDuration())) {
                        mCurrentMediaId = MediaProvider.getInstance().getMediaId(remoteMediaId);
                        if (mCallback != null && mMediaMetadata != null) {
                            mMediaMetadata.setDuration(getDuration());
                        }
                        updateLastKnownStreamPosition();
                    }
                }
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException | JSONException e) {
            if (mCallback != null) {
                mCallback.onError(e, true);
            }
        }
    }

    void updatePlaybackState() {
        final int status = VideoCastManager.getInstance().getPlaybackStatus();

        // Convert the remote playback states to media playback states.
        switch (status) {
            case MediaStatus.PLAYER_STATE_IDLE:
                final int idleReason = VideoCastManager.getInstance().getIdleReason();
                switch (idleReason) {
                    case MediaStatus.IDLE_REASON_ERROR:
                        if (mCallback != null)
                            mCallback.onError(new Exception("Error: " + idleReason), true);
                        break;
                    case MediaStatus.IDLE_REASON_INTERRUPTED:
                    case MediaStatus.IDLE_REASON_CANCELED:
                        // TODO: What should happen here?
                        mState = PlaybackStateCompat.STATE_NONE;
                        if (mCallback != null)
                            mCallback.onPlaybackStatusChanged(mState);
                        break;
                    case MediaStatus.IDLE_REASON_FINISHED:
                        if (mCallback != null)
                            mCallback.onCompletion();
                        break;
                    default:
                        setMetadataFromRemote();
                        if (mCallback != null)
                            mCallback.onPlaybackStatusChanged(mState);
                        break;
                }
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                mState = PlaybackStateCompat.STATE_BUFFERING;
                setMetadataFromRemote();
                if (mCallback != null)
                    mCallback.onPlaybackStatusChanged(mState);
                break;
            case MediaStatus.PLAYER_STATE_PLAYING:
                mState = PlaybackStateCompat.STATE_PLAYING;
                setMetadataFromRemote();
                if (mCallback != null)
                    mCallback.onPlaybackStatusChanged(mState);
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                mState = PlaybackStateCompat.STATE_PAUSED;
                setMetadataFromRemote();
                if (mCallback != null)
                    mCallback.onPlaybackStatusChanged(mState);
                break;
            default: // case unknown
                setMetadataFromRemote();
                if (mCallback != null)
                    mCallback.onPlaybackStatusChanged(mState);
                break;
        }
    }

    @Override
    public void setPlaybackParams(PlaybackParameters playbackParams) {
        if (mCallback != null)
            mCallback.onPlaybackStatusChanged(mState);
    }

}
