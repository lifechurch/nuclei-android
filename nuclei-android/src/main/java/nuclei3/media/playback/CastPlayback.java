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
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

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

    private MediaMetadata mMediaMetadata;
    private int mState;
    private Callback mCallback;
    private volatile long mCurrentPosition;
    private volatile MediaId mCurrentMediaId;
    private Surface mSurface;
    private long mSurfaceId;

    CastSession mCastSession;
    CastContext mCastContext;

    final private SessionManagerListener<CastSession> mSessionManagerListener = new SessionManagerListener<CastSession>() {

        @Override
        public void onSessionEnded(CastSession session, int error) {
            updatePlaybackState();
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
            mCastSession = session;
            updatePlaybackState();
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
            updatePlaybackState();
        }

        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            mCastSession = session;
            updatePlaybackState();
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
            updatePlaybackState();
        }

        @Override
        public void onSessionStarting(CastSession session) {
        }

        @Override
        public void onSessionEnding(CastSession session) {
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
        }

    };


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
        mCastContext = CastContext.getSharedInstance();
        mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
//        mCastContext.getSessionManager().removeSessionManagerListener(mSessionManagerListener, CastSession.class);
        mCastContext.getSessionManager().addSessionManagerListener(mSessionManagerListener, CastSession.class);
    }

    @Override
    public void stop(boolean notifyListeners) {
        if (mMediaMetadata != null)
            mMediaMetadata.setTimingSeeked(false);
        try {
            mCastSession.getRemoteMediaClient().stop();
        } catch (IllegalStateException e) {
            LOG.e("Error stopping", e);
        }
        if (notifyListeners)
            mCastContext.getSessionManager().removeSessionManagerListener(mSessionManagerListener, CastSession.class);
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
            mCastSession.getRemoteMediaClient().pause();
        } catch (IllegalStateException e) {
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
        if (!isConnected()) {
            return mCurrentPosition;
        }
        try {
            return mCastSession.getRemoteMediaClient().getApproximateStreamPosition();
        } catch (Exception e) {

        }
        return -1;
    }

    @Override
    protected long internalGetDuration() {
        if (!isConnected()) {
            return -1;
        }
        try {
            return mCastSession.getRemoteMediaClient().getStreamDuration();
        } catch (Exception e) {

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
        } catch (JSONException | IllegalArgumentException e) {
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
            if (isConnected()) {
                mCastSession.getRemoteMediaClient().pause();
                mCurrentPosition = (int) mCastSession.getRemoteMediaClient().getApproximateStreamPosition();
            } else {
                loadMedia(mMediaMetadata, false);
            }
        } catch (JSONException | IllegalArgumentException e) {
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
            if (isConnected()) {
                mCastSession.getRemoteMediaClient().seek((int) position);
                mCurrentPosition = position;
            } else {
                mCurrentPosition = position;
                loadMedia(mMediaMetadata, false);
            }
        } catch (JSONException | IllegalArgumentException e) {
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
        return mCastSession.isConnected();
    }

    @Override
    public boolean isPlaying() {
        try {
            return isConnected() && mCastSession.getRemoteMediaClient().isPlaying();
        } catch (Exception e) {

        }
        return false;
    }

    @Override
    public int getState() {
        return mState;
    }

    private void loadMedia(MediaMetadata metadataCompat, boolean autoPlay) throws JSONException {
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

        mCastSession.getRemoteMediaClient().load(media, new MediaLoadOptions.Builder()
                .setAutoplay(autoPlay)
                .setPlayPosition(mCurrentPosition).build());
    }

    /**
     * Helper method to convert a {@link android.media.MediaMetadata} to a
     * {@link MediaInfo} used for sending media to the receiver app.
     *
     * @param track      {@link MediaMetadata}
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
            RemoteMediaClient client = mCastSession.getRemoteMediaClient();
            if (client == null)
                return;
            MediaInfo mediaInfo = client.getMediaInfo();
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
        } catch (JSONException e) {
            if (mCallback != null) {
                mCallback.onError(e, true);
            }
        }
    }

    void updatePlaybackState() {
        int status;
        try {
            status = mCastSession.getRemoteMediaClient().getMediaStatus().getPlayerState();
        } catch (Exception e) {
            status = -1;
        }

        // Convert the remote playback states to media playback states.
        switch (status) {
            case MediaStatus.PLAYER_STATE_IDLE:
                final int idleReason = mCastSession.getRemoteMediaClient().getIdleReason();
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
