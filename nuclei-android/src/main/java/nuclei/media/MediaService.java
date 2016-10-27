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

package nuclei.media;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;

import java.lang.ref.WeakReference;
import java.util.List;

import nuclei.media.playback.CastPlayback;
import nuclei.media.playback.ExoPlayerPlayback;
import nuclei.media.playback.FallbackPlayback;
import nuclei.media.playback.Playback;
import nuclei.media.playback.PlaybackManager;
import nuclei.media.utils.CarHelper;
import nuclei.logs.Log;
import nuclei.logs.Logs;

public class MediaService extends MediaBrowserServiceCompat implements
        PlaybackManager.PlaybackServiceCallback {

    private static final Log LOG = Logs.newLog(MediaService.class);

    public static final String EVENT_TIMER = "nuclei.TIMER_CHANGE.";
    public static final String EVENT_SPEED = "nuclei.SPEED_CHANGE.";
    public static final String EVENT_CAST = "nuclei.CAST.";

    public static final String ERROR_INITIALIZATION = "Error Initialization";
    public static final String ERROR_LOAD = "Error Loading";
    public static final String ERROR_NETWORK = "Error Network";

    // Extra on MediaSession that contains the Cast device name currently connected to
    public static final String EXTRA_CONNECTED_CAST = "nuclei.CAST_NAME";

    public static final String EXTRA_SURFACE_ID = "nuclei.SURFACE_ID";
    public static final String EXTRA_SURFACE = "nuclei.SURFACE";
    public static final String EXTRA_SPEED = "nuclei.SPEED";
    public static final String EXTRA_TIMER = "nuclei.TIMER";

    public static final String ACTION_SET_SURFACE = "nuclei.ACTION_SET_SURFACE";
    public static final String ACTION_SET_SPEED = "nuclei.ACTION_SET_SPEED";
    public static final String ACTION_SET_TIMER = "nuclei.ACTION_SET_TIMER";

    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "nuclei.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";
    // A value of a CMD_NAME key that indicates that the music playback should switch
    // to local playback from cast playback.
    public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";
    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;
    private static final int REQUEST_CODE = 99;

    public static final String MEDIA_ID = "media_id";

    private PlaybackManager mPlaybackManager;

    private MediaSessionCompat mSession;
    private MediaNotificationManager mMediaNotificationManager;
    private Bundle mSessionExtras;
    private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    private MediaRouter mMediaRouter;
    private PackageValidator mPackageValidator;

    private boolean mIsConnectedToCar;
    private BroadcastReceiver mCarConnectionReceiver;

    /**
     * Consumer responsible for switching the Playback instances depending on whether
     * it is connected to a remote player.
     */
    private final VideoCastConsumerImpl mCastConsumer = new VideoCastConsumerImpl() {

        @Override
        public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId,
                                           boolean wasLaunched) {
            if (mSession != null && mPlaybackManager != null) {
                // In case we are casting, send the device name as an extra on MediaSession metadata.
                final String deviceName = VideoCastManager.getInstance().getDeviceName();
                mSessionExtras.putString(EXTRA_CONNECTED_CAST, deviceName);
                mSession.setExtras(mSessionExtras);
                // Now we can switch to CastPlayback
                Playback playback = new CastPlayback();
                mMediaRouter.setMediaSessionCompat(mSession);
                mPlaybackManager.switchToPlayback(playback, true);
                mSession.sendSessionEvent(createCastEvent(deviceName), Bundle.EMPTY);
            }
        }

        @Override
        public void onDisconnectionReason(int reason) {
            LOG.d("onDisconnectionReason");
            // This is our final chance to update the underlying stream position
            // In onDisconnected(), the underlying CastPlayback#mVideoCastConsumer
            // is disconnected and hence we update our local value of stream position
            // to the latest position.
            mPlaybackManager.getPlayback().updateLastKnownStreamPosition();
        }

        @Override
        public void onDisconnected() {
            LOG.d("onDisconnected");
            if (mSession != null && mPlaybackManager != null) {
                mSessionExtras.remove(EXTRA_CONNECTED_CAST);
                mSession.setExtras(mSessionExtras);
                Playback playback
                        = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                          ? new ExoPlayerPlayback(MediaService.this)
                          : new FallbackPlayback(MediaService.this);
                mMediaRouter.setMediaSessionCompat(null);
                mPlaybackManager.switchToPlayback(playback, false);
                mSession.sendSessionEvent(createCastEvent(""), Bundle.EMPTY);
            }
        }
    };

    /*
     * (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        LOG.d("onCreate");

        mPackageValidator = new PackageValidator(this);

        boolean casting = false;
        try {
            VideoCastManager.getInstance().addVideoCastConsumer(mCastConsumer);
            casting = VideoCastManager.getInstance().isConnected() || VideoCastManager.getInstance().isConnecting();
        } catch (IllegalStateException err) {
            LOG.e("Error registering cast consumer : " + err.getMessage());
        }

        Playback playback;

        // Start a new MediaSession
        mSession = new MediaSessionCompat(this, "NucleiMediaService");
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        setSessionToken(mSession.getSessionToken());

        try {
            mMediaNotificationManager = new MediaNotificationManager(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }

        mSessionExtras = new Bundle();

        if (casting) {
            mSessionExtras.putString(EXTRA_CONNECTED_CAST, VideoCastManager.getInstance().getDeviceName());
            mMediaRouter.setMediaSessionCompat(mSession);
            playback = new CastPlayback();
        } else
            playback =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                    ? new ExoPlayerPlayback(this)
                    : new FallbackPlayback(this);

        mPlaybackManager = new PlaybackManager(this, playback);

        mSession.setCallback(mPlaybackManager.getMediaSessionCallback());

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(new ComponentName(this, MediaService.class));
        mSession.setMediaButtonReceiver(PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0));

        CarHelper.setSlotReservationFlags(mSessionExtras, true, true, true);
        mSession.setExtras(mSessionExtras);

        mPlaybackManager.updatePlaybackState(null);

        registerCarConnectionReceiver();
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        if (startIntent != null) {
            String action = startIntent.getAction();
            String command = startIntent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(command)) {
                    mPlaybackManager.handlePauseRequest();
                } else if (CMD_STOP_CASTING.equals(command)) {
                    VideoCastManager.getInstance().disconnect();
                }
            } else {
                // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                MediaButtonReceiver.handleIntent(mSession, startIntent);
            }
        }
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LOG.d("onDestroy");
        unregisterCarConnectionReceiver();
        // Service is being killed, so make sure we release our resources
        mPlaybackManager.handleStopRequest(null);
        mMediaNotificationManager.stopNotification();
        try {
            VideoCastManager.getInstance().removeVideoCastConsumer(mCastConsumer);
        } catch (IllegalStateException e) {
            LOG.w("Error removing cast video consumer : " + e.getMessage());
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mSession.release();
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 Bundle rootHints) {
        LOG.d("OnGetRoot: clientPackageName=", clientPackageName, "; clientUid=", clientUid, " ; rootHints=", rootHints);
        return MediaProvider.getInstance().getBrowserRoot(clientPackageName, clientUid, rootHints);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
                               @NonNull final Result<List<MediaItem>> result) {
        MediaProvider.getInstance().onLoadChildren(parentMediaId, result);
    }

    @Override
    public float getAudioSpeed() {
        return MediaProvider.getInstance().getAudioSpeed();
    }

    private Queue mCurrentQueue;

    @Override
    public void onQueue(Queue queue) {
        if (mCurrentQueue != null && mCurrentQueue != queue) {
            MediaProvider.getInstance().evictQueue(mCurrentQueue);
        }
        mCurrentQueue = queue;
        if (queue == null) {
            mSession.setQueueTitle(null);
            mSession.setQueue(null);
        } else {
            mSession.setQueueTitle(queue.getTitle());
            mSession.setQueue(queue.toItems());
        }
    }

    @Override
    public void onSpeedSet(float speed) {
        MediaProvider.getInstance().setAudioSpeed(speed);
        mSession.sendSessionEvent(createSpeedEvent(speed), Bundle.EMPTY);
    }

    @Override
    public void onPlaybackPrepare(final MediaId id) {
    }

    @Override
    public void onPlaybackStart(final MediaId id) {
        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        Playback playback = mPlaybackManager.getPlayback();
        if (playback != null)
            MediaProvider.getInstance().onPlaybackStart(playback, id);

        mDelayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(new Intent(getApplicationContext(), MediaService.class));
    }

    @Override
    public void onTimerCount(long timeRemainingMs) {
        mSession.sendSessionEvent(createTimerEvent(timeRemainingMs), Bundle.EMPTY);
    }

    @Override
    public void onPlaybackPause(MediaId id) {
        Playback playback = mPlaybackManager.getPlayback();
        if (playback != null)
            MediaProvider.getInstance().onPlaybackPause(playback, id);
    }

    /**
     * Callback method called from PlaybackManager whenever the music stops playing.
     */
    @Override
    public void onPlaybackStop(MediaId id) {
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        Playback playback = mPlaybackManager.getPlayback();
        if (playback != null)
            MediaProvider.getInstance().onPlaybackStop(playback, id);
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        stopForeground(true);
    }

    @Override
    public void onPlaybackNext(MediaId id) {
        Playback playback = mPlaybackManager.getPlayback();
        if (playback != null)
            MediaProvider.getInstance().onPlaybackNext(playback, id);
    }

    @Override
    public void onPlaybackSeekTo(MediaId id, long currentPosition, long newPosition) {
        Playback playback = mPlaybackManager.getPlayback();
        if (playback != null)
            MediaProvider.getInstance().onPlaybackSeekTo(playback, id, currentPosition, newPosition);
    }

    @Override
    public void onPlaybackPrevious(MediaId id) {
        Playback playback = mPlaybackManager.getPlayback();
        if (playback != null)
            MediaProvider.getInstance().onPlaybackPrevious(playback, id);
    }

    @Override
    public void onMetadataUpdated(MediaMetadata mediaMetadataCompat) {
        try {
            mediaMetadataCompat.setSession(mSession);
            Context context = getApplicationContext();
            MediaId id = MediaProvider.getInstance().getMediaId(mediaMetadataCompat.getDescription().getMediaId());
            Intent intent = new Intent(context, id.type == MediaId.TYPE_AUDIO
                                                ? Configuration.AUDIO_ACTIVITY
                                                : Configuration.VIDEO_ACTIVITY);
            intent.putExtra(MEDIA_ID, id.toString());
            PendingIntent pi = PendingIntent.getActivity(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mSession.setSessionActivity(pi);
        } catch (Exception err) {
            LOG.e("Error initializing session activity", err);
        }

    }

    @Override
    public void onNotificationRequired() {
        mMediaNotificationManager.startNotification();
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
        mSession.setPlaybackState(newState);
    }

    @Override
    public void onCompletion() {
        Playback playback = mPlaybackManager.getPlayback();
        if (playback != null) {
            MediaId id = playback.getCurrentMediaId();
            MediaProvider.getInstance().onPlaybackCompletion(playback, id);
            MediaMetadata metadata = playback.getCurrentMetadata();
            if (metadata != null)
                metadata.setTimingSeeked(false);
        }
    }

    private void registerCarConnectionReceiver() {
        IntentFilter filter = new IntentFilter(CarHelper.ACTION_MEDIA_STATUS);
        mCarConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String connectionEvent = intent.getStringExtra(CarHelper.MEDIA_CONNECTION_STATUS);
                mIsConnectedToCar = CarHelper.MEDIA_CONNECTED.equals(connectionEvent);
                LOG.i("Connection event to Android Auto: " + connectionEvent + " isConnectedToCar=" + mIsConnectedToCar);
            }
        };
        registerReceiver(mCarConnectionReceiver, filter);
    }

    private void unregisterCarConnectionReceiver() {
        unregisterReceiver(mCarConnectionReceiver);
    }

    /**
     * A simple handler that stops the service if playback is not active (playing).
     */
    private static final class DelayedStopHandler extends Handler {
        private final WeakReference<MediaService> mWeakReference;

        private DelayedStopHandler(MediaService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaService service = mWeakReference.get();
            if (service != null && service.mPlaybackManager.getPlayback() != null) {
                if (service.mPlaybackManager.getPlayback().isPlaying()) {
                    LOG.d("Ignoring delayed stop since the media player is in use.");
                    return;
                }
                LOG.d("Stopping service with delay handler.");
                service.stopSelf();
            }
        }
    }

    // KJB: NOTE: As of writing this not all platforms will receive bundle data, so, we fake it
    public static String createTimerEvent(long timer) {
        return EVENT_TIMER + timer;
    }

    public static long getTimerFromEvent(String eventName) {
        return Long.valueOf(eventName.substring(EVENT_TIMER.length()));
    }

    public static String createSpeedEvent(float speed) {
        return EVENT_SPEED + speed;
    }

    public static float getSpeedFromEvent(String eventName) {
        return Float.valueOf(eventName.substring(EVENT_SPEED.length()));
    }

    public static String createCastEvent(String deviceName) {
        return EVENT_CAST + deviceName;
    }

    public static String getCastFromEvent(String eventName) {
        String name = eventName.substring(EVENT_CAST.length());
        if (name.length() == 0)
            return null;
        return name;
    }

}
