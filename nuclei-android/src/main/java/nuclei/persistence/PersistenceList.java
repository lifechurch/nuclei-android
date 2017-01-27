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

import java.io.Closeable;
import java.util.List;

public interface PersistenceList<T> extends List<T>, Closeable {

    Query<T> getQuery();

    Cursor getCursor();

    void swapCursor(Query<T> query, Cursor cursor);

    @Override
    T get(int location);

    @Override
    int size();

    void recycle(T object);

    void recycle(Query<T> query, T object);

    boolean isClosed();

    void close();

    interface CursorObjectMapper<T> {
        void map(Cursor cursor, T object);
        T newObject();
    }

    interface Listener<T> {
        void onAvailable(PersistenceList<T> list, boolean sizeChanged);
    }

}
