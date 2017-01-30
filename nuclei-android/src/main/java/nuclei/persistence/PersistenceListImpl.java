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
package nuclei.persistence;

import android.database.Cursor;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.Queue;

public class PersistenceListImpl<T> extends AbstractList<T> implements PersistenceList<T> {

    Query<T> mQuery;
    Cursor mCursor;
    final Class<T> mType;
    final Queue<T> mObjectQueue;
    final CursorObjectMapper<T> mObjectMapper;

    public PersistenceListImpl(Query<T> query, Cursor cursor) {
        if (cursor == null)
            throw new NullPointerException("Cursor cannot be null");
        if (cursor.isClosed())
            throw new IllegalArgumentException("Cursor cannot be closed");
        mQuery = query;
        mCursor = cursor;
        mType = query.type;
        mObjectQueue = new ArrayDeque<>(1);
        mObjectMapper = query.objectMapper;
    }

    @Override
    public Query<T> getQuery() {
        return mQuery;
    }

    @Override
    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public void swapCursor(Query<T> query, Cursor cursor) {
        if (query == null)
            throw new NullPointerException("Query cannot be null");
        mQuery = query;
        if (mCursor != null && mCursor != cursor)
            mCursor.close();
        mCursor = cursor;
    }

    @Override
    public T get(int location) {
        if (mCursor == null)
            throw new ArrayIndexOutOfBoundsException("No Cursor");
        if (mCursor.isClosed())
            throw new ArrayIndexOutOfBoundsException("Cursor is Closed");
        if (location == -1 || !mCursor.moveToPosition(location))
            throw new ArrayIndexOutOfBoundsException("Invalid position: " + location);
        T object;
        if (mObjectQueue.isEmpty())
            object = mObjectMapper.newObject();
        else
            object = mObjectQueue.poll();
        mObjectMapper.map(mCursor, object);
        return object;
    }

    @Override
    public int size() {
        if (mCursor == null || mCursor.isClosed())
            return 0;
        return mCursor.getCount();
    }

    @Override
    public void recycle(T object) {
        recycle(mQuery, object);
    }

    @Override
    public void recycle(Query<T> query, T object) {
        if (mQuery == null)
            return;
        if (query != mQuery)
            throw new IllegalArgumentException("Invalid object, using different query");
        mObjectQueue.add(object);
    }

    @Override
    public boolean isClosed() {
        return mCursor == null || mCursor.isClosed();
    }

    @Override
    public void close() {
        if (mCursor != null)
            mCursor.close();
        mCursor = null;
        mQuery = null;
    }

}
