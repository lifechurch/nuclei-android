/**
 * Copyright 2017 YouVersion
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

public class SelectQueryBuilder<T> extends QueryBuilder<T> {

    private static final String[] COLUMNS_COUNT = new String[]{"count(*) as _id"};

    SelectQueryBuilder(Query<T> query) {
        super(query);
    }

    public SelectQueryBuilder<T> orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public int count() {
        return count(null);
    }

    public int count(QueryArgs args) {
        args(args);
        Cursor cursor = query.execute(COLUMNS_COUNT, argVals, null);
        if (cursor == null)
            return 0;
        try {
            if (cursor.moveToNext())
                return cursor.getInt(0);
        } finally {
            cursor.close();
        }
        return 0;
    }

    public T executeOne(QueryArgs args) {
        Cursor cursor = execute(args);
        try {
            if (cursor.moveToFirst()) {
                T object = query.objectMapper.newObject();
                query.objectMapper.map(cursor, object);
                return object;
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public T executeOne() {
        return executeOne(null);
    }

    public Cursor execute(QueryArgs args) {
        args(args);
        return query.execute(argVals, orderBy);
    }

    public Cursor execute() {
        return execute(null);
    }

    public PersistenceList<T> executeList(QueryArgs args) {
        return new PersistenceListImpl<>(query, execute(args));
    }

    public PersistenceList<T> executeList() {
        return new PersistenceListImpl<>(query, execute());
    }

}
