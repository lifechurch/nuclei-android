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
package nuclei3.media;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.app.NotificationCompat;

import io.nuclei3.R;
import nuclei3.ui.util.ViewUtil;
import nuclei3.logs.Log;
import nuclei3.logs.Logs;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {

    static final Log LOG = Logs.newLog(MediaNotificationManager.class);

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = "nuclei.PAUSE";
    public static final String ACTION_PLAY = "nuclei.PLAY";
    public static final String ACTION_PREV = "nuclei.PREV";
    public static final String ACTION_NEXT = "nuclei.NEXT";
    public static final String ACTION_STOP_CASTING = "nuclei.STOP_CAST";
    public static final String ACTION_CANCEL = "nuclei.CANCEL";

    private final MediaService mService;
    private MediaSessionCompat.Token mSessionToken;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mTransportControls;
    private static final int UPDATE_DELAY = 1000;
    private long mLastNotificationUpdate;
    private Handler mUpdateHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            updateNotificationNow();
            return true;
        }
    });

    PlaybackStateCompat mPlaybackState;
    MediaMetadataCompat mMetadata;

    final NotificationManagerCompat mNotificationManager;

    private final PendingIntent mPauseIntent;
    private final PendingIntent mPlayIntent;
    private final PendingIntent mPreviousIntent;
    private final PendingIntent mNextIntent;
    private final PendingIntent mCancelIntent;

    private final PendingIntent mStopCastIntent;

    private final int mNotificationColor;

    private boolean mStarted = false;
    private Notification mRunningNotification;

    public MediaNotificationManager(MediaService service) throws RemoteException {
        mService = service;
        updateSessionToken();

        mNotificationColor = ViewUtil.getThemeAttrColor(mService, R.attr.colorPrimary);

        mNotificationManager = NotificationManagerCompat.from(service);

        String pkg = mService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mStopCastIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_STOP_CASTING).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT);
        mCancelIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_CANCEL).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {
            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();

            // The notification must be updated after setting started to true
            mRunningNotification = getNewNotification();
            if (mRunningNotification != null) {
                mController.registerCallback(mCb);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                filter.addAction(ACTION_STOP_CASTING);
                filter.addAction(ACTION_CANCEL);
                mService.registerReceiver(this, filter);

                mService.startForeground(NOTIFICATION_ID, mRunningNotification);
                mStarted = true;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // when starting a ForegroundService we need to call startForeground within 5 seconds or
            // there is a resulting ANR. see https://stackoverflow.com/questions/44425584/context-startforegroundservice-did-not-then-call-service-startforeground)
            mService.startForeground(NOTIFICATION_ID, mRunningNotification);
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (mStarted) {
            mStarted = false;
            mRunningNotification = null;
            mController.unregisterCallback(mCb);
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        LOG.d("Received intent with action " + action);
        switch (action) {
            case ACTION_PAUSE:
                mTransportControls.pause();
                break;
            case ACTION_PLAY:
                mTransportControls.play();
                break;
            case ACTION_NEXT:
                mTransportControls.skipToNext();
                break;
            case ACTION_PREV:
                mTransportControls.skipToPrevious();
                break;
            case ACTION_STOP_CASTING:
                Intent i = new Intent(context, MediaService.class);
                i.setAction(MediaService.ACTION_CMD);
                i.putExtra(MediaService.CMD_NAME, MediaService.CMD_STOP_CASTING);
                mService.startService(i);
                break;
            case ACTION_CANCEL:
                mTransportControls.stop();
                break;
            default:
                if (LOG.isLoggable(Log.WARN))
                    LOG.w("Unknown intent ignored. Action=" + action);
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    void updateSessionToken() throws RemoteException {
        MediaSessionCompat.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null && freshToken != null
                || mSessionToken != null && !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            if (mSessionToken != null) {
                mController = new MediaControllerCompat(mService, mSessionToken);
                mTransportControls = mController.getTransportControls();
                if (mStarted) {
                    mController.registerCallback(mCb);
                }
            }
        }
    }

    private PendingIntent createContentIntent(MediaDescriptionCompat description) {
        try {
            MediaId id = MediaProvider.getInstance().getMediaId(description.getMediaId());
            Intent openUI = MediaProvider.getInstance().getContentIntent(id);
            return PendingIntent.getActivity(mService, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    private final MediaControllerCompat.Callback mCb = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            mPlaybackState = state;
            LOG.d("Received new playback state ", state);
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED
                    || state.getState() == PlaybackStateCompat.STATE_NONE) {
                stopNotification();
            } else {
                suggestNotificationUpdate();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mMetadata = metadata;
            LOG.d("Received new metadata ", metadata);
            suggestNotificationUpdate();
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            LOG.d("Session was destroyed, resetting to the new session token");
            try {
                updateSessionToken();
            } catch (RemoteException e) {
                LOG.e("could not connect media controller", e);
            }
        }
    };

    private Notification getNewNotification() {
        Notification newNotification = null;
        if (System.currentTimeMillis() - mLastNotificationUpdate > UPDATE_DELAY) {
            newNotification = createNotification();
        }
        return newNotification;
    }

    private void suggestNotificationUpdate() {
        if (System.currentTimeMillis() - mLastNotificationUpdate > UPDATE_DELAY) {
            updateNotificationNow();
        } else {
            mUpdateHandler.removeCallbacksAndMessages(null);
            mUpdateHandler.sendEmptyMessageDelayed(0, UPDATE_DELAY);
        }
        mLastNotificationUpdate = System.currentTimeMillis();
    }

    private void updateNotificationNow() {
        Notification notification = createNotification();
        if (notification != null)
            mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    Notification createNotification() {
        LOG.d("updateNotificationMetadata. mMetadata=", mMetadata);
        if (mMetadata == null || mPlaybackState == null) {
            return null;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mService, ResourceProvider.getInstance().getNotificationChannelId());
        int playPauseButtonPosition = 0;

        // If skip to previous action is enabled
        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            notificationBuilder.addAction(ResourceProvider.getInstance().getDrawable(ResourceProvider.PREVIOUS),
                    ResourceProvider.getInstance().getString(ResourceProvider.PREVIOUS), mPreviousIntent);

            // If there is a "skip to previous" button, the play/pause button will
            // be the second one. We need to keep track of it, because the MediaStyle notification
            // requires to specify the index of the buttons (actions) that should be visible
            // when in compact view.
            playPauseButtonPosition = 1;
        }

        addPlayPauseAction(notificationBuilder);

        // If skip to next action is enabled
        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            notificationBuilder.addAction(ResourceProvider.getInstance().getDrawable(ResourceProvider.NEXT),
                    ResourceProvider.getInstance().getString(ResourceProvider.NEXT), mNextIntent);
        }

        MediaDescriptionCompat description = mMetadata.getDescription();

        Bitmap bitmap = description.getIconBitmap();
        if (bitmap != null && bitmap.isRecycled())
            bitmap = null;

        if (bitmap == null) {
            bitmap = mMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notificationBuilder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(playPauseButtonPosition)  // show only play/pause in compact view
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(mCancelIntent)
                    .setMediaSession(mSessionToken))
                    .setContentIntent(createContentIntent(description));
        }

        notificationBuilder
                .setColor(mNotificationColor)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSmallIcon(ResourceProvider.getInstance().getDrawable(ResourceProvider.ICON_SMALL))
                .setLargeIcon(bitmap);

        if (mController != null && mController.getExtras() != null) {
            String castName = mController.getExtras().getString(MediaService.EXTRA_CONNECTED_CAST);
            if (castName != null) {
                CharSequence castInfo = ResourceProvider.getInstance().getString(ResourceProvider.CASTING_TO_DEVICE, castName);
                notificationBuilder.setSubText(castInfo);
                notificationBuilder.addAction(ResourceProvider.getInstance().getDrawable(ResourceProvider.ICON_CLOSE),
                        ResourceProvider.getInstance().getString(ResourceProvider.STOP_CASTING), mStopCastIntent);
            }
        }

        setNotificationPlaybackState(notificationBuilder);

        return notificationBuilder.build();
    }

    private void addPlayPauseAction(NotificationCompat.Builder builder) {
        LOG.d("updatePlayPauseAction");
        CharSequence label;
        int icon;
        PendingIntent intent;
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            label = ResourceProvider.getInstance().getString(ResourceProvider.PAUSE);
            icon = ResourceProvider.getInstance().getDrawable(ResourceProvider.PAUSE);
            intent = mPauseIntent;
        } else {
            label = ResourceProvider.getInstance().getString(ResourceProvider.PLAY);
            icon = ResourceProvider.getInstance().getDrawable(ResourceProvider.PLAY);
            intent = mPlayIntent;
        }
        builder.addAction(new NotificationCompat.Action(icon, label, intent));
    }

    private static final int MILLI = 1000;

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        LOG.d("updateNotificationPlaybackState. mPlaybackState=", mPlaybackState);
        if (mPlaybackState == null || !mStarted) {
            return;
        }
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING
                && mPlaybackState.getPosition() >= 0) {
            LOG.d("updateNotificationPlaybackState. updating playback position to ",
                    (System.currentTimeMillis() - mPlaybackState.getPosition()) / MILLI, " seconds");
            builder
                    .setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            LOG.d("updateNotificationPlaybackState. hiding playback position");
            builder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }

}
