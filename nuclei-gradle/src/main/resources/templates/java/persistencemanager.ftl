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
import android.content.Context;
import android.net.Uri;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ${package}.providers.Schemas;

public class Persistence {

    @Deprecated
    public static void initialize(Context context) {
        // NO-OP
    }

    @Deprecated
    public static <T> PersistenceList<T> query(Query<T> query, String...selectionArgs) {
        return queryWithOrder(query, null, selectionArgs);
    }

    @Deprecated
    public static <T> PersistenceList<T> queryWithOrder(Query<T> query, String orderBy, String...selectionArgs) {
        if (query.opType != Query.QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Query is not a select");
        orderBy = orderBy == null ? query.orderBy : orderBy;
        Cursor cursor = query.newSelect().orderBy(orderBy).execute(Query.args(selectionArgs));
        if (cursor == null)
            return null;
        return new PersistenceListImpl<T>(query, cursor);
    }

    @Deprecated
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

    @Deprecated
    public static int getCount(Query query, String...selectionArgs) {
        return query.newSelect().count(Query.args(selectionArgs));
    }

    public static <T> PersistenceList<T> wrap(Query<T> query, Cursor cursor) {
        return new PersistenceListImpl<T>(query, cursor);
    }

    @Deprecated
    public static <T> Uri replace(MapperEntity<T> entity, T object) {
        return entity.replace(object);
    }

    @Deprecated
    public static <T> Uri insert(MapperEntity<T> entity, T object) {
        return entity.insert(object);
    }

    @Deprecated
    public static <T> int insert(MapperEntity<T> entity, T[] object) {
        return entity.insert(object);
    }

    @Deprecated
    public static <T> int update(Query<T> query, T object, String...selectionArgs) {
        return query.newUpdate().update(Query.args(selectionArgs), object);
    }

    @Deprecated
    public static <T> int update(Query<T> query, ContentValues contentValues, String...selectionArgs) {
        return query.newUpdate().update(Query.args(selectionArgs), contentValues);
    }

    @Deprecated
    public static <T> int delete(Query<T> query, String...selectionArgs) {
        return query.newUpdate().delete(Query.args(selectionArgs));
    }

    @Deprecated
    public static <T> ContentValues toInsertContentValues(MapperEntity<T> entity, T object) {
        return entity.mapper.map(object);
    }

    @Deprecated
    public static <T> ContentValues toUpdateContentValues(Query<T> query, T object) {
        if (query.opType != Query.QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Query is not an update");
        return query.contentValuesMapper.map(object);
    }

    @Deprecated
    public static <T> ContentValues toDeleteContentValues(Query<T> query, T object) {
        if (query.opType != Query.QUERY_OPERATION_DELETE)
            throw new IllegalArgumentException("Query is not a delete");
        return query.contentValuesMapper.map(object);
    }

    public static ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> ops) throws RemoteException, OperationApplicationException {
        return Query.applyBatch(Schemas.AUTHORITY, ops);
    }

}
