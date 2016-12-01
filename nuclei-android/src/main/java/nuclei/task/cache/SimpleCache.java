/**
 * Copyright 2016 YouVersion
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.task.cache;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import okhttp3.internal.Util;
import okhttp3.internal.cache.DiskLruCache;
import okhttp3.internal.io.FileSystem;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public final class SimpleCache implements Closeable, Flushable {

    private static final int VERSION = 1;
    private static final int HEADER = 0;
    private static final int CONTENT = 1;

    private static String stringToKey(String key) {
        return Util.md5Hex(key);
    }

    final DiskLruCache cache;

    public SimpleCache(File directory, long maxSize) {
        cache = DiskLruCache.create(FileSystem.SYSTEM, directory, VERSION, 2, maxSize);
    }

    public Iterator<String> keys() throws IOException { // copy of okhttp3.Cache.urls()
        return new Iterator<String>() {
            final Iterator<DiskLruCache.Snapshot> delegate = cache.snapshots();

            String nextKey;
            boolean canRemove;

            @Override
            public boolean hasNext() {
                if (nextKey != null)
                    return true;
                canRemove = false;
                while (delegate.hasNext()) {
                    DiskLruCache.Snapshot snapshot = delegate.next();
                    try {
                        BufferedSource metadata = Okio.buffer(snapshot.getSource(HEADER));
                        nextKey = metadata.readUtf8LineStrict();
                        return true;
                    } catch (IOException ignored) {
                    } finally {
                        snapshot.close();
                    }
                }
                return false;
            }

            @Override
            public String next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                String result = nextKey;
                nextKey = null;
                canRemove = true;
                return result;
            }

            @Override
            public void remove() {
                if (!canRemove)
                    throw new IllegalStateException("remove() before next()");
                delegate.remove();
            }
        };
    }

    public Entry get(String key) {
        DiskLruCache.Snapshot snapshot = null;
        try {
            String cacheKey = stringToKey(key);
            snapshot = cache.get(cacheKey);
            if (snapshot == null)
                return null;
            Entry entry = new Entry(snapshot);
            if (!entry.matches(key, VERSION)) {
                Util.closeQuietly(entry);
                return null;
            }
            return entry;
        } catch (IOException e) {
            Util.closeQuietly(snapshot);
            return null;
        }
    }

    /**
     * @param key
     * @param ttl The number of seconds this entry is valid
     * @return
     * @throws IOException
     */
    public Entry put(String key, int ttl) throws IOException {
        String cacheKey = stringToKey(key);
        DiskLruCache.Editor editor = null;
        try {
            editor = cache.edit(cacheKey);
            if (editor == null)
                return null;
            return new Entry(key, System.currentTimeMillis(), ttl, editor);
        } catch (IOException e) {
            try {
                if (editor != null)
                    editor.abort();
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public boolean remove(String key) throws IOException {
        return cache.remove(stringToKey(key));
    }

    public void evictAll() throws IOException {
        cache.evictAll();
    }

    public boolean isClosed() {
        return cache.isClosed();
    }

    @Override
    public void flush() throws IOException {
        cache.flush();
    }

    @Override
    public void close() throws IOException {
        cache.close();
    }

    public static final class Entry implements Closeable {

        private final String key;
        private final int version;
        private final DiskLruCache.Snapshot snapshot;
        private final DiskLruCache.Editor editor;
        private final long created;
        private final long ttl;

        Entry(DiskLruCache.Snapshot snapshot) throws IOException {
            this.snapshot = snapshot;
            this.editor = null;

            BufferedSource source = Okio.buffer(snapshot.getSource(HEADER));
            try {
                key = source.readUtf8LineStrict();
                version = source.readInt();
                created = source.readLong();
                ttl = source.readLong();
            } finally {
                source.close();
            }
        }

        Entry(String key, long created, int ttl, DiskLruCache.Editor editor) throws IOException {
            this.snapshot = null;
            this.editor = editor;
            this.key = key;
            this.version = VERSION;
            this.created = created;
            this.ttl = ttl * 1000;

            BufferedSink sink = Okio.buffer(editor.newSink(HEADER));
            sink.writeUtf8(key);
            sink.writeByte('\n');
            sink.writeInt(version);
            sink.writeLong(created);
            sink.writeLong(this.ttl);
            sink.close();
        }

        public long getCreated() {
            return created;
        }

        public boolean isExpired() {
            return getCreated() + ttl < System.currentTimeMillis();
        }

        public BufferedSink newSink() throws IOException {
            return Okio.buffer(editor.newSink(CONTENT));
        }

        public BufferedSource getSource() throws IOException {
            return Okio.buffer(snapshot.getSource(CONTENT));
        }

        boolean matches(String key, int version) {
            return this.key.equals(key)
                    && this.version == version;
        }

        public void abort() throws IOException {
            if (editor != null)
                editor.abortUnlessCommitted();
        }

        public void close() throws IOException {
            if (snapshot != null)
                snapshot.close();
            if (editor != null)
                editor.commit();
        }

    }


}
