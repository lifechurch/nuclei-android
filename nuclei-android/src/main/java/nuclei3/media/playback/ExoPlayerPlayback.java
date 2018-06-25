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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;

import nuclei3.media.MediaId;
import nuclei3.media.MediaMetadata;
import nuclei3.media.MediaProvider;
import nuclei3.media.MediaService;
import nuclei3.logs.Log;
import nuclei3.logs.Logs;

public class ExoPlayerPlayback extends BasePlayback
        implements
        Playback,
        AudioManager.OnAudioFocusChangeListener,
        Player.EventListener,
        MediaSourceEventListener {

    static final Log LOG = Logs.newLog(ExoPlayerPlayback.class);

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    private static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    private static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    protected static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private final Handler mHandler;
    final MediaService mService;
    private final WifiManager.WifiLock mWifiLock;
    private int mState;
    private boolean mPlayOnFocusGain;
    private Callback mCallback;
    private volatile boolean mAudioNoisyReceiverRegistered;
    private volatile long mCurrentPosition;
    private volatile MediaId mCurrentMediaId;
    private volatile MediaMetadata mMediaMetadata;
    private boolean mPrepared;
    private boolean mRestart;
    private boolean mPlayWhenReady = true;

    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private final AudioManager mAudioManager;
    private SimpleExoPlayer mMediaPlayer;

    private long mSurfaceId;
    private Surface mSurface;
    private PlaybackParameters mPlaybackParams;

    private final PowerManager.WakeLock mWakeLock;

    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private int mIllegalStateRetries;

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                LOG.d("Headphones disconnected.");
                if (isPlaying()) {
                    Intent i = new Intent(context, MediaService.class);
                    i.setAction(MediaService.ACTION_CMD);
                    i.putExtra(MediaService.CMD_NAME, MediaService.CMD_PAUSE);
                    mService.startService(i);
                }
            }
        }
    };

    public ExoPlayerPlayback(MediaService service) {
        mService = service;
        mHandler = new Handler();
        final Context ctx = service.getApplicationContext();
        mAudioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        PowerManager powerManager = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "nuclei_media_wifi_lock");
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nuclei_media_cpu_lock");
    }

    @Override
    public void start() {
    }

    @Override
    public void stop(boolean notifyListeners) {
        LOG.d("stop");
        if (mMediaMetadata != null)
            mMediaMetadata.setTimingSeeked(false);
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        //mCurrentPosition = getCurrentStreamPosition();
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        relaxResources(true);
    }

    @Override
    public void temporaryStop() {
        LOG.d("stop");
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        if (mMediaPlayer != null)
            mMediaPlayer.stop();
        relaxResources(false);
    }

    @Override
    public void updateLastKnownStreamPosition() {
        mCurrentPosition = getCurrentStreamPosition();
    }

    public void stopFully() {
        stop(true);
    }

    @Override
    public void setState(int state) {
        mState = state;
        if (state == PlaybackStateCompat.STATE_ERROR) {
            try {
                mCurrentPosition = getCurrentStreamPosition();
            } catch (Exception err) {
                LOG.e("Error capturing current pos", err);
            }
        }
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return mPlayOnFocusGain || isStatePlaying();
    }

    private boolean isStatePlaying() {
        return isMediaPlayerPlaying()
                || mState == PlaybackStateCompat.STATE_PLAYING
                || mState == PlaybackStateCompat.STATE_BUFFERING
                || mState == PlaybackStateCompat.STATE_CONNECTING;
    }

    @Override
    protected long internalGetCurrentStreamPosition() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getCurrentPosition();
        }
        return mCurrentPosition;
    }

    @Override
    protected void internalPlay(MediaMetadata metadataCompat, Timing timing, boolean seek) {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        boolean mediaHasChanged = mCurrentMediaId == null
                || !TextUtils.equals(metadataCompat.getDescription().getMediaId(), mCurrentMediaId.toString());
        if (mediaHasChanged || mRestart) {
            mRestart = false;
            mCurrentPosition = getStartStreamPosition();
            mMediaMetadata = metadataCompat;
            mMediaMetadata.setCallback(mCallback);
            mCurrentMediaId = MediaProvider.getInstance().getMediaId(metadataCompat.getDescription().getMediaId());
        }

        mPlayWhenReady = true;
        if (mState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mMediaPlayer != null) {
            if (!mWakeLock.isHeld())
                mWakeLock.acquire();
            if (!mWifiLock.isHeld())
                mWifiLock.acquire();
            configMediaPlayerState(false, true);
        } else {
            mState = mMediaPlayer != null
                    ? mState == PlaybackStateCompat.STATE_STOPPED
                    ? PlaybackStateCompat.STATE_STOPPED
                    : PlaybackStateCompat.STATE_PAUSED
                    : PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false); // release everything except MediaPlayer
            setTrack(metadataCompat);
        }

        if (timing != null && seek)
            internalSeekTo(timing.start);
    }

    @Override
    protected void internalPrepare(MediaMetadata metadataCompat, Timing timing) {
        boolean mediaHasChanged = mCurrentMediaId == null
                || !TextUtils.equals(metadataCompat.getDescription().getMediaId(), mCurrentMediaId.toString());
        if (mediaHasChanged) {
            stop(true);

            mCurrentPosition = getStartStreamPosition();
            mMediaMetadata = metadataCompat;
            mMediaMetadata.setCallback(mCallback);
            mCurrentMediaId = MediaProvider.getInstance().getMediaId(metadataCompat.getDescription().getMediaId());

            if (mCallback != null)
                mCallback.onMetadataChanged(mMediaMetadata);

            mPlayWhenReady = false;
            setTrack(metadataCompat);
            if (timing != null)
                internalSeekTo(timing.start);
        }
    }

    private void setTrack(MediaMetadata track) {
        track.setTimingSeeked(false);
        @SuppressWarnings("ResourceType") String source = track.getString(MediaProvider.CUSTOM_METADATA_TRACK_SOURCE);
        @SuppressWarnings("ResourceType") int type = (int) track.getLong(MediaProvider.CUSTOM_METADATA_TRACK_TYPE);
        if (LOG.isLoggable(Log.INFO))
            LOG.i("setTrack=" + source + ", type=" + type);
        createMediaPlayer(source, type);

        if (mPlayWhenReady)
            mState = PlaybackStateCompat.STATE_BUFFERING;

        if (mCallback != null)
            mCallback.onPlaybackStatusChanged(mState);
    }

    private boolean isMediaPlayerPlaying() {
        if (mMediaPlayer == null || !mMediaPlayer.getPlayWhenReady())
            return false;
        int state = mMediaPlayer.getPlaybackState();
        return state == Player.STATE_READY || state == Player.STATE_BUFFERING;
    }

    @Override
    public void pause() {
        LOG.d("pause");
        mPlayWhenReady = false;
        if (isPlaying()) {
            // Pause media player and cancel the 'foreground service' state.
            if (isMediaPlayerPlaying()) {
                mCurrentPosition = getCurrentStreamPosition();
                mMediaPlayer.setPlayWhenReady(false);
            }
        }
        // while paused, retain the MediaPlayer but give up audio focus
        relaxResources(false);
        giveUpAudioFocus();
        mState = PlaybackStateCompat.STATE_PAUSED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        unregisterAudioNoisyReceiver();
    }

    @Override
    protected long internalGetDuration() {
        return mMediaPlayer == null ? -1 : mMediaPlayer.getDuration();
    }

    @Override
    protected void internalSeekTo(long position) {
        if (LOG.isLoggable(Log.INFO))
            LOG.d("internalSeekTo");
        mCurrentPosition = position;
        if (mMediaPlayer == null) {
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }
        } else {
            if (isPlaying()) {
                mState = PlaybackStateCompat.STATE_BUFFERING;
            }
            mMediaPlayer.seekTo(position);
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }
        }
    }

    @Override
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setCurrentStreamPosition(long pos) {
        this.mCurrentPosition = pos;
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

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        LOG.d("tryToGetAudioFocus");
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        LOG.d("giveUpAudioFocus");
        if (mAudioFocus == AUDIO_FOCUSED) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private void configMediaPlayerState(boolean updateMetaData, boolean forcePlay) {
        if (LOG.isLoggable(Log.DEBUG))
            LOG.d("configMediaPlayerState. mAudioFocus=" + mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (isPlaying()) {
                pause();
            }
        } else {  // we have audio focus:
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(VOLUME_DUCK); // we'll be relatively quiet
                }
            } else {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain && mPlayWhenReady) {
                if (!isMediaPlayerPlaying()) {
                    if (LOG.isLoggable(Log.INFO))
                        LOG.d("configMediaPlayerState startMediaPlayer. seeking to " + mCurrentPosition);
                    if (mState == PlaybackStateCompat.STATE_PAUSED || mState == PlaybackStateCompat.STATE_STOPPED) {
                        if (forcePlay || mCurrentPosition != mMediaPlayer.getCurrentPosition()) {
                            if (!mWakeLock.isHeld())
                                mWakeLock.acquire();
                            if (!mWifiLock.isHeld())
                                mWifiLock.acquire();
                            mState = PlaybackStateCompat.STATE_BUFFERING;
                            mMediaPlayer.seekTo(mCurrentPosition);
                            mMediaPlayer.setPlayWhenReady(true);
                        } else
                            mState = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        mMediaPlayer.seekTo(mCurrentPosition);
                        mState = PlaybackStateCompat.STATE_BUFFERING;
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
        if (mCallback != null) {
            if (updateMetaData)
                mCallback.onMetadataChanged(mMediaMetadata);
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        if (LOG.isLoggable(Log.INFO))
            LOG.d("onAudioFocusChange. focusChange=" + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            LOG.e("onAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
        }
        configMediaPlayerState(false, false);
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    protected DataSource.Factory buildDataSourceFactory(Context context, boolean useBandwidthMeter, boolean http) {
        return http
                ? buildHttpDataSourceFactory(context, useBandwidthMeter)
                : buildFileDataSourceFactory(useBandwidthMeter);
    }

    protected HttpDataSource.Factory buildHttpDataSourceFactory(Context context, boolean useBandwidthMeter) {
        final String userAgent = Util.getUserAgent(context, "NucleiPlayer");
        return new DefaultHttpDataSourceFactory(userAgent, useBandwidthMeter ? BANDWIDTH_METER : null, 15000, 15000, false);
    }

    protected DataSource.Factory buildFileDataSourceFactory(boolean useBandwidthMeter) {
        return new FileDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    protected SimpleExoPlayer newMediaPlayer(Context context, String url, int type) {
        return ExoPlayerFactory.newSimpleInstance(context,
                new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(BANDWIDTH_METER)));
    }

    protected MediaSource newMediaSource(Context context, String url, int type, Handler handler) {
        boolean hls = false;
        boolean localFile = url.startsWith("file://");
        if (!localFile) {
            try {
                hls = type == MediaId.TYPE_VIDEO || Uri.parse(url).getPath().endsWith(".m3u8");
            } catch (Exception ignore) {
            }
        }
        // expecting MP3 here ... otherwise HLS
        if ((localFile || type == MediaId.TYPE_AUDIO) && !hls) {
            return new ExtractorMediaSource.Factory(buildDataSourceFactory(context, true, !localFile))
                    .createMediaSource(Uri.parse(url));
        } else {
            return new HlsMediaSource.Factory(buildDataSourceFactory(context, true, true))
                    .setMinLoadableRetryCount(5)
                    .createMediaSource(Uri.parse(url));
        }
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayer(String url, int type) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer.removeListener(this);
        }
        mPrepared = false;
        mMediaPlayer = newMediaPlayer(mService.getApplicationContext(), url, type);
        mMediaPlayer.addListener(this);

        MediaSource mediaSource = newMediaSource(mService.getApplicationContext(), url, type, mHandler);
        mMediaPlayer.prepare(mediaSource);

        // Make sure the media player will acquire a wake-lock while
        // playing. If we don't do that, the CPU might go to sleep while the
        // song is playing, causing playback to stop.
        if (!mWakeLock.isHeld())
            mWakeLock.acquire();
        if (!mWifiLock.isHeld())
            mWifiLock.acquire();
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (LOG.isLoggable(Log.DEBUG))
            LOG.d("onStateChanged=" + playbackState + ", " + playWhenReady);
        if (!mPrepared && playbackState == Player.STATE_READY && mMediaPlayer != null) {
            mPrepared = true;
            if (!mWakeLock.isHeld())
                mWakeLock.acquire();
            if (!mWifiLock.isHeld())
                mWifiLock.acquire();
            configMediaPlayerState(true, false);
            setSurface(mSurfaceId, mSurface);
            mMediaPlayer.seekTo(mCurrentPosition);
            mMediaPlayer.setPlayWhenReady(mPlayWhenReady);
        } else if (mMediaPlayer != null
                && mState != PlaybackStateCompat.STATE_ERROR
                && mState != PlaybackStateCompat.STATE_BUFFERING)
            mCurrentPosition = mMediaPlayer.getCurrentPosition();

        if (mMediaPlayer != null && mMediaMetadata != null) {
            final long duration = getDuration();
            if (mMediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) != duration)
                mMediaMetadata.setDuration(duration);
        }

        switch (playbackState) {
            case Player.STATE_BUFFERING:
                mState = PlaybackStateCompat.STATE_BUFFERING;
                mIllegalStateRetries = 0;
                break;
            case Player.STATE_ENDED:
                mState = PlaybackStateCompat.STATE_NONE;
                mIllegalStateRetries = 0;
                break;
            case Player.STATE_IDLE:
                if (mState != PlaybackStateCompat.STATE_ERROR)
                    mState = PlaybackStateCompat.STATE_NONE;
                break;
            case Player.STATE_READY:
                mIllegalStateRetries = 0;
                if (isMediaPlayerPlaying())
                    mState = PlaybackStateCompat.STATE_PLAYING;
                else
                    mState = PlaybackStateCompat.STATE_PAUSED;
                break;
            default:
                mState = PlaybackStateCompat.STATE_NONE;
                break;
        }

        if (mCallback != null)
            mCallback.onPlaybackStatusChanged(mState);

        if (playbackState == Player.STATE_ENDED) {
            mRestart = true;
            if (mCallback != null)
                mCallback.onCompletion();
        } else if (mState != PlaybackStateCompat.STATE_NONE) {
            if (mMediaPlayer != null && mPlaybackParams != null)
                mMediaPlayer.setPlaybackParameters(mPlaybackParams);
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        onError(error);
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onMediaPeriodCreated(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {

    }

    @Override
    public void onMediaPeriodReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {

    }

    @Override
    public void onLoadStarted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {

    }

    @Override
    public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {

    }

    @Override
    public void onLoadCanceled(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {

    }

    @Override
    public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {

    }

    @Override
    public void onReadingStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {

    }

    @Override
    public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {

    }

    @Override
    public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {

    }

    private void onError(Exception e) {
        LOG.e("onError", e);

        if (e instanceof ExoPlaybackException && e.getCause() instanceof IllegalStateException) {
            final int maxRetries = 4;
            if (mIllegalStateRetries < maxRetries) {
                mIllegalStateRetries++;
                pause();
                relaxResources(true);
                if (mMediaMetadata != null) {
                    play(mMediaMetadata);
                    return;
                }
            }
        }

        Throwable err = e;
        do {
            if (err instanceof IOException) {
                if (mCallback != null) {
                    mCallback.onError(e, false);
                    long pos = getCurrentStreamPosition();
                    stop(true);
                    mCurrentPosition = pos;
                }
                return;
            }
            err = err.getCause();
        } while (err != null);

        if (mCallback != null) {
            stop(true);
            mCallback.onError(e, false);
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *                           be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        if (LOG.isLoggable(Log.DEBUG))
            LOG.d("relaxResources. releaseMediaPlayer=" + releaseMediaPlayer);

        mService.stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mSurface = null;
            mMediaPlayer.setVideoSurface(null);
            mMediaPlayer.release();
            mMediaPlayer.removeListener(this);
            mMediaPlayer = null;
            mPrepared = false;
        }
        if (mWifiLock.isHeld())
            mWifiLock.release();
        if (mWakeLock.isHeld())
            mWakeLock.release();
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mService.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mService.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
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
    public void setSurface(final long surfaceId, final Surface surface) {
        if (surface == null && mSurfaceId != surfaceId)
            return;
        mSurfaceId = surfaceId;
        mSurface = surface;
        if (mMediaPlayer != null)
            mMediaPlayer.setVideoSurface(surface);
    }

    @Override
    public void setPlaybackParams(PlaybackParameters playbackParams) {
        mPlaybackParams = playbackParams;
        if (mMediaPlayer != null)
            mMediaPlayer.setPlaybackParameters(playbackParams);
        if (mCallback != null)
            mCallback.onPlaybackStatusChanged(mState);
    }

}
