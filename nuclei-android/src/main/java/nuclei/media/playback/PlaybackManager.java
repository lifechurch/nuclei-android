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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Surface;

import java.lang.ref.WeakReference;

import nuclei.media.MediaId;
import nuclei.media.MediaMetadata;
import nuclei.media.MediaProvider;
import nuclei.media.MediaService;
import nuclei.media.Queue;
import nuclei.media.QueueItem;
import nuclei.task.Result;
import nuclei.logs.Log;
import nuclei.logs.Logs;

public class PlaybackManager implements Playback.Callback {

    private static final Log LOG = Logs.newLog(PlaybackManager.class);

    public static final int ONE_SECOND = 1000;
    public static final int THIRY_SECOND = 30000;

    public static final int FIVE_MINUTES = 300000;
    public static final int TEN_MINUTES = 600000;
    public static final int FIFTEEN_MINUTES = 900000;
    public static final int THIRY_MINUTES = 1800000;
    public static final int ONE_HOUR = 3600000;
    public static final int TWO_HOUR = 7200000;

    private static final int TIMER_COUNTDOWN = 1;
    private static final int TIMER_TIMING = 2;

    private MediaMetadata mMediaMetadata;
    private Playback mPlayback;
    private PlaybackServiceCallback mServiceCallback;
    private MediaSessionCallback mMediaSessionCallback;
    private long mTimer = -1;
    private final PlaybackHandler mHandler = new PlaybackHandler(this);
    private Queue mQueue;

    public PlaybackManager(PlaybackServiceCallback serviceCallback, Playback playback) {
        mServiceCallback = serviceCallback;
        mMediaSessionCallback = new MediaSessionCallback();
        mPlayback = playback;
        mPlayback.setCallback(this);
        mPlayback.start();
    }

    public Playback getPlayback() {
        return mPlayback;
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mMediaSessionCallback;
    }

