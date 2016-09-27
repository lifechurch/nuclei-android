package nuclei.media;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Surface;
import android.widget.SeekBar;
import android.widget.TextView;

import nuclei.logs.Log;
import nuclei.logs.Logs;

public class MediaInterface {

    private static final Log LOG = Logs.newLog(MediaInterface.class);

    private FragmentActivity mActivity;
    private MediaInterfaceCallback mCallbacks;
    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaControls;
    private MediaControllerCompat.Callback mMediaCallback;
    private MediaPlayerController mPlayerControls;

    public MediaInterface(FragmentActivity activity, MediaId mediaId, MediaInterfaceCallback callback) {
        mActivity = activity;
        mCallbacks = callback;
        mPlayerControls = new MediaPlayerController(mediaId);
        mPlayerControls.setMediaControls(mCallbacks, null);
        mPlayerControls.setViews(
                mCallbacks.getTimePlayed(this),
                mCallbacks.getTimeRemaining(this),
                mCallbacks.getProgress(this));
        mMediaBrowser = new MediaBrowserCompat(activity.getApplicationContext(),
                new ComponentName(activity, MediaService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        MediaInterface.this.onConnected();
                    }
                }, null);
        mMediaBrowser.connect();
    }

    public MediaPlayerController getPlayerController() {
        return mPlayerControls;
    }

    public MediaControllerCompat getMediaController() {
        return mMediaControls;
    }

    public void setSurface(Surface surface) {
        Bundle args = new Bundle();
        args.putParcelable(MediaService.EXTRA_SURFACE, surface);
        getMediaController()
                .getTransportControls().sendCustomAction(MediaService.ACTION_SET_TIMER, args);
    }

    public void clearViews() {
        if (mPlayerControls != null)
            mPlayerControls.clearViews();
    }

    public void onDestroy() {
        clearViews();
        if (mPlayerControls != null) {
            mPlayerControls.setMediaControls(null, null);
            mPlayerControls.clearViews();
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
        mActivity = null;
    }

    private void onConnected() {
        try {
            if (mActivity == null)
                return;
            mMediaControls = new MediaControllerCompat(mActivity, mMediaBrowser.getSessionToken());
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
            mMediaControls.registerCallback(mMediaCallback);
            mActivity.setSupportMediaController(mMediaControls);

            if (mPlayerControls != null)
                mPlayerControls.setMediaControls(mCallbacks, mMediaControls);

            if (mMediaControls.getPlaybackState() != null)
                onPlaybackStateChanged(mMediaControls.getPlaybackState());

            Intent intent = mActivity.getIntent();
            if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction())) {
                Bundle params = intent.getExtras();
                String query = params.getString(SearchManager.QUERY);
                LOG.i("Starting from voice search query=" + query);
                mMediaControls.getTransportControls()
                        .playFromSearch(query, params);
            }
            if (mCallbacks != null)
                mCallbacks.onConnected(this);
        } catch (RemoteException err) {
            LOG.e("Error in onConnected", err);
        }
    }

    private void onPlaybackStateChanged(PlaybackStateCompat state) {
        if (state.getState() != PlaybackStateCompat.STATE_BUFFERING) {
            mCallbacks.onLoaded(mPlayerControls);
        } else if (state.getState() == PlaybackStateCompat.STATE_BUFFERING) {
            mCallbacks.onLoading(mPlayerControls);
        }
        mCallbacks.onStateChanged(this, state);
        if (mPlayerControls != null) {
            if (MediaPlayerController.isPlaying(mMediaControls, state, mPlayerControls.getMediaId()))
                mCallbacks.onPlaying(mPlayerControls);
            else
                mCallbacks.onPaused(mPlayerControls);
        }
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            if (mPlayerControls != null && !MediaPlayerController.isEquals(mMediaControls, mPlayerControls.getMediaId())) {
                String mediaId = MediaPlayerController.getMediaId(mMediaControls);
                if (mediaId != null) {
                    MediaId id = MediaProvider.getInstance().getMediaId(mediaId);
                    if (!id.equals(mPlayerControls.getMediaId())) {
                        mPlayerControls.clearViews();
                        mPlayerControls = new MediaPlayerController(id);
                    }
                    mPlayerControls.setMediaControls(mCallbacks, mMediaControls);
                    mPlayerControls.setViews(
                            mCallbacks.getTimePlayed(this),
                            mCallbacks.getTimeRemaining(this),
                            mCallbacks.getProgress(this));
                }
            }
        }
        if (mPlayerControls != null)
            mPlayerControls.update();
    }

    private void onSessionEvent(String event, Bundle extras) {
        if (mCallbacks != null) {
            if (event.startsWith(MediaService.EVENT_TIMER)) {
                mCallbacks.onTimerChanged(this, MediaService.getTimerFromEvent(event));
            } else if (event.startsWith(MediaService.EVENT_SPEED)) {
                mCallbacks.onSpeedChanged(this, MediaService.getSpeedFromEvent(event));
            }
        }
    }

    private void onMetadataChanged(MediaMetadataCompat metadata) {
        if (mPlayerControls != null)
            mPlayerControls.update();
    }

    public interface MediaInterfaceCallback {

        void onConnected(MediaInterface mediaInterface);

        void onLoading(MediaPlayerController controller);

        void onLoaded(MediaPlayerController controller);

        void onPlaying(MediaPlayerController controller);

        void onPaused(MediaPlayerController controller);

        void onTimerChanged(MediaInterface mediaInterface, long timer);

        void onSpeedChanged(MediaInterface mediaInterface, float speed);

        void onStateChanged(MediaInterface mediaInterface, PlaybackStateCompat state);

        TextView getTimePlayed(MediaInterface mediaInterface);

        TextView getTimeRemaining(MediaInterface mediaInterface);

        SeekBar getProgress(MediaInterface mediaInterface);

    }

}
