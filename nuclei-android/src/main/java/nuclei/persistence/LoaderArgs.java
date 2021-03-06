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

class LoaderArgs<T> {

    private PersistenceList<T> list;

    Query<T> query;
    String[] selectionArgs;
    String orderBy;
    PersistenceList.Listener<T> listener;

    void onAvailable(Cursor cursor) {
        if (list == null && (cursor == null || cursor.isClosed()))
            return;
        boolean sizeChanged = false;
        if (list == null)
            list = new PersistenceListImpl<>(query, cursor);
        else {
            sizeChanged = cursor != null && list.size() != cursor.getCount();
            list.swapCursor(query, cursor);
        }
        listener.onAvailable(list, sizeChanged);
    }

}
