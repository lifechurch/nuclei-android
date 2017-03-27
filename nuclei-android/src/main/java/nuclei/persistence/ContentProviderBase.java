/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
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

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

// derived from com/android/browser/provider/SQLiteContentProvider.java
public abstract class ContentProviderBase extends ContentProvider {

    public static final String REPLACE_RECORD = "___replace_record___";

    private static final int SLEEP_AFTER_YIELD_DELAY = 4000;
    private static final int MAX_OPERATIONS_PER_YIELD_POINT = 500;
    protected static final Token sToken = new Token();

    private final ThreadLocal<Boolean> mApplyingBatch = new ThreadLocal<>();
    private final Set<Uri> mChangedUris = new HashSet<>();
    private SQLiteDatabase mDb;
    private SQLiteOpenHelper mDbHelper;

    @Override
    public boolean onCreate() {
        Query.initialize(getContext());
        mDbHelper = onCreateHelper();
        return true;
    }

    protected abstract SQLiteOpenHelper onCreateHelper();

    @Override
    public abstract String getType(@NonNull Uri uri);

    @Override
    public abstract Cursor query(@NonNull Uri uri, String[] columns, String where, String[] whereArgs, String sortOrder);

    private boolean applyingBatch() {
        Boolean applyingBatch = mApplyingBatch.get();
        return applyingBatch != null && applyingBatch;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Uri result = null;
        boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            mDb = mDbHelper.getWritableDatabase();
            mDb.beginTransaction();
            try {
                result = insertInTransaction(mDb, uri, values);
                mDb.setTransactionSuccessful();
                mChangedUris.add(uri);
            } finally {
                mDb.endTransaction();
            }
            onEndTransaction();
        } else {
            result = insertInTransaction(mDb, uri, values);
            mChangedUris.add(uri);
        }
        return result;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        int numValues = values.length;
        mDb = mDbHelper.getWritableDatabase();
        mDb.beginTransaction();
        try {
            for (int i = 0; i < numValues; i++) {
                insertInTransaction(mDb, uri, values[i]);
                mChangedUris.add(uri);
                mDb.yieldIfContendedSafely();
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        onEndTransaction();
        return numValues;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            mDb = mDbHelper.getWritableDatabase();
            mDb.beginTransaction();
            try {
                count = updateInTransaction(mDb, uri, values, selection, selectionArgs);
                mDb.setTransactionSuccessful();
                mChangedUris.add(uri);
            } finally {
                mDb.endTransaction();
            }
            onEndTransaction();
        } else {
            count = updateInTransaction(mDb, uri, values, selection, selectionArgs);
            mChangedUris.add(uri);
        }
        return count;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            mDb = mDbHelper.getWritableDatabase();
            mDb.beginTransaction();
            try {
                count = deleteInTransaction(mDb, uri, selection, selectionArgs);
                mDb.setTransactionSuccessful();
                mChangedUris.add(uri);
            } finally {
                mDb.endTransaction();
            }
            onEndTransaction();
        } else {
            count = deleteInTransaction(mDb, uri, selection, selectionArgs);
            mChangedUris.add(uri);
        }
        return count;
    }

    @Override
    public
    @NonNull
    ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        int ypCount = 0;
        int opCount = 0;
        mDb = mDbHelper.getWritableDatabase();
        mDb.beginTransaction();
        try {
            mApplyingBatch.set(true);
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                if (++opCount >= MAX_OPERATIONS_PER_YIELD_POINT) {
                    throw new OperationApplicationException(
                            "Too many content provider operations between yield points. "
                                    + "The maximum number of operations per yield point is "
                                    + MAX_OPERATIONS_PER_YIELD_POINT, ypCount);
                }
                final ContentProviderOperation operation = operations.get(i);
                if (i > 0 && operation.isYieldAllowed()) {
                    opCount = 0;
                    if (mDb.yieldIfContendedSafely(SLEEP_AFTER_YIELD_DELAY)) {
                        ypCount++;
                    }
                }
                results[i] = operation.apply(this, results, i);
            }
            mDb.setTransactionSuccessful();
            return results;
        } finally {
            mApplyingBatch.set(false);
            mDb.endTransaction();
            onEndTransaction();
        }
    }

    protected void onEndTransaction() {
        Set<Uri> changed;
        synchronized (mChangedUris) {
            changed = new HashSet<>(mChangedUris);
            mChangedUris.clear();
        }
        Context context = getContext();
        if (context != null) {
            ContentResolver resolver = context.getContentResolver();
            for (Uri uri : changed) {
                resolver.notifyChange(uri, null, false);
            }
        }
    }

    // transactions
    protected abstract Uri insertInTransaction(SQLiteDatabase database, Uri uri, ContentValues values);

    protected abstract int updateInTransaction(SQLiteDatabase database, Uri uri, ContentValues values, String selection, String[] selectionArgs);

    protected abstract int deleteInTransaction(SQLiteDatabase database, Uri uri, String selection, String[] selectionArgs);

    // query
    protected Cursor query(Uri uri, String tableName, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        String orderBy = sortOrder;
        if (TextUtils.isEmpty(sortOrder))
            orderBy = "_id asc";
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(tableName);
        mDb = mDbHelper.getWritableDatabase();
        return qb.query(mDb,
                columns,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );
    }

    protected Cursor queryById(Uri uri, String tableName, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(tableName);
        StringBuilder w = new StringBuilder();
        w.append("_id = ");
        w.append(uri.getLastPathSegment());
        if (selection != null) {
            w.append(" AND (");
            w.append(selection);
            w.append(")");
        }
        mDb = mDbHelper.getWritableDatabase();
        return qb.query(mDb,
                columns,
                w.toString(),
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    // insert
    protected Uri insert(SQLiteDatabase database, Uri uri, String tableName, ContentValues values) {
        if (values.containsKey(REPLACE_RECORD)) {
            values.remove(REPLACE_RECORD);
            long rowId = database.replaceOrThrow(
                    tableName,
                    "_id", // A hack, SQLite sets this column value to null if values is empty.
                    values
            );
            if (rowId > 0)
                return ContentUris.withAppendedId(uri, rowId);
        } else {
            long rowId = database.insertOrThrow(
                    tableName,
                    "_id", // A hack, SQLite sets this column value to null if values is empty.
                    values
            );
            if (rowId > 0)
                return ContentUris.withAppendedId(uri, rowId);
        }
        return null;
    }

    // update
    protected int update(SQLiteDatabase database, Uri uri, String tableName, ContentValues values, String selection, String[] selectionArgs) {
        return database.update(
                tableName,
                values,
                selection,
                selectionArgs
        );
    }

    protected int updateById(SQLiteDatabase database, Uri uri, String tableName, ContentValues values, String selection, String[] selectionArgs) {
        StringBuilder w = new StringBuilder();
        w.append("_id = ");
        w.append(uri.getLastPathSegment());
        if (selection != null) {
            w.append(" AND (");
            w.append(selection);
            w.append(")");
        }
        return database.update(
                tableName,
                values,
                w.toString(),
                selectionArgs
        );
    }

    // delete
    protected int delete(SQLiteDatabase database, Uri uri, String tableName, String where, String[] whereArgs) {
        return database.delete(
                tableName,
                where,
                whereArgs
        );
    }

    protected int deleteById(SQLiteDatabase database, Uri uri, String tableName, String where, String[] whereArgs) {
        String finalWhere;
        StringBuilder w = new StringBuilder();
        w.append("_id = ");
        w.append(uri.getLastPathSegment());
        if (where != null) {
            w.append(" AND (");
            w.append(where);
            w.append(")");
        }
        finalWhere = w.toString();
        return database.delete(
                tableName,
                finalWhere,
                whereArgs
        );
    }

    protected static class Token {

    }
}
