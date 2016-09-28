package nuclei.media;

import android.support.v4.media.session.MediaSessionCompat;

import java.util.ArrayList;
import java.util.List;

public class Queue {

    private MediaId mId;
    private CharSequence mTitle;
    private List<QueueItem> mItems;
    private int mItemPosition;
    private MediaId mPrevious;
    private MediaId mNext;

    public Queue(MediaId id, CharSequence title, List<QueueItem> items, MediaId previous, MediaId next) {
        mId = id;
        mTitle = title;
        mItems = items;
        mItemPosition = 0;
        if (previous != null && !previous.queue)
            throw new IllegalArgumentException("Previous Queue ID must be a queue");
        mPrevious = previous;
        if (next != null && !next.queue)
            throw new IllegalArgumentException("Next Queue ID must be a queue");
        mNext = next;
    }

    public MediaId getId() {
        return mId;
    }

    public MediaId getPreviousQueue() {
        return mPrevious;
    }

    public MediaId getNextQueue() {
        return mNext;
    }

    public String getCurrentId() {
        return getCurrentItem().getMediaId();
    }

    public QueueItem getCurrentItem() {
        return mItems.get(mItemPosition);
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public List<QueueItem> getItems() {
        return mItems;
    }

    public boolean hasNext() {
        return mItemPosition + 1 < mItems.size();
    }

    public boolean empty() {
        return mItems.isEmpty();
    }

    public QueueItem next() {
        mItemPosition++;
        return mItems.get(mItemPosition);
    }

    public boolean hasPrevious() {
        return mItemPosition > 0;
    }

    public QueueItem previous() {
        mItemPosition--;
        return mItems.get(mItemPosition);
    }

    public QueueItem moveToId(long id) {
        int i = 0;
        for (QueueItem item : mItems) {
            if (item.getQueueId() == id) {
                mItemPosition = i;
                break;
            }
            i++;
        }
        if (i == mItems.size())
            return null;
        return mItems.get(mItemPosition);
    }

    public boolean setMetadata(MediaMetadata metadata) {
        final String mediaId = metadata.getMediaId();
        int i = 0;
        for (QueueItem item : mItems) {
            if (item.getMediaId().equals(mediaId)) {
                mItemPosition = i;
                break;
            }
            i++;
        }
        return i != mItems.size();
    }

    public List<MediaSessionCompat.QueueItem> toItems() {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>(mItems.size());
        int size = mItems.size();
        for (int i = 0; i < size; i++)
            items.add(mItems.get(i).getItem());
        return items;
    }

}
