package nuclei.media;

import android.app.Activity;
import android.content.Context;

public final class Configuration {

    static Class<? extends Activity> AUDIO_ACTIVITY;
    static Class<? extends Activity> VIDEO_ACTIVITY;

    private Configuration() {
    }

    public static void initialize(Context context,
                                  Class<? extends Activity> audioActivity,
                                  Class<? extends Activity> videoActivity,
                                  MediaProvider provider,
                                  ResourceProvider resourceProvider) {
        AUDIO_ACTIVITY = audioActivity;
        VIDEO_ACTIVITY = videoActivity;
        MediaProvider.initialize(context, provider);
        ResourceProvider.initialize(context, resourceProvider);
    }

}
