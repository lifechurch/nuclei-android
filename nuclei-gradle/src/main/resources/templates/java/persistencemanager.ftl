<#--
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
-->
package ${package}.persistence;

import nuclei.persistence.Query;
import nuclei.persistence.PersistenceList;
import nuclei.persistence.PersistenceListImpl;
import nuclei.persistence.Query.MapperEntity;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ${package}.providers.Schemas;

public class Persistence {

    private static Context sContext;

    public static void initialize(Context context) {
        sContext = context.getApplicationContext();
    }

    public static <T> PersistenceList<T> query(Query<T> query, String...selectionArgs) {
        return queryWithOrder(query, null, selectionArgs);
    }

    public static <T> PersistenceList<T> queryWithOrder(Query<T> query, String orderBy, String...selectionArgs) {
        if (query.opType != Query.QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Query is not a select");
        Cursor cursor = query.execute(sContext, selectionArgs, orderBy == null ? query.orderBy : orderBy);
        if (cursor == null)
            return null;
        return new PersistenceListImpl<T>(query, cursor);
    }

    public static <T> T queryOne(Query<T> query, String...selectionArgs) {
        PersistenceList<T> list = null;
        try {
            list = query(query, selectionArgs);
            if (list != null && list.size() > 0)
                return list.get(0);
        } finally {
            if (list != null)
                list.close();
        }
        return null;
    }

    private static final String[] COLUMNS_COUNT = new String[]{"count(*) as _id"};

    public static int getCount(Query query, String...selectionArgs) {
        if (query.opType != Query.QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Query is not a select");
        Cursor cursor = sContext.getContentResolver()
                                .query(query.uri, COLUMNS_COUNT, query.selection, selectionArgs, null);
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

    public static <T> PersistenceList<T> wrap(Query<T> query, Cursor cursor) {
        return new PersistenceListImpl<T>(query, cursor);
    }

    public static <T> Uri replace(MapperEntity<T> entity, T object) {
        ContentValues contentValues = entity.mapper.map(object);
        contentValues.put(nuclei.persistence.ContentProviderBase.REPLACE_RECORD, true);
        return sContext.getContentResolver().insert(entity.uri, contentValues);
    }

    public static <T> Uri insert(MapperEntity<T> entity, T object) {
        ContentValues contentValues = entity.mapper.map(object);
        return sContext.getContentResolver().insert(entity.uri, contentValues);
    }

    public static <T> int insert(MapperEntity<T> entity, T[] object) {
        int len = object.length;
        ContentValues[] contentValues = new ContentValues[len];
        for (int i = 0; i < len; i++) {
            contentValues[i] = entity.mapper.map(object[i]);
        }
        return sContext.getContentResolver().bulkInsert(entity.uri, contentValues);
    }

    public static <T> int update(Query<T> query, T object, String...selectionArgs) {
        if (query.opType != Query.QUERY_OPERATION_UPDATE)
                throw new IllegalArgumentException("Query is not an update");
        return query.update(sContext, object, selectionArgs);
    }

    public static <T> int update(Query<T> query, ContentValues contentValues, String...selectionArgs) {
        if (query.opType != Query.QUERY_OPERATION_UPDATE)
                throw new IllegalArgumentException("Query is not an update");
        return query.update(sContext, contentValues, selectionArgs);
    }

    public static <T> int delete(Query<T> query, String...selectionArgs) {
        if (query.opType != Query.QUERY_OPERATION_DELETE)
                throw new IllegalArgumentException("Query is not a delete");
        return query.delete(sContext, selectionArgs);
    }

    public static <T> ContentValues toInsertContentValues(MapperEntity<T> entity, T object) {
        return entity.mapper.map(object);
    }

    public static <T> ContentValues toUpdateContentValues(Query<T> query, T object) {
        if (query.opType != Query.QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Query is not an update");
        return query.contentValuesMapper.map(object);
    }

    public static <T> ContentValues toDeleteContentValues(Query<T> query, T object) {
        if (query.opType != Query.QUERY_OPERATION_DELETE)
            throw new IllegalArgumentException("Query is not a delete");
        return query.contentValuesMapper.map(object);
    }

    public static ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> ops) throws RemoteException, OperationApplicationException {
        return sContext.getContentResolver().applyBatch(Schemas.AUTHORITY, ops);
    }

}
