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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

import nuclei3.logs.Log;
import nuclei3.logs.Logs;
import nuclei3.media.playback.PlaybackManager;

public class MediaInterface implements LifecycleObserver {

    private static final Log LOG = Logs.newLog(MediaInterface.class);

    public static final String ACTION_CONNECTED = "nuclei.media.interface.CONNECTED";

    private static final AtomicLong SURFACE_ID = new AtomicLong(1);

    private Context mAppliationContext;
    private Activity mLActivity;
    private FragmentActivity mFragmentActivity;
    MediaInterfaceCallback mCallbacks;
    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaControls;
    private MediaControllerCompat.Callback mMediaCallback;
    private MediaPlayerController mPlayerControls;
    private ProgressHandler mProgressHandler = new ProgressHandler(this);
    private Handler mCallbackHandler = new Handler(Looper.getMainLooper());
    private Surface mSurface;
    private long mSurfaceId;

    private MediaId mCurrentId;

    public MediaInterface(FragmentActivity activity, MediaId mediaId, MediaInterfaceCallback callback) {
        mFragmentActivity = activity;
        mAppliationContext = activity.getApplicationContext();
        mCallbacks = callback;
        mPlayerControls = new MediaPlayerController(mediaId);
        mPlayerControls.setMediaControls(mCallbacks, null);
        mMediaBrowser = new MediaBrowserCompat(mAppliationContext,
                new ComponentName(mAppliationContext, MediaService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        MediaInterface.this.onConnected();
                    }
                }, null);
        mMediaBrowser.connect();
        mSurfaceId = SURFACE_ID.incrementAndGet();
        if (SURFACE_ID.longValue() == Long.MAX_VALUE)
            SURFACE_ID.set(1);
    }

    @TargetApi(21)
    public MediaInterface(Activity activity, MediaId mediaId, MediaInterfaceCallback callback) {
        mLActivity = activity;
        mAppliationContext = activity.getApplicationContext();
        mCallbacks = callback;
        mPlayerControls = new MediaPlayerController(mediaId);
        mPlayerControls.setMediaControls(mCallbacks, null);
        mMediaBrowser = new MediaBrowserCompat(mAppliationContext,
                new ComponentName(mAppliationContext, MediaService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        MediaInterface.this.onConnected();
                    }
                }, null);
        mMediaBrowser.connect();
        mSurfaceId = SURFACE_ID.incrementAndGet();
        if (SURFACE_ID.longValue() == Long.MAX_VALUE)
            SURFACE_ID.set(1);
    }

    @TargetApi(21)
    public MediaInterface(Context context, MediaId mediaId, MediaInterfaceCallback callback) {
        mCallbacks = callback;
        mAppliationContext = context.getApplicationContext();
        mPlayerControls = new MediaPlayerController(mediaId);
        mPlayerControls.setMediaControls(mCallbacks, null);
        mMediaBrowser = new MediaBrowserCompat(mAppliationContext,
                new ComponentName(mAppliationContext, MediaService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        MediaInterface.this.onConnected();
                    }
                }, null);
        mMediaBrowser.connect();
        mSurfaceId = SURFACE_ID.incrementAndGet();
        if (SURFACE_ID.longValue() == Long.MAX_VALUE)
            SURFACE_ID.set(1);
    }

    public MediaPlayerController getPlayerController() {
        return mPlayerControls;
    }

    public MediaControllerCompat getMediaController() {
        return mMediaControls;
    }

    public Handler getCallbackHandler() {
        return mCallbackHandler;
    }

    public void autoHide() {
        if (mProgressHandler != null) {
            mProgressHandler.removeMessages(ProgressHandler.AUTO_HIDE);
            mProgressHandler.sendEmptyMessageDelayed(ProgressHandler.AUTO_HIDE, 3000);
        }
    }

    public void cancelAutoHide() {
        if (mProgressHandler != null)
            mProgressHandler.removeMessages(ProgressHandler.AUTO_HIDE);
    }

    public void setSurface(Surface surface) {
        if (mMediaControls != null) {
            final Bundle args = new Bundle();
            args.putParcelable(MediaService.EXTRA_SURFACE, surface);
            args.putLong(MediaService.EXTRA_SURFACE_ID, mSurfaceId);
            mMediaControls.getTransportControls().sendCustomAction(MediaService.ACTION_SET_SURFACE, args);
            mSurface = null;
        } else {
            mSurface = surface;
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        if (mCallbacks != null)
            mCallbacks.onDestroy(this);
        if (mPlayerControls != null) {
            mPlayerControls.setMediaControls(null, null);
        }
        if (mMediaControls != null && mMediaCallback != null)
            mMediaControls.unregisterCallback(mMediaCallback);
        if (mMediaBrowser != null)
            mMediaBrowser.disconnect();
        mMediaControls = null;
        mMediaCallback = null;
        mPlayerControls = null;
        mMediaBrowser = null;
        mCallbacks = null;
        mFragmentActivity = null;
        mLActivity = null;
        mProgressHandler = null;
        mCallbackHandler = null;
    }

    @TargetApi(21)
    private void setMediaControllerL() {
        if (mLActivity != null)
            mLActivity.setMediaController((MediaController) mMediaControls.getMediaController());
    }

    void onConnected() {
        try {
            mMediaControls = new MediaControllerCompat(mAppliationContext, mMediaBrowser.getSessionToken());
            mMediaCallback = new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    MediaInterface.this.onPlaybackStateChanged(state);
                }

                @Override
                public void onSessionEvent(String event, Bundle extras) {
                    MediaInterface.this.onSessionEvent(event, extras);
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    MediaInterface.this.onMetadataChanged(metadata);
                }
            };
            mMediaControls.registerCallback(mMediaCallback, mCallbackHandler);

            if (mFragmentActivity != null) {
                MediaControllerCompat.setMediaController(mFragmentActivity, mMediaControls);
                //mFragmentActivity.setSupportMediaController(mMediaControls);
            } else if (mLActivity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                setMediaControllerL();

            if (mPlayerControls != null)
                mPlayerControls.setMediaControls(mCallbacks, mMediaControls);

            if (mMediaControls.getPlaybackState() != null)
                onPlaybackStateChanged(mMediaControls.getPlaybackState());

            if (mLActivity != null || mFragmentActivity != null) {
                final Intent intent = mFragmentActivity == null ? mLActivity.getIntent() : mFragmentActivity.getIntent();
                if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction())) {
                    final Bundle params = intent.getExtras();
                    if (params != null) {
                        final String query = params.getString(SearchManager.QUERY);
                        LOG.i("Starting from voice search query=" + query);
                        mMediaControls.getTransportControls()
                                .playFromSearch(query, params);
                    }
                }
            }
            if (mCallbacks != null) {
                mCallbacks.onConnected(this);
                MediaMetadataCompat metadataCompat = mMediaControls.getMetadata();
                if (metadataCompat != null) {
                    final long duration = metadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                    if (duration > 0)
                        mCallbacks.setTimeTotal(this, duration);
                }
            }

            Bundle extras = mMediaControls.getExtras();
            if (extras != null && extras.containsKey(MediaService.EXTRA_CONNECTED_CAST)) {
                mCallbacks.onCasting(this, extras.getString(MediaService.EXTRA_CONNECTED_CAST));
            }

            if (mPlayerControls != null && mPlayerControls.isPlaying())
                mProgressHandler.start();

            if (mSurface != null)
                setSurface(mSurface);

            LocalBroadcastManager.getInstance(mAppliationContext).sendBroadcast(new Intent(ACTION_CONNECTED));
        } catch (RemoteException err) {
            LOG.e("Error in onConnected", err);
        }
    }

    void onPlaybackStateChanged(PlaybackStateCompat state) {
        if (mCallbacks != null) {
            if (state.getState() != PlaybackStateCompat.STATE_BUFFERING) {
                mCallbacks.onLoaded(mPlayerControls);
            } else if (state.getState() == PlaybackStateCompat.STATE_BUFFERING) {
                mCallbacks.onLoading(mPlayerControls);
            }
            mCallbacks.onStateChanged(this, state);
        }
        if (mPlayerControls != null) {
            mPlayerControls.mPlaybackState = state;
            if (isPlaying(state)) {
                if (mCallbacks != null)
                    mCallbacks.onPlaying(mPlayerControls);
                if (mProgressHandler != null)
                    mProgressHandler.start();
            } else {
                if (mCallbacks != null) {
                    if (state.getState() == PlaybackStateCompat.STATE_STOPPED)
                        mCallbacks.onStopped(mPlayerControls);
                    else
                        mCallbacks.onPaused(mPlayerControls);
                }
                if (mProgressHandler != null)
                    mProgressHandler.stop();
            }
        } else if (mProgressHandler != null) {
            mProgressHandler.stop();
        }
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING && mCallbacks != null) {
            if (mPlayerControls != null
                    && (!mPlayerControls.isMediaControlsSet() || !(mCurrentId != null && mCurrentId.equals(mPlayerControls.getMediaId())))) {
                mPlayerControls.setMediaControls(mCallbacks, mMediaControls);
            }
        }
    }

    void onSessionEvent(String event, Bundle extras) {
        if (mCallbacks != null) {
            if (event.startsWith(MediaService.EVENT_TIMER)) {
                mCallbacks.onTimerChanged(this, MediaService.getTimerFromEvent(event));
            } else if (event.startsWith(MediaService.EVENT_SPEED)) {
                float speed = MediaService.getSpeedFromEvent(event);
                mCallbacks.onSpeedChanged(this, speed);
            } else if (event.startsWith(MediaService.EVENT_CAST)) {
                mCallbacks.onCasting(this, MediaService.getCastFromEvent(event));
            }
        }
    }

    void onMetadataChanged(@Nullable MediaMetadataCompat metadata) {
        if (mCallbacks != null) {
            if (mPlayerControls != null) {
                mPlayerControls.mMetaData = metadata;
                final String mediaId = getMediaId(metadata);
                if (mediaId != null && (mPlayerControls.mMediaId == null || !mediaId.equals(mPlayerControls.mMediaIdStr))) {
                    LOG.v("Media ID Changed");
                    mPlayerControls.mMediaId = MediaProvider.getInstance().getMediaId(mediaId);
                    mPlayerControls.mMediaIdStr = mPlayerControls.mMediaId.toString();
                    mCurrentId = mPlayerControls.mMediaId;
                    onPlaybackStateChanged(mMediaControls.getPlaybackState());
                }
            }
            mCallbacks.onMetadataChanged(this, metadata);
            final long duration = metadata == null ? 0 : metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            if (duration > 0)
                mCallbacks.setTimeTotal(this, duration);
        }
    }

    public void setSpeed(float speed) {
        if (mMediaControls != null) {
            Bundle args = new Bundle();
            args.putFloat(MediaService.EXTRA_SPEED, speed);
            mMediaControls.getTransportControls().sendCustomAction(MediaService.ACTION_SET_SPEED, args);
        }
    }

    public void setTimer(long timer) {
        if (mMediaControls != null) {
            Bundle args = new Bundle();
            args.putLong(MediaService.EXTRA_TIMER, timer);
            mMediaControls.getTransportControls().sendCustomAction(MediaService.ACTION_SET_TIMER, args);
        }
    }

    public void setAutoContinue(boolean autoContinue) {
        if (mMediaControls != null) {
            Bundle args = new Bundle();
            args.putBoolean(MediaService.EXTRA_AUTO_CONTINUE, autoContinue);
            mMediaControls.getTransportControls().sendCustomAction(MediaService.ACTION_SET_AUTO_CONTINUE, args);
        }
    }

    public interface MediaInterfaceCallback {

        void onConnected(MediaInterface mediaInterface);

        boolean onPlay(String currentMediaId, String id, MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onPause(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onSkipNext(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onSkipPrevious(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onFastForward(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onRewind(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);

        long getNextPosition(long nextPosition);

        void onLoading(MediaPlayerController controller);

        void onLoaded(MediaPlayerController controller);

        void onPlaying(MediaPlayerController controller);

        void onPaused(MediaPlayerController controller);

        void onStopped(MediaPlayerController controller);

        void onTimerChanged(MediaInterface mediaInterface, long timer);

        void onSpeedChanged(MediaInterface mediaInterface, float speed);

        void onStateChanged(MediaInterface mediaInterface, PlaybackStateCompat state);

        void onCasting(MediaInterface mediaInterface, String deviceName);

        void setTimePlayed(MediaInterface mediaInterface, long played);

        void setTimeTotal(MediaInterface mediaInterface, long total);

        void setVisible(MediaInterface mediaInterface, boolean visible);

        boolean isPositionChanging(MediaInterface mediaInterface);

        void setPosition(MediaInterface mediaInterface, long max, long position, long secondaryPosition);

        void onMetadataChanged(MediaInterface mediaInterface, MediaMetadataCompat mediaMetadataCompat);

        void onDestroy(MediaInterface mediaInterface);

    }

    public static class ProgressHandler extends Handler {

        private static final int SHOW_PROGRESS = 1;
        private static final int AUTO_HIDE = 2;
        public static final int MAX_PROGRESS = 1000;

        private final WeakReference<MediaInterface> mMediaInterface;

        public ProgressHandler(MediaInterface mediaInterface) {
            mMediaInterface = new WeakReference<>(mediaInterface);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS: {
                    final MediaInterface mediaInterface = mMediaInterface.get();
                    if (mediaInterface != null
                            && mediaInterface.mCallbacks != null
                            && !mediaInterface.mCallbacks.isPositionChanging(mediaInterface)) {
                        final MediaPlayerController controller = mediaInterface.getPlayerController();
                        final long position = controller.getCurrentPosition();
                        final long duration = controller.getDuration();
                        long currentPos = 0;
                        if (duration > 0) {
                            currentPos = PlaybackManager.ONE_SECOND * position / duration;
                        }
                        final int percent = mediaInterface.getPlayerController().getBufferPercentage();
                        mediaInterface.mCallbacks.setPosition(mediaInterface, MAX_PROGRESS, currentPos, percent * 10);
                        mediaInterface.mCallbacks.setTimePlayed(mediaInterface, position);
                        sendEmptyMessageDelayed(SHOW_PROGRESS, PlaybackManager.ONE_SECOND - (position % PlaybackManager.ONE_SECOND));
                    }
                    break;
                }
                case AUTO_HIDE: {
                    final MediaInterface mediaInterface = mMediaInterface.get();
                    if (mediaInterface != null && mediaInterface.mCallbacks != null) {
                        if (mediaInterface.mCallbacks.isPositionChanging(mediaInterface))
                            sendEmptyMessageDelayed(AUTO_HIDE, 3000);
                        else
                            mediaInterface.mCallbacks.setVisible(mediaInterface, false);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        public void start() {
            if (!hasMessages(SHOW_PROGRESS))
                sendEmptyMessage(SHOW_PROGRESS);
        }

        public void stop() {
            removeMessages(SHOW_PROGRESS);
        }
    }

    static boolean isPlaying(PlaybackStateCompat playbackStateCompat) {
        int state = -1;
        if (playbackStateCompat != null)
            state = playbackStateCompat.getState();
        return state == PlaybackStateCompat.STATE_BUFFERING || state == PlaybackStateCompat.STATE_PLAYING;
    }

    @Nullable
    static String getMediaId(MediaControllerCompat mediaControllerCompat) {
        if (mediaControllerCompat != null) {
            MediaMetadataCompat metadataCompat = mediaControllerCompat.getMetadata();
            if (metadataCompat != null) {
                return getMediaId(metadataCompat);
            }
        }
        return null;
    }

    @Nullable
    static String getMediaId(MediaMetadataCompat metadataCompat) {
        MediaDescriptionCompat descriptionCompat = metadataCompat == null ? null : metadataCompat.getDescription();
        if (descriptionCompat != null)
            return descriptionCompat.getMediaId();
        return null;
    }

}
