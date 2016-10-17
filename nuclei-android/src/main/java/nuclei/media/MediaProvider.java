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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.util.LruCache;

import java.util.Collections;
import java.util.List;

import nuclei.media.playback.Playback;
import nuclei.task.Result;
import nuclei.task.Task;
import nuclei.task.Tasks;

public abstract class MediaProvider {

    public static final String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";
    public static final String CUSTOM_METADATA_TRACK_TYPE = "__SOURCE_TYPE__";
    public static final String CUSTOM_METADATA_TIMING_START = "__TIMING_START__";
    public static final String CUSTOM_METADATA_TIMING_END = "__TIMING_END__";

    public static final String MEDIA_ID_SCHEME = "nuclei-media";
    private static Context CONTEXT;
    private static MediaProvider INSTANCE;

    public static void initialize(Context context, MediaProvider provider) {
        CONTEXT = context.getApplicationContext();
        INSTANCE = provider;
    }

    public static MediaProvider getInstance() {
        return INSTANCE;
    }

    private static final int METADATA_CACHE_SIZE = 5;

    private final LruCache<String, MediaMetadata> mMetadataCache = new LruCache<>(METADATA_CACHE_SIZE);
    private final LruCache<String, Queue> mQueueCache = new LruCache<>(METADATA_CACHE_SIZE);

    public abstract float getAudioSpeed();

    public abstract void setAudioSpeed(float speed);

    public final <T extends MediaId> T getMediaId(String id) {
        T mediaId = parseMediaId(id);
        if (!mediaId.uri.getScheme().equals(MEDIA_ID_SCHEME))
            throw new IllegalArgumentException("Invalid Media Scheme " + MEDIA_ID_SCHEME + " != " + mediaId.uri.getScheme());
        return mediaId;
    }

    protected abstract <T extends MediaId> T parseMediaId(String id);

    public Uri getUri(String id) {
        return getMediaId(id).uri;
    }

    public Uri newUri(Object object) {
        MediaId id = newMediaId(object);
        return id.uri;
    }

    public abstract <T extends MediaId> T newMediaId(Object object);

    protected final Uri.Builder newUriBuilder(boolean queue, int type) {
        return new Uri.Builder()
                .scheme(MEDIA_ID_SCHEME)
                .appendQueryParameter("_queue", Boolean.toString(queue))
                .appendQueryParameter("_type", Integer.toString(type));
    }

    public abstract void onError(String message);

    public MediaBrowserServiceCompat.BrowserRoot getBrowserRoot(@NonNull String clientPackageName,
                                                                         int clientUid,
                                                                         Bundle rootHints) {
        return new MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_SCHEME + "://", null);
    }

    public abstract Result<String> search(final String query);

    public Result<MediaMetadata> getMediaMetadata(final MediaId id) {
        return Tasks.execute(new Task<MediaMetadata>() {
            @Override
            public String getId() {
                return "get-media-metadata";
            }

            @Override
            public void run(Context context) {
                MediaMetadata metadata = getMediaMetadataSync(id);
                onComplete(metadata);
            }
        })
        .addCallback(new Result.CallbackAdapter<MediaMetadata>() {
            @Override
            public void onResult(final MediaMetadata mediaMetadata) {
                mMetadataCache.put(id.toString(), mediaMetadata);

                final String url = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
                AlbumArtCache.getInstance().fetch(CONTEXT, url, new AlbumArtCache.FetchListener() {
                    @Override
                    public void onFetchedImage(String artUrl, Bitmap image) {
                        mediaMetadata.setAlbumArt(image);
                    }

                    @Override
                    public void onFetchedIcon(String artUrl, Bitmap icon) {
                        mediaMetadata.setDisplayIcon(icon);
                    }
                });
            }
        });
    }

    protected abstract MediaMetadata getMediaMetadataSync(final MediaId id);
    protected abstract Queue getQueueSync(MediaId id);

    public void clearMetadataCache() {
        mMetadataCache.evictAll();
    }

    public void clearQueueCache() {
        mQueueCache.evictAll();
    }

    public void evictQueue(Queue queue) {
        mQueueCache.remove(queue.getId().toString());
    }

    public void evictMetadataCache(MediaMetadata metadata) {
        mMetadataCache.remove(metadata.getMediaId());
    }

    public Result<Queue> getQueue(final MediaId id) {
        return Tasks.execute(new Task<Queue>() {
            @Override
            public String getId() {
                return "get-queue";
            }

            @Override
            public void run(Context context) {
                Queue items = getQueueSync(id);
                onComplete(items);
            }
        })
        .addCallback(new Result.CallbackAdapter<Queue>() {
            @Override
            public void onResult(final Queue queue) {
                if (queue != null && !queue.empty()) {
                    mQueueCache.put(id.toString(), queue);
                    for (QueueItem item : queue.getItems()) {
                        getQueueItemImage(item);
                    }
                }
            }
        });
    }

    private void getQueueItemImage(final QueueItem item) {
        Uri uri = item.getIconUri();
        if (uri != null) {
            final String url = uri.toString();
            AlbumArtCache.getInstance().fetch(CONTEXT, url, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetchedImage(String artUrl, Bitmap image) {

                }

                @Override
                public void onFetchedIcon(String artUrl, Bitmap icon) {
                    item.setIcon(icon);
                }
            });
        }
    }

    public Queue getCachedQueue(MediaId id) {
        Queue items = mQueueCache.get(id.toString());
        if (items == null)
            throw new NullPointerException("Media ID (" + id + ") not found");
        return items;
    }

    public MediaMetadata getCachedMedia(MediaId mediaId) {
        MediaMetadata mediaMetadataCompat = mMetadataCache.get(mediaId.toString());
        if (mediaMetadataCompat == null)
            throw new NullPointerException("Media ID (" + mediaId + ") not found");
        return mediaMetadataCompat;
    }

    public void onLoadChildren(
            @NonNull final String parentMediaId,
            @NonNull final MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(Collections.<MediaBrowserCompat.MediaItem>emptyList());
    }

    public abstract void onPlaybackStart(Playback playback, MediaId id);

    public abstract void onPlaybackPause(Playback playback, MediaId id);

    public abstract void onPlaybackSeekTo(Playback playback, MediaId id, long currentPosition, long newPosition);

    public abstract void onPlaybackStop(Playback playback, MediaId id);

    public abstract void onPlaybackNext(Playback playback, MediaId id);

    public abstract void onPlaybackPrevious(Playback playback, MediaId id);

    public abstract void onPlaybackCompletion(Playback playback, MediaId id);

}
