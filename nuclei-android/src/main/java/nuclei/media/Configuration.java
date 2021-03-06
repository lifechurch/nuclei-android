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
