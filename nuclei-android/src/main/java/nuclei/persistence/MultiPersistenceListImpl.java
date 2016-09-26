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
import java.util.HashMap;
import java.util.Map;

public class MultiPersistenceListImpl<T> extends AbstractList<T> implements PersistenceList<T> {

    Map<Query<T>, QueryList<T>> mListMap;
    QueryList<T>[] mLists;

    public MultiPersistenceListImpl(Query<T>[] queries) {
        mLists = new QueryList[queries.length];
        mListMap = new HashMap<>(queries.length);
        for (int i = 0; i < queries.length; i++) {
            mLists[i] = new QueryList<>(queries[i]);
            mListMap.put(queries[i], mLists[i]);
        }
    }

    public void setList(Query<T> query, PersistenceList<T> list) {
        QueryList<T> queryList = mListMap.get(query);
        if (queryList != null)
            queryList.list = list;
        else
            throw new IllegalArgumentException("Query not found");
    }

    public QueryList<T> getList(int location) {
        int offset = 0;
        for (QueryList<T> query : mLists) {
            if (query.list == null)
                continue;
            if (query.list.size() + offset > location) {
                query.offset = offset;
                return query;
            } else {
                offset += query.list.size();
            }
        }
        return null;
    }

    @Override
    public T get(int location) {
        QueryList<T> list = getList(location);
        if (list == null)
            return null;
        return list.get(location);
    }

    @Override
    public int size() {
        int size = 0;
        for (QueryList<T> list : mLists) {
            if (list.list != null)
                size += list.list.size();
        }
        return size;
    }

    @Override
    public Query<T> getQuery() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cursor getCursor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void swapCursor(Query<T> query, Cursor cursor) {
        QueryList<T> list = mListMap.get(query);
        if (list == null)
            throw new IllegalArgumentException("Invalid query");
        list.list.swapCursor(query, cursor);
    }

    @Override
    public void recycle(T object) {
        throw new UnsupportedOperationException("Use recycle(Query, Object)");
    }

    @Override
    public void recycle(Query<T> query, T object) {
        QueryList<T> list = mListMap.get(query);
        if (list == null)
            throw new IllegalArgumentException("Invalid query");
        if (list.list != null)
            list.list.recycle(query, object);
    }

    @Override
    public void close() {
        for (QueryList<T> list : mLists) {
            list.list.close();
        }
        mLists = null;
    }

    public static class QueryList<T> {

        public final Query<T> query;
        protected PersistenceList<T> list;
        protected int offset;

        public QueryList(Query<T> query) {
            this.query = query;
        }

        public T get(int location) {
            return list.get(location - offset);
        }

        public Cursor cursor() {
            return list.getCursor();
        }

        public int offset() {
            return offset;
        }

        public int size() {
            return list.size();
        }

    }

}
