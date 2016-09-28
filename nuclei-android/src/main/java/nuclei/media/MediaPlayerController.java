package nuclei.media;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.MediaController;

public class MediaPlayerController implements MediaController.MediaPlayerControl {

    private MediaId mMediaId;
    private MediaInterface.MediaInterfaceCallback mCallbacks;
    private MediaControllerCompat mMediaControls;
    private boolean mStartWhenReady;


    public MediaPlayerController(MediaId mediaId) {
        mMediaId = mediaId;
    }

    public void setMediaId(MediaId mediaId, boolean play) {
        mMediaId = mediaId;
        if (play)
            start();
        else
            mMediaControls.getTransportControls().prepareFromMediaId(mediaId.toString(), null);
    }

    protected boolean isMediaControlsSet() {
        return mCallbacks != null && mMediaControls != null;
    }

    public void setMediaControls(MediaInterface.MediaInterfaceCallback callback, MediaControllerCompat mediaControls) {
        mCallbacks = callback;
        mMediaControls = mediaControls;
        String mediaId = MediaPlayerController.getMediaId(mMediaControls);
        if (mediaId != null)
            mMediaId = MediaProvider.getInstance().getMediaId(mediaId);
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
            } else {
                mMediaControls.getTransportControls().playFromMediaId(id, null);
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

}