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
package nuclei.media.playback;

import nuclei.logs.Log;
import nuclei.logs.Logs;
import nuclei.media.MediaMetadata;

public abstract class BasePlayback implements Playback {

    private static final Log LOG = Logs.newLog(BasePlayback.class);

    private Timing mTiming;

    @Override
    public final void play(MediaMetadata metadata) {
        mTiming = metadata.getTiming();
        internalPlay(metadata, mTiming, !metadata.isTimingSeeked());
        metadata.setTimingSeeked(true);
    }

    protected abstract void internalPlay(MediaMetadata metadata, Timing timing, boolean seek);

    @Override
    public final void prepare(MediaMetadata metadata) {
        pause();
        mTiming = metadata.getTiming();
        internalPrepare(metadata, mTiming);
    }

    protected abstract void internalPrepare(MediaMetadata metadata, Timing timing);

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
