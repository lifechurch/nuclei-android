package nuclei.media.playback;

import android.os.Build;

import nuclei.media.MediaService;

public class PlaybackFactory {

    private static PlaybackFactory INSTANCE = new PlaybackFactory();

    public static void setFactoryInstance(PlaybackFactory factoryInstance) {
        INSTANCE = factoryInstance;
    }

    public static Playback createLocalPlayback(MediaService service) {
        return INSTANCE.onCreatePlayback(service);
    }

    public static Playback createCastPlayback(MediaService service) {
        return INSTANCE.onCreateCastPlayback(service);
    }


    protected Playback onCreateCastPlayback(MediaService service) {
        return new CastPlayback();
    }

    protected Playback onCreatePlayback(MediaService service) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                ? new ExoPlayerPlayback(service)
                : new FallbackPlayback(service);
    }

}
