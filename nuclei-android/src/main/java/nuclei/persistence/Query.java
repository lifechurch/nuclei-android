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

import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class Query<T> {

    public static final int QUERY_OPERATION_SELECT = 1;
    public static final int QUERY_OPERATION_INSERT = 2;
    public static final int QUERY_OPERATION_UPDATE = 3;
    public static final int QUERY_OPERATION_DELETE = 4;

    public final String id;
    public final Uri uri;
    public final Class<T> type;
    public final int opType;

    public final String[] projection;
    public final String selection;
    public final String orderBy;
    public final PersistenceList.CursorObjectMapper<T> objectMapper;
    public final ContentValuesMapper<T> contentValuesMapper;

    public Query(String id, int opType, Uri uri, Class<T> type, PersistenceList.CursorObjectMapper<T> objectMapper, String selection, String orderBy, String...projection) {
        this.id = id;
        this.opType = opType;
        this.uri = uri;
        this.type = type;
        this.projection = projection;
        this.selection = selection;
        this.orderBy = orderBy;
        this.objectMapper = objectMapper;
        this.contentValuesMapper = null;
    }

    public Query(String id, int opType, Uri uri, Class<T> type, ContentValuesMapper<T> contentValuesMapper, String selection) {
        this.id = id;
        this.opType = opType;
        this.uri = uri;
        this.type = type;
        this.projection = null;
        this.selection = selection;
        this.orderBy = null;
        this.contentValuesMapper = contentValuesMapper;
        this.objectMapper = null;
    }

    public Uri insert(Context context, ContentValues values) {
        if (opType != QUERY_OPERATION_INSERT)
            throw new IllegalArgumentException("Not an insert query");
        return context.getContentResolver().insert(uri, values);
    }

    public Uri insert(Context context, T value) {
        if (opType != QUERY_OPERATION_INSERT)
            throw new IllegalArgumentException("Not an insert query");
        if (contentValuesMapper == null)
            throw new IllegalArgumentException("Content Value Mapper not set");
        ContentValues values = contentValuesMapper.map(value);
        return context.getContentResolver().insert(uri, values);
    }

    public int update(Context context, ContentValues values, String...args) {
        if (opType != QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Not an update query");
        return context.getContentResolver().update(uri, values, selection, args);
    }

    public int update(Context context, T value, String...args) {
        if (opType != QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Not an update query");
        if (contentValuesMapper == null)
            throw new IllegalArgumentException("Content Value Mapper not set");
        ContentValues values = contentValuesMapper.map(value);
        return context.getContentResolver().update(uri, values, selection, args);
    }

    public int delete(Context context, String...args) {
        if (opType != QUERY_OPERATION_DELETE)
            throw new IllegalArgumentException("Not a delete query");
        return context.getContentResolver().delete(uri, selection, args);
    }

    public Cursor execute(Context context, String[] args, String sort) {
        return execute(context.getContentResolver(), args, sort);
    }

    public Cursor execute(ContentResolver resolver, String[] args, String sort) {
        if (opType != QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Not a select query");
        return resolver.query(uri, projection, selection, args, sort);
    }

    @TargetApi(11)
    public android.content.CursorLoader executeLoader(Context context, String[] args, String sort) {
        if (opType != QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Not a select query");
        return new android.content.CursorLoader(context, uri, projection, selection, args, sort);
    }

    public android.support.v4.content.CursorLoader executeSupportLoader(Context context, String[] args, String sort) {
        if (opType != QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Not a select query");
        return new android.support.v4.content.CursorLoader(context, uri, projection, selection, args, sort);
    }

    public ContentProviderOperation toReplaceOperation(T object) {
        if (opType != QUERY_OPERATION_INSERT)
            throw new IllegalArgumentException("Not an insert query");
        if (contentValuesMapper == null)
            throw new IllegalArgumentException("Content Values Mapper is null");
        ContentValues values = contentValuesMapper.map(object);
        values.put(ContentProviderBase.REPLACE_RECORD, true);
        return ContentProviderOperation.newInsert(uri)
                .withValues(values)
                .build();
    }

    public ContentProviderOperation toInsertOperation(T object) {
        if (opType != QUERY_OPERATION_INSERT)
            throw new IllegalArgumentException("Not an insert query");
        if (contentValuesMapper == null)
            throw new IllegalArgumentException("Content Values Mapper is null");
        return ContentProviderOperation.newInsert(uri)
                .withValues(contentValuesMapper.map(object))
                .build();
    }

    public ContentProviderOperation toUpdateOperation(T object, String...selectionArgs) {
        if (opType != QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Not an update query");
        if (contentValuesMapper == null)
            throw new IllegalArgumentException("Content Values Mapper is null");
        return ContentProviderOperation.newUpdate(uri)
                .withSelection(selection, selectionArgs)
                .withValues(contentValuesMapper.map(object))
                .build();
    }

    public ContentProviderOperation toDeleteOperation(String...selectionArgs) {
        if (opType != QUERY_OPERATION_DELETE)
            throw new IllegalArgumentException("Not a delete query");
        return ContentProviderOperation.newDelete(uri)
                .withSelection(selection, selectionArgs)
                .build();
    }

    public interface ContentValuesMapper<T> {
        ContentValues map(T object);
    }

    public static class MapperEntity<T> {
        public final Uri uri;
        public final ContentValuesMapper<T> mapper;

        public MapperEntity(Uri uri, ContentValuesMapper<T> mapper) {
            this.uri = uri;
            this.mapper = mapper;
        }

        public ContentProviderOperation toInsertOperation(T object) {
            return ContentProviderOperation.newInsert(uri)
                    .withValues(mapper.map(object))
                    .build();
        }

        public ContentProviderOperation toReplaceOperation(T object) {
            return ContentProviderOperation.newInsert(uri)
                    .withValues(mapper.map(object))
                    .withValue(ContentProviderBase.REPLACE_RECORD, true)
                    .build();
        }

    }

}
