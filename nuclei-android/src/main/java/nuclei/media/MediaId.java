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

import android.net.Uri;

public abstract class MediaId {

    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_AUDIO = 2;

    public final Uri uri;
    public final int type;
    public final boolean queue;

    public MediaId(Uri uri) {
        this.uri = uri;
        this.queue = Boolean.parseBoolean(uri.getQueryParameter("_queue"));
        this.type = getInt("_type", -1);
    }

    protected int getInt(String name, int defaultValue) {
        try {
            return Integer.parseInt(uri.getQueryParameter(name));
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    protected long getLong(String name, long defaultValue) {
        try {
            return Long.parseLong(uri.getQueryParameter(name));
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MediaId && uri.compareTo(((MediaId) o).uri) == 0;
    }

    @Override
    public String toString() {
        return uri.toString();
    }

}
