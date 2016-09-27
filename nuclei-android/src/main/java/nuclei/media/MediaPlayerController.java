package nuclei.media;

import android.os.Handler;
import android.os.Message;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

import nuclei.media.playback.PlaybackManager;

public class MediaPlayerController implements MediaController.MediaPlayerControl {

    private static final int SHOW_PROGRESS = 1;

    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 3600;

    private static final int MAX_PROGRESS = 1000;
    private static final int TEN = 10;

    private final ControlHandler mHandler = new ControlHandler(this);

    private MediaId mMediaId;
    private MediaInterface.MediaInterfaceCallback mCallbacks;
    private MediaControllerCompat mMediaControls;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private boolean mDragging;
    private boolean mStartWhenReady;

    private TextView mCurrent;
    private TextView mRemaining;
    private SeekBar mPosition;

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            mDragging = true;
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            long duration = getDuration();
            long newPosition = (duration * progress) / PlaybackManager.ONE_SECOND;
            seekTo((int) newPosition);
            if (mCurrent != null)
                mCurrent.setText(stringForTime((int) newPosition));
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }

    };

    public MediaPlayerController(MediaId mediaId) {
        mMediaId = mediaId;
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    public void setMediaId(MediaId mediaId, boolean play) {
        mMediaId = mediaId;
        if (play)
            start();
        else
            mMediaControls.getTransportControls().prepareFromMediaId(mediaId.toString(), null);
    }

    public void setMediaControls(MediaInterface.MediaInterfaceCallback callback, MediaControllerCompat mediaControls) {
        mCallbacks = callback;
        mMediaControls = mediaControls;
        if (mMediaId == null && mediaControls != null) {
            MediaMetadataCompat mediaMetadataCompat = mediaControls.getMetadata();
            if (mediaMetadataCompat != null) {
                MediaDescriptionCompat descriptionCompat = mediaMetadataCompat.getDescription();
                if (descriptionCompat != null && descriptionCompat.getMediaId() != null)
                    mMediaId = MediaProvider.getInstance().getMediaId(descriptionCompat.getMediaId());
            }
        }
        update();
        if (mStartWhenReady) {
            mStartWhenReady = false;
            start();
        }
    }

    private boolean isEquals() {
        return isEquals(mMediaControls, mMediaId);
    }

    @Override
    public void start() {
        if (mMediaId == null)
            return;
        if (mMediaControls != null) {
            if (mCallbacks != null) {
                mCallbacks.onLoading(this);
                mCallbacks.onPlaying(this);
            }
            String currentMediaId = null;
            MediaMetadataCompat metadataCompat = mMediaControls.getMetadata();
            if (metadataCompat != null) {
                MediaDescriptionCompat descriptionCompat = metadataCompat.getDescription();
                if (descriptionCompat != null) {
                    currentMediaId = descriptionCompat.getMediaId();
                }
            }
            final String id = mMediaId.toString();
            if (currentMediaId != null && currentMediaId.equals(id)
                    && mMediaControls.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED) {
                mMediaControls.getTransportControls().play();
                if (mHandler != null)
                    mHandler.sendEmptyMessage(SHOW_PROGRESS);
            } else {
                mMediaControls.getTransportControls().playFromMediaId(id, null);
                if (mHandler != null)
                    mHandler.sendEmptyMessage(SHOW_PROGRESS);
            }
        } else {
            mStartWhenReady = true;
        }
    }

    @Override
    public void pause() {
        if (mCallbacks != null)
            mCallbacks.onPaused(this);
        if (mMediaControls != null)
            mMediaControls.getTransportControls().pause();
    }

    @Override
    public int getDuration() {
        if (isEquals()) {
            MediaMetadataCompat mediaMetadataCompat = mMediaControls.getMetadata();
            if (mediaMetadataCompat != null) {
                long duration = mediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                if (duration > 0)
                    return (int) duration;
            }
        }
        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isEquals()) {
            return (int) mMediaControls.getPlaybackState().getPosition();
        }
        return -1;
    }

    @Override
    public void seekTo(int pos) {
        if (isEquals())
            mMediaControls.getTransportControls().seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return isPlaying(mMediaControls, mMediaId);
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaControls != null) {
            int dur = getDuration();
            if (dur < 1)
                return 0;
            return (int) mMediaControls.getPlaybackState().getBufferedPosition() / getDuration();
        }
        return -1;
    }

    @Override
    public boolean canPause() {
        return isEquals();
    }

    @Override
    public boolean canSeekBackward() {
        return isEquals();
    }

    @Override
    public boolean canSeekForward() {
        return isEquals();
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public MediaId getMediaId() {
        return mMediaId;
    }

    public void setViews(TextView current, TextView remaining, SeekBar position) {
        mCurrent = current;
        mRemaining = remaining;
        mPosition = position;
        if (mPosition != null) {
            mPosition.setOnSeekBarChangeListener(mSeekListener);
            mPosition.setMax(MAX_PROGRESS);
        }
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    public void update() {
        if (!mHandler.hasMessages(SHOW_PROGRESS))
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    public void clearViews() {
        mCurrent = null;
        mRemaining = null;
        if (mPosition != null)
            mPosition.setOnSeekBarChangeListener(null);
        mPosition = null;
        mHandler.removeMessages(SHOW_PROGRESS);
    }

    private int setProgress() {
        if (mDragging) {
            return 0;
        }
        long position = getCurrentPosition();
        long duration = getDuration();
        if (mPosition != null) {
            if (duration > 0) {
                long pos = PlaybackManager.ONE_SECOND * position / duration;
                mPosition.setProgress((int) pos);
            }
            int percent = getBufferPercentage();
            mPosition.setSecondaryProgress(percent * TEN);
        }

        if (mRemaining != null)
            mRemaining.setText(stringForTime(duration));
        if (mCurrent != null)
            mCurrent.setText(stringForTime(position));

        return (int) position;
    }

    public String stringForTime(long timeMs) {
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

    public static boolean isEquals(MediaControllerCompat mediaControllerCompat, MediaId mediaId) {
        if (mediaControllerCompat != null && mediaId != null) {
            MediaMetadataCompat metadataCompat = mediaControllerCompat.getMetadata();
            if (metadataCompat != null) {
                MediaDescriptionCompat descriptionCompat = metadataCompat.getDescription();
                if (descriptionCompat != null) {
                    return mediaId.toString().equals(descriptionCompat.getMediaId());
                }
            }
        }
        return false;
    }

    public static String getMediaId(MediaControllerCompat mediaControllerCompat) {
        if (mediaControllerCompat != null) {
            MediaMetadataCompat metadataCompat = mediaControllerCompat.getMetadata();
            if (metadataCompat != null) {
                MediaDescriptionCompat descriptionCompat = metadataCompat.getDescription();
                if (descriptionCompat != null) {
                    return descriptionCompat.getMediaId();
                }
            }
        }
        return null;
    }

    public static boolean isPlaying(MediaControllerCompat mediaControllerCompat, PlaybackStateCompat playbackStateCompat, MediaId mediaId) {
        if (isEquals(mediaControllerCompat, mediaId)) {
            if (playbackStateCompat == null)
                playbackStateCompat = mediaControllerCompat.getPlaybackState();
            int state = -1;
            if (playbackStateCompat != null)
                state = playbackStateCompat.getState();
            return isEquals(mediaControllerCompat, mediaId)
                    && (state == PlaybackStateCompat.STATE_BUFFERING || state == PlaybackStateCompat.STATE_PLAYING);
        }
        return false;
    }

    public static boolean isPlaying(MediaControllerCompat mediaControllerCompat, MediaId mediaId) {
        return isPlaying(mediaControllerCompat, null, mediaId);
    }

    private static class ControlHandler extends Handler {

        private WeakReference<MediaPlayerController> mControl;

        public ControlHandler(MediaPlayerController control) {
            mControl = new WeakReference<>(control);
        }

        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case SHOW_PROGRESS:
                    MediaPlayerController control = mControl.get();
                    if (control != null) {
                        pos = control.setProgress();
                        if (!control.mDragging && control.isPlaying()) {
                            msg = obtainMessage(SHOW_PROGRESS);
                            sendMessageDelayed(msg, PlaybackManager.ONE_SECOND - (pos % PlaybackManager.ONE_SECOND));
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}