    public void handlePrepareRequest() {
        if (mMediaMetadata != null) {
            MediaId id = MediaProvider.getInstance().getMediaId(mMediaMetadata.getDescription().getMediaId());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (id.type == MediaId.TYPE_AUDIO)
                    mPlayback.setPlaybackParams(new PlaybackParams()
                            .allowDefaults()
                            .setSpeed(mServiceCallback.getAudioSpeed()));
                else
                    mPlayback.setPlaybackParams(new PlaybackParams().allowDefaults());
            }
            mServiceCallback.onPlaybackPrepare(id);
            mPlayback.prepare(mMediaMetadata);

            mHandler.removeMessages(TIMER_COUNTDOWN);
            mHandler.removeMessages(TIMER_TIMING);

            if (mQueue != null && !mQueue.setMetadata(mMediaMetadata)) {
                mQueue = null;
                mServiceCallback.onQueue(null);
            }
        }
    }

    public void handlePlayRequest() {
        if (mMediaMetadata != null) {
            MediaId id = MediaProvider.getInstance().getMediaId(mMediaMetadata.getDescription().getMediaId());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (id.type == MediaId.TYPE_AUDIO)
                    mPlayback.setPlaybackParams(new PlaybackParams()
                            .allowDefaults()
                            .setSpeed(mServiceCallback.getAudioSpeed()));
                else
                    mPlayback.setPlaybackParams(new PlaybackParams().allowDefaults());
            }
            mServiceCallback.onPlaybackStart(id);
            mServiceCallback.onNotificationRequired();
            mPlayback.play(mMediaMetadata);

            if (mTimer > -1) {
                mHandler.removeMessages(TIMER_COUNTDOWN);
                mHandler.sendEmptyMessageDelayed(TIMER_COUNTDOWN, ONE_SECOND);
            }

            if (mPlayback.getTiming() != null) {
                mHandler.removeMessages(TIMER_TIMING);
                mHandler.sendEmptyMessageDelayed(TIMER_TIMING, ONE_SECOND);
            }

            if (mQueue != null && !mQueue.setMetadata(mMediaMetadata)) {
                mQueue = null;
                mServiceCallback.onQueue(null);
            }
        }
    }

    public void handlePauseRequest() {
        if (mPlayback.isPlaying()) {
            mPlayback.pause();
            MediaId mediaId = mPlayback.getCurrentMediaId();
            mServiceCallback.onPlaybackPause(mediaId);
        }
    }

    public void handleStopRequest(String withError) {
        mPlayback.stop(true);
        MediaId mediaId = mPlayback.getCurrentMediaId();
        mServiceCallback.onPlaybackStop(mediaId);
        updatePlaybackState(withError);
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    public void updatePlaybackState(String error) {
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
            position = mPlayback.getCurrentStreamPosition();
        }

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        int state = mPlayback == null ? -1 : mPlayback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
            if (mPlayback != null) {
                int lastState = state;
                mPlayback.setState(state);
                if (mPlayback.isPlaying()) {
                    mPlayback.setState(lastState);
                    state = lastState;
                } else {
                    mPlayback.pause();
                }
            }
            mHandler.removeMessages(TIMER_COUNTDOWN);
            mHandler.removeMessages(TIMER_TIMING);
            MediaProvider.getInstance().onError(error);
        }
        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        mServiceCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            mServiceCallback.onNotificationRequired();
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_URI
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_PREPARE
                | PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PREPARE_FROM_URI
                | PlaybackStateCompat.ACTION_REWIND
                | PlaybackStateCompat.ACTION_FAST_FORWARD;
        if (mPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        if (mQueue != null) {
            if (mQueue.hasNext() || mQueue.getNextQueue() != null)
                actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
            if (mQueue.hasPrevious() || mQueue.getPreviousQueue() != null)
                actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
        }
        return actions;
    }

    @Override
    public void onCompletion() {
        mServiceCallback.onCompletion();
        if (mQueue != null) {
            if ((mQueue.hasNext() || mQueue.getNextQueue() != null) && mMediaSessionCallback != null) {
                if (mPlayback.getTiming() != null)
                    mPlayback.temporaryStop();
                mMediaSessionCallback.onSkipToNext();
            } else {
                mQueue = null;
                mServiceCallback.onQueue(null);
                handleStopRequest(null);
            }
        } else {
            // If skipping was not possible, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void onMetadataChanged(MediaMetadata metadataCompat) {
        mServiceCallback.onMetadataUpdated(metadataCompat);
    }

    /**
     * Switch to a different Playback instance, maintaining all playback state, if possible.
     *
     * @param playback switch to this playback
     */
    public void switchToPlayback(Playback playback, boolean resumePlaying) {
        if (playback == null) {
            throw new IllegalArgumentException("Playback cannot be null");
        }
        // suspend the current one.
        int oldState = mPlayback.getState();
        long pos = mPlayback.getCurrentStreamPosition();
        MediaId currentMediaId = mPlayback.getCurrentMediaId();
        mPlayback.stop(false);
        playback.setCallback(this);
        playback.setCurrentStreamPosition(pos < 0 ? 0 : pos);
        playback.setCurrentMediaId(currentMediaId);
        playback.start();
        // finally swap the instance
        mPlayback = playback;
        switch (oldState) {
            case PlaybackStateCompat.STATE_BUFFERING:
            case PlaybackStateCompat.STATE_CONNECTING:
            case PlaybackStateCompat.STATE_PAUSED:
                mPlayback.pause();
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                if (resumePlaying && mMediaMetadata != null) {
                    mPlayback.play(mMediaMetadata);
                } else if (!resumePlaying) {
                    mPlayback.pause();
                } else {
                    mPlayback.stop(true);
                }
                break;
            case PlaybackStateCompat.STATE_NONE:
                break;
            default:

        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            handlePlayRequest();
        }

        @Override
        public void onSeekTo(long position) {
            long current = mPlayback.getCurrentStreamPosition();
            mPlayback.seekTo((int) position);
            mServiceCallback.onPlaybackSeekTo(mPlayback.getCurrentMediaId(), current, position);
        }

        private void onQueue(Queue queue, final Bundle extras, boolean play) {
            mQueue = queue;
            if (mQueue != null && !mQueue.empty()) {
                mServiceCallback.onQueue(mQueue);
                if (play)
                    onPlayFromMediaId(mQueue.getCurrentId(), extras);
                else
                    onPrepareFromMediaId(mQueue.getCurrentId(), extras);
            } else {
                mServiceCallback.onQueue(null);
                onStop();
            }
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            onPlayFromMediaId(uri.toString(), extras);
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            onPrepareFromMediaId(uri.toString(), extras);
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, final Bundle extras) {
            final MediaId id = MediaProvider.getInstance().getMediaId(mediaId);
            if (id.queue) {
                try {
                    onQueue(MediaProvider.getInstance().getCachedQueue(id), extras, false);
                } catch (NullPointerException err) {
                    MediaProvider.getInstance()
                            .getQueue(id)
                            .addCallback(new Result.CallbackAdapter<Queue>() {
                                @Override
                                public void onResult(Queue queue) {
                                    onQueue(queue, extras, false);
                                }

                                @Override
                                public void onException(Exception err) {
                                    LOG.e("Error getting metadata", err);
                                    onError(err.getMessage());
                                }
                            });
                }
            } else {
                try {
                    mMediaMetadata = MediaProvider.getInstance().getCachedMedia(id);
                    handlePrepareRequest();
                } catch (NullPointerException err) {
                    MediaProvider.getInstance()
                            .getMediaMetadata(id)
                            .addCallback(new Result.CallbackAdapter<MediaMetadata>() {
                                @Override
                                public void onResult(MediaMetadata mediaMetadata) {
                                    mMediaMetadata = mediaMetadata;
                                    handlePrepareRequest();
                                }

                                @Override
                                public void onException(Exception err) {
                                    LOG.e("Error getting metadata", err);
                                    onError(err.getMessage());
                                }
                            });
                }
            }
        }

        @Override
        public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
            final MediaId id = MediaProvider.getInstance().getMediaId(mediaId);
            if (id.queue) {
                try {
                    onQueue(MediaProvider.getInstance().getCachedQueue(id), extras, true);
                } catch (NullPointerException err) {
                    MediaProvider.getInstance()
                            .getQueue(id)
                            .addCallback(new Result.CallbackAdapter<Queue>() {
                                @Override
                                public void onResult(Queue queue) {
                                    onQueue(queue, extras, true);
                                }

                                @Override
                                public void onException(Exception err) {
                                    LOG.e("Error getting metadata", err);
                                    onError(err.getMessage());
                                }
                            });
                }
            } else {
                try {
                    mMediaMetadata = MediaProvider.getInstance().getCachedMedia(id);
                    handlePlayRequest();
                } catch (NullPointerException err) {
                    MediaProvider.getInstance()
                            .getMediaMetadata(id)
                            .addCallback(new Result.CallbackAdapter<MediaMetadata>() {
                                @Override
                                public void onResult(MediaMetadata mediaMetadata) {
                                    mMediaMetadata = mediaMetadata;
                                    handlePlayRequest();
                                }

                                @Override
                                public void onException(Exception err) {
                                    LOG.e("Error getting metadata", err);
                                    onError(err.getMessage());
                                }
                            });
                }
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            mPlayback.setState(PlaybackStateCompat.STATE_CONNECTING);
            MediaProvider.getInstance()
                    .search(query)
                    .addCallback(new Result.CallbackAdapter<String>() {
                        @Override
                        public void onResult(final String mediaId) {
                            if (mediaId == null)
                                updatePlaybackState("Could not find music");
                            else {
                                MediaId id = MediaProvider.getInstance().getMediaId(mediaId);
                                MediaProvider.getInstance()
                                        .getMediaMetadata(id)
                                        .addCallback(new Result.CallbackAdapter<MediaMetadata>() {
                                            @Override
                                            public void onResult(MediaMetadata mediaMetadata) {
                                                mMediaMetadata = mediaMetadata;
                                                handlePlayRequest();
                                            }
                                        });
                            }
                        }

                        @Override
                        public void onException(Exception err) {
                            updatePlaybackState(err.getMessage());
                        }
                    });
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            handleStopRequest(null);
        }

        @Override
        public void onFastForward() {
            long current = mPlayback.getCurrentStreamPosition();
            long position = current + THIRY_SECOND;
            mPlayback.seekTo(position);
            mServiceCallback.onPlaybackSeekTo(mPlayback.getCurrentMediaId(), current, position);
        }

        @Override
        public void onRewind() {
            long current = mPlayback.getCurrentStreamPosition();
            long position = Math.max(0, current - THIRY_SECOND);
            mPlayback.seekTo(position);
            mServiceCallback.onPlaybackSeekTo(mPlayback.getCurrentMediaId(), current, position);
        }

        @Override
        public void onSkipToNext() {
            mServiceCallback.onPlaybackNext(mPlayback.getCurrentMediaId());
            if (mQueue != null) {
                if (mQueue.hasNext()) {
                    final QueueItem item = mQueue.next();
                    final String mediaId = item.getMediaId();
                    onPlayFromMediaId(mediaId, null);
                } else {
                    MediaId queueId = mQueue.getNextQueue();
                    onPlayFromMediaId(queueId.toString(), null);
                }
            }
        }

        @Override
        public void onSkipToPrevious() {
            mServiceCallback.onPlaybackPrevious(mPlayback.getCurrentMediaId());
            if (mQueue != null) {
                if (mQueue.hasPrevious()) {
                    final QueueItem item = mQueue.previous();
                    final String mediaId = item.getMediaId();
                    onPlayFromMediaId(mediaId, null);
                } else {
                    MediaId queueId = mQueue.getPreviousQueue();
                    onPlayFromMediaId(queueId.toString(), null);
                }
            }
        }

        @Override
        public void onSkipToQueueItem(long id) {
            if (mQueue != null) {
                final QueueItem item = mQueue.moveToId(id);
                if (item != null) {
                    final String mediaId = item.getMediaId();
                    onPlayFromMediaId(mediaId, null);
                }
            }
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            switch (action) {
                case MediaService.ACTION_SET_SURFACE:
                    long surfaceId = extras.getLong(MediaService.EXTRA_SURFACE_ID);
                    if (!extras.containsKey(MediaService.EXTRA_SURFACE)) {
                        mPlayback.setSurface(surfaceId, null);
                    } else {
                        final Surface surface = extras.getParcelable(MediaService.EXTRA_SURFACE);
                        mPlayback.setSurface(surfaceId, surface);
                    }
                    break;
                case MediaService.ACTION_SET_SPEED:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        final float speed = extras.getFloat(MediaService.EXTRA_SPEED);
                        mServiceCallback.onSpeedSet(speed);
                        mPlayback.setPlaybackParams(new PlaybackParams()
                                .allowDefaults()
                                .setSpeed(speed));
                    }
                    break;
                case MediaService.ACTION_SET_TIMER:
                    mTimer = extras.getLong(MediaService.EXTRA_TIMER);
                    mHandler.removeMessages(TIMER_COUNTDOWN);
                    if (mTimer != -1)
                        mHandler.sendEmptyMessageDelayed(TIMER_COUNTDOWN, ONE_SECOND);
                    mServiceCallback.onTimerCount(mTimer);
                    break;
                default:
                    break;
            }
        }

    }

    public interface PlaybackServiceCallback {

        float getAudioSpeed();

        void onQueue(Queue queue);

        void onSpeedSet(float speed);

        void onPlaybackPrepare(MediaId mediaId);

        void onPlaybackStart(MediaId mediaId);

        void onNotificationRequired();

        void onTimerCount(long timeRemainingMs);

        void onPlaybackPause(MediaId mediaId);

        void onPlaybackStop(MediaId mediaId);

        void onPlaybackNext(MediaId mediaId);

        void onPlaybackSeekTo(MediaId mediaId, long currentPosition, long newPosition);

        void onPlaybackPrevious(MediaId mediaId);

        void onMetadataUpdated(MediaMetadata mediaMetadataCompat);

        void onPlaybackStateUpdated(PlaybackStateCompat newState);

        void onCompletion();

    }

    private static class PlaybackHandler extends Handler {

        private WeakReference<PlaybackManager> mManager;

        PlaybackHandler(PlaybackManager manager) {
            mManager = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TIMER_COUNTDOWN: {
                    PlaybackManager playback = mManager.get();
                    if (playback != null && playback.mPlayback != null && playback.mPlayback.isPlaying()) {
                        if (playback.mTimer <= 0) {
                            playback.mHandler.removeMessages(TIMER_COUNTDOWN);
                            playback.mTimer = -1;
                            playback.handlePauseRequest();
                        } else {
                            playback.mTimer -= ONE_SECOND;
                            playback.mServiceCallback.onTimerCount(playback.mTimer);
                            sendEmptyMessageDelayed(TIMER_COUNTDOWN, ONE_SECOND);
                        }
                    }
                    break;
                }
                case TIMER_TIMING: {
                    PlaybackManager playback = mManager.get();
                    if (playback != null && playback.mPlayback != null && playback.mPlayback.isPlaying()) {
                        Timing timing = playback.mPlayback.getTiming();
                        if (timing != null) {
                            long pos = playback.mPlayback.getStartStreamPosition() + playback.mPlayback.getCurrentStreamPosition();
                            if (timing.end > pos) {
                                sendEmptyMessageDelayed(TIMER_TIMING, ONE_SECOND);
                            } else {
                                playback.onCompletion();
                            }
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

}
