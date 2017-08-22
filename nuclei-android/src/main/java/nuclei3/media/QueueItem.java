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
package nuclei3.media;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;

public class QueueItem {

    private MediaSessionCompat.QueueItem mQueueItem;

    public QueueItem(MediaSessionCompat.QueueItem queueItem) {
        mQueueItem = queueItem;
    }

    public long getQueueId() {
        return mQueueItem.getQueueId();
    }

    public String getMediaId() {
        return mQueueItem.getDescription().getMediaId();
    }

    public Uri getIconUri() {
        return mQueueItem.getDescription().getIconUri();
    }

    public void setIcon(Bitmap bitmap) {
        MediaDescriptionCompat desc = mQueueItem.getDescription();
        desc = new MediaDescriptionCompat.Builder()
                .setTitle(desc.getTitle())
                .setDescription(desc.getDescription())
                .setMediaId(desc.getMediaId())
                .setMediaUri(desc.getMediaUri())
                .setIconUri(desc.getIconUri())
                .setIconBitmap(bitmap)
                .setExtras(desc.getExtras())
                .setSubtitle(desc.getSubtitle())
                .build();
        mQueueItem = new MediaSessionCompat.QueueItem(desc, mQueueItem.getQueueId());
    }

    public MediaSessionCompat.QueueItem getItem() {
        return mQueueItem;
    }

}
