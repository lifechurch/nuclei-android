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

import android.content.Context;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public abstract class ResourceProvider {

    public static final int PREVIOUS = 1;
    public static final int NEXT = 2;
    public static final int STOP_CASTING = 3;
    public static final int CASTING_TO_DEVICE = 4;
    public static final int PAUSE = 5;
    public static final int PLAY = 6;
    public static final int SPEED = 9;
    public static final int TIMER = 10;
    public static final int OFF = 11;

    public static final int MINUTES = 12;
    public static final int HOURS = 13;

    public static final int ICON_CLOSE = 7;
    public static final int ICON_SMALL = 8;

    private static Context CONTEXT;
    private static ResourceProvider INSTANCE;

    public static void initialize(Context context, ResourceProvider config) {
        CONTEXT = context.getApplicationContext();
        INSTANCE = config;
    }

    public static ResourceProvider getInstance() {
        return INSTANCE;
    }

    protected Context getContext() {
        return CONTEXT;
    }

    public abstract String getNotificationChannelId();
    public abstract CharSequence getString(int id);
    public abstract CharSequence getString(int id, String v);
    public abstract CharSequence getQuantityString(int id, int quantity);
    public abstract int getDrawable(int id);

    public abstract int getExceptionCode(Exception err);
    public abstract String getExceptionMessage(Exception err);

    public List<String> getSpeeds() {
        return Arrays.asList("0.75x", "1x", "1.25x", "1.5x", "2x");
    }

    public float getSpeed(String speed) {
        return Float.parseFloat(speed.substring(0, speed.length() - 1));
    }

    public String getSelectedSpeed() {
        return String.format("%sx", NumberFormat.getNumberInstance(Locale.getDefault())
                .format(MediaProvider.getInstance().getAudioSpeed()));
    }

}
