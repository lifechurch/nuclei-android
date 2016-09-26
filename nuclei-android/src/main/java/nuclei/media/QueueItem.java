package nuclei.media;

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
