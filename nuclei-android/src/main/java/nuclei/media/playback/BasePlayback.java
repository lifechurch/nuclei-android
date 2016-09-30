package nuclei.media.playback;

import nuclei.logs.Log;
import nuclei.logs.Logs;
import nuclei.media.MediaId;
import nuclei.media.MediaMetadata;

public abstract class BasePlayback implements Playback {

    private static final Log LOG = Logs.newLog(BasePlayback.class);

    private Timing mTiming;

    @Override
    public final void play(MediaMetadata metadata) {
        Timing timing = metadata.getTiming();
        if (timing != null) {
            String id = metadata.getMediaId();
            MediaId currentId = getCurrentMediaId();
            setTiming(timing, currentId == null || !id.equals(currentId.toString()));
        } else {
            setTiming(null, false);
        }
        internalPlay(metadata);
    }

    protected abstract void internalPlay(MediaMetadata metadata);

    @Override
    public long getDuration() {
        if (mTiming != null)
            return mTiming.end - mTiming.start;
        return internalGetDuration();
    }

    protected abstract long internalGetDuration();

    @Override
    public Timing getTiming() {
        return mTiming;
    }

    @Override
    public void setTiming(Timing timing) {
        setTiming(timing, true);
    }

    @Override
    public void setTiming(Timing timing, boolean seek) {
        if (timing != null) {
            mTiming = timing;
            if (seek)
                internalSeekTo(timing.start);
        } else {
            mTiming = null;
        }
    }

    @Override
    public long getStartStreamPosition() {
        return mTiming == null ? 0 : mTiming.start;
    }

    @Override
    public final void seekTo(long position) {
        if (LOG.isLoggable(Log.DEBUG))
            LOG.d("seekTo called with " + position);
        if (mTiming != null)
            position += mTiming.start;
        internalSeekTo(position);
    }

    protected abstract void internalSeekTo(long position);

    @Override
    public final long getCurrentStreamPosition() {
        if (mTiming == null)
            return internalGetCurrentStreamPosition();
        else
            return internalGetCurrentStreamPosition() - mTiming.start;
    }

    protected abstract long internalGetCurrentStreamPosition();
}
