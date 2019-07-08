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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import androidx.loader.content.CursorLoader;

import java.util.ArrayList;

public class Query<T> {

    public static final int QUERY_OPERATION_SELECT = 1;
    public static final int QUERY_OPERATION_INSERT = 2;
    public static final int QUERY_OPERATION_UPDATE = 3;
    public static final int QUERY_OPERATION_DELETE = 4;

    static Context CONTEXT;

    public static void initialize(Context context) {
        if (context != null)
            CONTEXT = context.getApplicationContext();
    }

    public static QueryArgs args() {
        return new QueryArgs();
    }

    @Deprecated
    public static QueryArgs args(String...args) {
        return args().args(args);
    }

    public static ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> ops) throws RemoteException, OperationApplicationException {
        return CONTEXT.getContentResolver().applyBatch(authority, ops);
    }

    public final String id;
    public final PersistenceUri uri;
    public final Class<T> type;
    public final int opType;

    public final String[] projection;
    public final int placeholders;
    public final String selection;
    public final String orderBy;
    public final PersistenceList.CursorObjectMapper<T> objectMapper;
    public final ContentValuesMapper<T> contentValuesMapper;

    public Query(String id, int opType, PersistenceUri uri, Class<T> type, PersistenceList.CursorObjectMapper<T> objectMapper, int placeholders, String selection, String orderBy, String...projection) {
        this.id = id;
        this.opType = opType;
        this.uri = uri;
        this.type = type;
        this.projection = projection;
        this.placeholders = placeholders;
        this.selection = selection;
        this.orderBy = orderBy;
        this.objectMapper = objectMapper;
        this.contentValuesMapper = null;
    }

    public Query(String id, int opType, PersistenceUri uri, Class<T> type, ContentValuesMapper<T> contentValuesMapper, int placeholders, String selection) {
        this.id = id;
        this.opType = opType;
        this.uri = uri;
        this.type = type;
        this.projection = null;
        this.placeholders = placeholders;
        this.selection = selection;
        this.orderBy = null;
        this.contentValuesMapper = contentValuesMapper;
        this.objectMapper = null;
    }

    public SelectQueryBuilder<T> newSelect() {
        if (opType != Query.QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Query is not a select");
        return new SelectQueryBuilder<>(this);
    }

    public UpdateQueryBuilder<T> newUpdate() {
        return new UpdateQueryBuilder<>(this);
    }

    int update(ContentValues values, String[] args) {
        if (opType != QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Not an update query");
        if (args != null && placeholders != args.length)
            throw new IllegalArgumentException("Invalid selection args");
        return CONTEXT.getContentResolver().update(uri.toUri(), values, selection, args);
    }

    int update(T value, String[] args) {
        if (opType != QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Not an update query");
        if (contentValuesMapper == null)
            throw new IllegalArgumentException("Content Value Mapper not set");
        if (args != null && placeholders != args.length)
            throw new IllegalArgumentException("Invalid selection args");
        ContentValues values = contentValuesMapper.map(value);
        return CONTEXT.getContentResolver().update(uri.toUri(), values, selection, args);
    }

    int delete(String[] args) {
        if (opType != QUERY_OPERATION_DELETE)
            throw new IllegalArgumentException("Not a delete query");
        if (args != null && placeholders != args.length)
            throw new IllegalArgumentException("Invalid selection args");
        return CONTEXT.getContentResolver().delete(uri.toUri(), selection, args);
    }

    Cursor execute(String[] args, String sort) {
        if (opType != QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Not a select query");
        if (args != null && placeholders != args.length)
            throw new IllegalArgumentException("Invalid selection args");
        return CONTEXT.getContentResolver().query(uri.toUri(), projection, selection, args, sort);
    }

    Cursor execute(String[] projection, String[] args, String sort) {
        if (opType != QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Not a select query");
        if (args != null && placeholders != args.length)
            throw new IllegalArgumentException("Invalid selection args");
        return CONTEXT.getContentResolver().query(uri.toUri(), projection, selection, args, sort);
    }

    android.content.CursorLoader executeLoader(String[] args, String sort) {
        if (opType != QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Not a select query");
        if (args != null && placeholders != args.length)
            throw new IllegalArgumentException("Invalid selection args");
        return new android.content.CursorLoader(CONTEXT, uri.toUri(), projection, selection, args, sort);
    }

    CursorLoader executeSupportLoader(String[] args, String sort) {
        if (opType != QUERY_OPERATION_SELECT)
            throw new IllegalArgumentException("Not a select query");
        if (args != null && placeholders != args.length)
            throw new IllegalArgumentException("Invalid selection args");
        return new CursorLoader(CONTEXT, uri.toUri(), projection, selection, args, sort);
    }

    @Deprecated
    public ContentProviderOperation toUpdateOperation(T object, String...args) {
        return toUpdateOperation(object, args().args(args));
    }

    public ContentProviderOperation toUpdateOperation(T object, QueryArgs args) {
        if (opType != QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Not an update query");
        if (contentValuesMapper == null)
            throw new IllegalArgumentException("Content Values Mapper is null");
        args.validate(this);
        return ContentProviderOperation.newUpdate(uri.toUri())
                .withSelection(selection, args.args())
                .withValues(contentValuesMapper.map(object))
                .build();
    }

    @Deprecated
    public ContentProviderOperation toDeleteOperation(String...args) {
        return toDeleteOperation(args().args(args));
    }

    public ContentProviderOperation toDeleteOperation(QueryArgs args) {
        if (opType != QUERY_OPERATION_DELETE)
            throw new IllegalArgumentException("Not a delete query");
        args.validate(this);
        return ContentProviderOperation.newDelete(uri.toUri())
                .withSelection(selection, args.args())
                .build();
    }

    public interface ContentValuesMapper<T> {
        ContentValues map(T object);
    }

    public static class MapperEntity<T> {

        public final PersistenceUri uri;
        public final ContentValuesMapper<T> mapper;

        public MapperEntity(PersistenceUri uri, ContentValuesMapper<T> mapper) {
            this.uri = uri;
            this.mapper = mapper;
        }

        public Uri replace(T object) {
            ContentValues contentValues = mapper.map(object);
            contentValues.put(nuclei.persistence.ContentProviderBase.REPLACE_RECORD, true);
            return insert(contentValues);
        }

        public int replace(T[] object) {
            int len = object.length;
            ContentValues[] contentValues = new ContentValues[len];
            for (int i = 0; i < len; i++) {
                contentValues[i] = mapper.map(object[i]);
                contentValues[i].put(nuclei.persistence.ContentProviderBase.REPLACE_RECORD, true);
            }
            return CONTEXT.getContentResolver().bulkInsert(uri.toUri(), contentValues);
        }

        public Uri insert(T object) {
            ContentValues contentValues = mapper.map(object);
            return insert(contentValues);
        }

        public Uri insert(ContentValues contentValues) {
            return CONTEXT.getContentResolver().insert(uri.toUri(), contentValues);
        }

        public int insert(T[] object) {
            int len = object.length;
            ContentValues[] contentValues = new ContentValues[len];
            for (int i = 0; i < len; i++) {
                contentValues[i] = mapper.map(object[i]);
            }
            return CONTEXT.getContentResolver().bulkInsert(uri.toUri(), contentValues);
        }

        public ContentProviderOperation toInsertOperation(T object) {
            return ContentProviderOperation.newInsert(uri.toUri())
                    .withValues(mapper.map(object))
                    .build();
        }

        public ContentProviderOperation toReplaceOperation(T object) {
            return ContentProviderOperation.newInsert(uri.toUri())
                    .withValues(mapper.map(object))
                    .withValue(ContentProviderBase.REPLACE_RECORD, true)
                    .build();
        }

    }

}
