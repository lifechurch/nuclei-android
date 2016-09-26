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
package ${packageName};

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

public class NucleiDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "${databaseName}";
    public static final int DB_VERSION = ${versionCount};

    private Context mContext;

    protected NucleiDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            db.setForeignKeyConstraintsEnabled(true);
        else
            db.execSQL("PRAGMA foreign_keys=ON;");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        <#list baseVersion.sql as sql>
        db.execSQL("${sql}");
        </#list>
        onUpgrade(db, 1, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        <#if versions?size gt 1>
        <#assign v = 0>
        for (int v = oldVersion + 1; v <= newVersion; v++) {
            switch (v) {
                case 1:
                    break; <#list versions as version> <#assign v = v + 1> <#if v gt 1>
                case ${v}:
                    <#list version.sql as sql>
                    db.execSQL("${sql}");
                    </#list>
                    break; </#if></#list>
                default:
                    throw new UnsupportedOperationException();
            }
        }
        </#if>
    }

}