package nuclei.ui.view.media;

import android.graphics.PorterDuff;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Formatter;
import java.util.Locale;

import io.nuclei.R;
import nuclei.media.MediaInterface;
import nuclei.media.MediaPlayerController;
import nuclei.media.ResourceProvider;
import nuclei.media.playback.PlaybackManager;
import nuclei.ui.util.ViewUtil;

public class DefaultCallback implements MediaInterface.MediaInterfaceCallback {

    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 3600;

    private final StringBuilder mFormatBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

    private final ImageView play;
    private final View loading;
    private final TextView timePlayed;
    private final TextView timeTotal;
    private final TextView speed;
    private final TextView timer;
    private final SeekBar seekBar;
    private final ImageView next;
    private final ImageView previous;

    private PlayerControlsView mView;
    private OnConnectedListener mConnectedListener;

    public DefaultCallback(PlayerControlsView view, OnConnectedListener listener) {
        mView = view;
        mConnectedListener = listener;
        play = (ImageView) view.findViewById(R.id.btn_play);
        loading = view.findViewById(R.id.media_loading);
        timePlayed = (TextView) view.findViewById(R.id.time_played);
        timeTotal = (TextView) view.findViewById(R.id.time_total);
        speed = (TextView) view.findViewById(R.id.btn_speed);
        timer = (TextView) view.findViewById(R.id.btn_timer);
        seekBar = (SeekBar) view.findViewById(R.id.progress);
        next = (ImageView) view.findViewById(R.id.btn_next);
        previous = (ImageView) view.findViewById(R.id.btn_previous);
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(mSeekListener);
            seekBar.setMax(MediaInterface.ProgressHandler.MAX_PROGRESS);
        }
    }

    private MediaInterface.ProgressHandler mHandler;
    private boolean mDragging;
    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            mDragging = true;
            if (mHandler != null)
                mHandler.stop();
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            if (mView == null || mView.mMediaInterface == null)
                return;
            MediaInterface mediaInterface = mView.mMediaInterface;
            MediaPlayerController controller = mediaInterface.getPlayerController();
            long duration = controller.getDuration();
            long newPosition = (duration * progress) / PlaybackManager.ONE_SECOND;
            controller.seekTo((int) newPosition);
            newPosition = PlaybackManager.ONE_SECOND * newPosition / duration;
            setPosition(mediaInterface, MediaInterface.ProgressHandler.MAX_PROGRESS, newPosition, -1);
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            if (mHandler != null)
                mHandler.start();
        }

    };

    @Override
    public void onConnected(nuclei.media.MediaInterface mediaInterface) {
        if (mConnectedListener != null) {
            if (mView == null || mView.mMediaInterface == null)
                return;
            mConnectedListener.onConnected(mView, mediaInterface);
        }
        if (mHandler == null)
            mHandler = new MediaInterface.ProgressHandler(mediaInterface);
        mHandler.start();
    }

    @Override
    public void onLoading(MediaPlayerController controller) {
        if (loading != null)
            loading.setVisibility(View.VISIBLE);
        if (mHandler != null)
            mHandler.start();
    }

    @Override
    public void onLoaded(MediaPlayerController controller) {
        if (loading != null)
            loading.setVisibility(View.GONE);
    }

    @Override
    public void onPlaying(MediaPlayerController controller) {
        if (play != null)
            play.setActivated(true);
        if (mHandler != null)
            mHandler.start();
    }

    @Override
    public void onPaused(MediaPlayerController controller) {
        onStopped(controller);
    }

    @Override
    public void onStopped(MediaPlayerController controller) {
        if (play != null)
            play.setActivated(false);
        if (mHandler != null)
            mHandler.stop();
    }

    @Override
    public void onTimerChanged(nuclei.media.MediaInterface mediaInterface, long t) {
        if (mView == null)
            return;
        mView.mTimer = t;
        if (timer != null) {
            if (t < 1)
                timer.setText(ResourceProvider.getInstance().getString(ResourceProvider.TIMER));
            else
                timer.setText(stringForTime(t));
        }
    }

    @Override
    public void onSpeedChanged(nuclei.media.MediaInterface mediaInterface, float s) {
        if (speed != null)
            speed.setText(ResourceProvider.getInstance().getSelectedSpeed());
    }

    @Override
    public void onStateChanged(nuclei.media.MediaInterface mediaInterface, PlaybackStateCompat state) {
        if (mView == null)
            return;
        onHandleState(mView, next, previous, state);
    }

    @Override
    public void setTimePlayed(nuclei.media.MediaInterface mediaInterface, long played) {
        if (played < 0)
            played = 0;
        if (timePlayed != null)
            timePlayed.setText(stringForTime(played));
    }

    @Override
    public void setTimeTotal(nuclei.media.MediaInterface mediaInterface, long remaining) {
        if (timeTotal != null)
            timeTotal.setText(stringForTime(remaining));
    }

    @Override
    public void setVisible(MediaInterface mediaInterface, boolean visible) {
        if (mView != null) {
            if (visible)
                mView.show();
            else
                mView.hide();
        }
    }

    @Override
    public boolean isPositionChanging(MediaInterface mediaInterface) {
        return mDragging;
    }

    @Override
    public void setPosition(nuclei.media.MediaInterface mediaInterface, long max, long position, long secondaryPosition) {
        if (seekBar != null) {
            seekBar.setMax((int) max);
            seekBar.setProgress((int) position);
            if (secondaryPosition != -1)
                seekBar.setSecondaryProgress((int) secondaryPosition);
        }
    }

    @Override
    public void onMetadataChanged(MediaInterface mediaInterface, MediaMetadataCompat mediaMetadataCompat) {

    }

    @Override
    public void onDestroy(MediaInterface mediaInterface) {
        mView = null;
        if (mHandler != null)
            mHandler.stop();
        mHandler = null;
        mConnectedListener = null;
    }

    private String stringForTime(long timeMs) {
        long totalSeconds = timeMs / PlaybackManager.ONE_SECOND;

        int seconds = (int) (totalSeconds % ONE_MINUTE);
        int minutes = (int) ((totalSeconds / ONE_MINUTE) % ONE_MINUTE);
        int hours = (int) (totalSeconds / ONE_HOUR);

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public static void onHandleState(View view, ImageView next, ImageView previous, PlaybackStateCompat state) {
        int enabled = ViewUtil.getThemeAttrColor(view.getContext(), android.R.attr.textColorPrimary);
        int disabled = ViewUtil.getThemeAttrColor(view.getContext(), android.R.attr.textColorSecondary);
        if (next != null)
            next.setColorFilter(state != null && (state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                    == PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    ? enabled
                    : disabled, PorterDuff.Mode.SRC_ATOP);
        if (previous != null)
            previous.setColorFilter(state != null && (state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    == PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    ? enabled
                    : disabled, PorterDuff.Mode.SRC_ATOP);
    }


    public interface OnConnectedListener {
        void onConnected(PlayerControlsView view, MediaInterface mediaInterface);
    }

}
