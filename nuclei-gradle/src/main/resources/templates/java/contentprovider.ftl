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
package ${package};

import nuclei.persistence.ContentProviderBase;

import android.content.ContentUris;
import android.database.sqlite.SQLiteQueryBuilder;

import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class NucleiContentProvider extends ContentProviderBase {

    private static final int SLEEP_AFTER_YIELD_DELAY = 4000;
    private static final int MAX_OPERATIONS_PER_YIELD_POINT = 500;

    private static UriMatcher sUriMatcher;
    private static String sAuthority;

    static {
        setAuthority(Schemas.DEFAULT_AUTHORITY);
    }

    public static String getAuthority() {
        return sAuthority;
    }

    public static void setAuthority(String authority) {
        if (authority == null)
            throw new NullPointerException();
        if (sAuthority != null && sAuthority.equals(authority))
            return;
        sAuthority = authority;
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        <#assign i = 0><#list models as model>
        sUriMatcher.addURI(authority, "${model.name}", ${i});
        sUriMatcher.addURI(authority, "${model.name}/#", ${i + 1});
        Schemas.${model.name}.CONTENT_URI.setAuthority(sToken, authority);
        <#assign i = i + 2></#list>
    }

    @Override
    protected SQLiteOpenHelper onCreateHelper() {
        return new NucleiDbHelper(getContext());
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) { <#assign i = 0><#list models as model>
            case ${i}:
                return "${model.contentType}";
            case ${i + 1}:
                return "${model.contentItemType}"; <#assign i = i + 2></#list>
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] columns, String where, String[] whereArgs, String sortOrder) {
        Cursor c;
        switch (sUriMatcher.match(uri)) { <#assign i = 0><#list models as model>
            case ${i}:
                c = query(uri, "${model.name}", columns, where, whereArgs, sortOrder);
                break;
            case ${i + 1}:
                c = queryById(uri, "${model.name}", columns, where, whereArgs, sortOrder);
                break; <#assign i = i + 2></#list>
            default:
                throw new UnsupportedOperationException();
        }
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    // transactions
    @Override
    protected Uri insertInTransaction(SQLiteDatabase database, Uri uri, ContentValues values) {
        Uri newUri;
        switch (sUriMatcher.match(uri)) { <#assign i = 0><#list models as model>
            case ${i}:
                newUri = insert(database, uri, "${model.name}", values);
                break; <#assign i = i + 2></#list>
            default:
                throw new UnsupportedOperationException();
        }
        return newUri;
    }

    @Override
    protected int updateInTransaction(SQLiteDatabase database, Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count;
        switch (sUriMatcher.match(uri)) { <#assign i = 0><#list models as model>
            case ${i}:
                count = update(database, uri, "${model.name}", values, selection, selectionArgs);
                break;
            case ${i + 1}:
                count = updateById(database, uri, "${model.name}", values, selection, selectionArgs);
                break; <#assign i = i + 2></#list>
            default:
                throw new UnsupportedOperationException();
        }
        return count;
    }

    @Override
    protected int deleteInTransaction(SQLiteDatabase database, Uri uri, String selection, String[] selectionArgs) {
        int count;
        switch (sUriMatcher.match(uri)) { <#assign i = 0><#list models as model>
            case ${i}:
                count = delete(database, uri, "${model.name}", selection, selectionArgs);
                break;
            case ${i + 1}:
                count = deleteById(database, uri, "${model.name}", selection, selectionArgs);
                break; <#assign i = i + 2></#list>
            default:
                throw new UnsupportedOperationException();
        }
        return count;
    }

}
