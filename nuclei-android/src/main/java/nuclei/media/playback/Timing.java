package nuclei.media.playback;

import java.io.Serializable;

public class Timing implements Serializable {

    public final long start;
    public final long end;

    public Timing(long start, long end) {
        this.start = start;
        this.end = end;
    }

}
