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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.Surface;

import java.io.IOException;

import nuclei.logs.Log;
import nuclei.logs.Logs;
import nuclei.media.MediaId;
import nuclei.media.MediaMetadata;
import nuclei.media.MediaProvider;
import nuclei.media.MediaService;

import static android.media.MediaPlayer.OnCompletionListener;
import static android.media.MediaPlayer.OnErrorListener;
import static android.media.MediaPlayer.OnPreparedListener;
import static android.media.MediaPlayer.OnSeekCompleteListener;

/**
 * A class that implements local media playback using {@link MediaPlayer}.
 */
public class FallbackPlayback extends BasePlayback implements Playback, AudioManager.OnAudioFocusChangeListener,
        OnCompletionListener, OnErrorListener, OnPreparedListener, OnSeekCompleteListener {

    private static final Log LOG = Logs.newLog(FallbackPlayback.class);

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    private final WifiManager.WifiLock mWifiLock;
    private int mState;
    private boolean mPlayOnFocusGain;
    private Callback mCallback;
    private volatile boolean mAudioNoisyReceiverRegistered;
    private volatile long mCurrentPosition;
    private volatile MediaId mCurrentMediaId;
    private volatile MediaMetadata mMetadata;

    private volatile boolean mPrepared;

    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private final AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private long mSurfaceId;
    private Surface mSurface;
    private PlaybackParams mPlaybackParams;

    private MediaService mService;

    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (isPlaying()) {
                    Intent i = new Intent(context, MediaService.class);
                    i.setAction(MediaService.ACTION_CMD);
                    i.putExtra(MediaService.CMD_NAME, MediaService.CMD_PAUSE);
                    mService.startService(i);
                }
            }
        }
    };

    public FallbackPlayback(MediaService service) {
        mService = service;
        this.mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        this.mWifiLock = ((WifiManager) mService.getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "uAmp_lock");
        this.mState = PlaybackStateCompat.STATE_NONE;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop(boolean notifyListeners) {
        if (mMetadata != null)
            mMetadata.setTimingSeeked(false);
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        mCurrentPosition = getCurrentStreamPosition();
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        relaxResources(true);
    }

    @Override
    public void temporaryStop() {
        if (mMetadata != null)
            mMetadata.setTimingSeeked(false);
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        if (mMediaPlayer != null)
            mMediaPlayer.stop();
        relaxResources(false);
    }

    @Override
    public void setState(int state) {
        this.mState = state;
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    private boolean isMediaPlayerPlaying() {
        return mPrepared && mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    @Override
    public boolean isPlaying() {
        return mPlayOnFocusGain || isStatePlaying();
    }

    private boolean isStatePlaying() {
        return (isMediaPlayerPlaying()
                || mState == PlaybackStateCompat.STATE_PLAYING
                || mState == PlaybackStateCompat.STATE_BUFFERING
                || mState == PlaybackStateCompat.STATE_CONNECTING);
    }

    @Override
    protected long internalGetDuration() {
        return !mPrepared || mMediaPlayer == null ? -1 : mMediaPlayer.getDuration();
    }

    @Override
    protected long internalGetCurrentStreamPosition() {
        return mPrepared && mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        mCurrentPosition = getCurrentStreamPosition();
    }

    @Override
    protected void internalPlay(MediaMetadata metadataCompat, Timing timing, boolean seek) {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        boolean mediaHasChanged = mCurrentMediaId == null
                || !TextUtils.equals(metadataCompat.getDescription().getMediaId(), mCurrentMediaId.toString());
        if (mediaHasChanged) {
            mCurrentPosition = getStartStreamPosition();
            mMetadata = metadataCompat;
            mCurrentMediaId = MediaProvider.getInstance().getMediaId(metadataCompat.getDescription().getMediaId());
        }

        if (mState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mMediaPlayer != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false); // release everything except MediaPlayer
            //noinspection ResourceType
            String source = metadataCompat.getString(MediaProvider.CUSTOM_METADATA_TRACK_SOURCE);

            try {
                createMediaPlayerIfNeeded();

                mState = PlaybackStateCompat.STATE_BUFFERING;

                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(source);
                // Starts preparing the media player in the background. When
                // it's done, it will call our OnPreparedListener (that is,
                // the onPrepared() method on this class, since we set the
                // listener to 'this'). Until the media player is prepared,
                // we *cannot* call start() on it!
                mMediaPlayer.prepareAsync();

                // If we are streaming from the internet, we want to hold a
                // Wifi lock, which prevents the Wifi radio from going to
                // sleep while the song is playing.
                mWifiLock.acquire();

                if (mCallback != null) {
                    mCallback.onPlaybackStatusChanged(mState);
                }

            } catch (Exception ex) {
                if (mCallback != null) {
                    mCallback.onError(ex, true);
                }
            }
        }

        if (timing != null && seek)
            internalSeekTo(timing.start);
    }

    @Override
    protected void internalPrepare(MediaMetadata metadataCompat, Timing timing) {
        boolean mediaHasChanged = mCurrentMediaId == null
                || !TextUtils.equals(metadataCompat.getDescription().getMediaId(), mCurrentMediaId.toString());
        if (mediaHasChanged) {
            mCurrentPosition = getStartStreamPosition();
            mMetadata = metadataCompat;
            mMetadata.setCallback(mCallback);
            mCurrentMediaId = MediaProvider.getInstance().getMediaId(metadataCompat.getDescription().getMediaId());
            if (mCallback != null) {
                mCallback.onMetadataChanged(mMetadata);
                mCallback.onPlaybackStatusChanged(mState);
            }
            if (timing != null)
                internalSeekTo(timing.start);
        }
    }

    @Override
    public void pause() {
        if (isPlaying()) {
            // Pause media player and cancel the 'foreground service' state.
            if (isMediaPlayerPlaying()) {
                mCurrentPosition = getCurrentStreamPosition();
                mMediaPlayer.pause();
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
    protected void internalSeekTo(long position) {
        if (!mPrepared || mMediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackStateCompat.STATE_BUFFERING;
            }
            mMediaPlayer.seekTo((int) position);
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
        mMetadata = metadata;
    }

    @Override
    public MediaId getCurrentMediaId() {
        return mCurrentMediaId;
    }

    @Override
    public MediaMetadata getCurrentMetadata() {
        return mMetadata;
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
        if (surface == null && mSurfaceId != surfaceId)
            return;
        mSurfaceId = surfaceId;
        mSurface = surface;
        if (mPrepared && mMediaPlayer != null)
            mMediaPlayer.setSurface(surface);
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
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
    private void configMediaPlayerState() {
        if (!mPrepared)
            return;
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                pause();
            }
        } else {  // we have audio focus:
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (mMediaPlayer != null) {
                    if (!mMediaPlayer.isPlaying()) {
                        if (mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                            mState = PlaybackStateCompat.STATE_PLAYING;
                            mMediaPlayer.start();
                        } else {
                            mState = PlaybackStateCompat.STATE_BUFFERING;
                            mMediaPlayer.seekTo((int) mCurrentPosition);
                        }
                    } else {
                        mState = PlaybackStateCompat.STATE_PLAYING;
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
        if (mState == PlaybackStateCompat.STATE_PLAYING)
            mIllegalStateRetries = 0;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
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
        }
        configMediaPlayerState();
    }

    /**
     * Called when MediaPlayer has completed a seek.
     *
     * @see OnSeekCompleteListener
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mCurrentPosition = mp.getCurrentPosition();
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            if (!mMediaPlayer.isPlaying())
                mMediaPlayer.start();
            mState = PlaybackStateCompat.STATE_PLAYING;
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    /**
     * Called when media player is done playing current song.
     *
     * @see OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        mIllegalStateRetries = 0;
        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    /**
     * Called when media player is done preparing.
     *
     * @see OnPreparedListener
     */
    @Override
    public void onPrepared(MediaPlayer player) {
        mPrepared = true;
        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        if (mSurface != null && mSurface.isValid())
            player.setSurface(mSurface);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mPlaybackParams != null)
                player.setPlaybackParams(mPlaybackParams);
        }

        if (mMediaPlayer != null && mMetadata != null && mMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) != mMediaPlayer.getDuration()) {
            mMetadata.setDuration(getDuration());
            if (mCallback != null)
                mCallback.onMetadataChanged(mMetadata);
        }

        configMediaPlayerState();
    }

    int mIllegalStateRetries;

    /**
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player.
     *
     * @see OnErrorListener
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LOG.e("onError", new Exception("MediaPlayer error " + what + " (" + extra + ")"));

        final int maxRetries = 4;
        if (mIllegalStateRetries < maxRetries) {
            mIllegalStateRetries++;
            pause();
            relaxResources(true);
            if (mMetadata != null) {
                play(mMetadata);
                return true;
            }
        }

        if (mCallback != null) {
            stop(true);
            mCallback.onError(new Exception("MediaPlayer error " + what + " (" + extra + ")"), true);
        }
        return true; // true indicates we handled the error
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {
        mPrepared = false;
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mMediaPlayer.setWakeMode(mService.getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mMediaPlayer.reset();
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
        mService.stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mPrepared = false;
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
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
    public void setPlaybackParams(PlaybackParams playbackParams) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mMediaPlayer == null || !mPrepared) {
                mPlaybackParams = playbackParams;
            } else {
                mMediaPlayer.setPlaybackParams(playbackParams);

                if (mState != PlaybackStateCompat.STATE_PLAYING && mMediaPlayer.isPlaying()) {
                    mState = PlaybackStateCompat.STATE_PLAYING;
                }
                if (mCallback != null) {
                    mCallback.onPlaybackStatusChanged(mState);
                }
            }
        }
    }
}
