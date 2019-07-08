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
import android.os.Bundle;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.collection.SparseArrayCompat;

import static androidx.loader.app.LoaderManager.LoaderCallbacks;

public class SupportPersistenceLoaderImpl implements LoaderCallbacks<Cursor>, PersistenceLoader {

    LoaderManager mManager;
    SparseArrayCompat<LoaderArgs> mParams = new SparseArrayCompat<>();
    int mIndex;

    public static SupportPersistenceLoaderImpl newLoaderManager(LoaderManager manager) {
        SupportPersistenceLoaderImpl persistenceLoader = new SupportPersistenceLoaderImpl();
        persistenceLoader.mManager = manager;
        return persistenceLoader;
    }

    public <T> LoaderQueryBuilder<T> newLoaderBuilder(Query<T> query, PersistenceList.Listener<T> listener) {
        return new LoaderQueryBuilder<T>(query, this, listener);
    }

    public <T> int execute(Query<T> query, PersistenceList.Listener<T> listener, String...selectionArgs) {
        return executeWithOrder(query, listener, null, selectionArgs);
    }

    public <T> int executeWithOrder(Query<T> query, PersistenceList.Listener<T> listener, String orderBy, String...selectionArgs) {
        if (mParams == null || mManager == null)
            return -1;
        LoaderArgs<T> a = new LoaderArgs<T>();
        a.query = query;
        a.selectionArgs = selectionArgs;
        a.orderBy = orderBy == null ? query.orderBy : orderBy;
        a.listener = listener;

        mIndex++;
        mParams.put(mIndex, a);

        mManager.initLoader(mIndex, null, this);
        return mIndex;
    }

    public void reexecute(int id, String...selectionArgs) {
        if (mParams != null && mManager != null) {
            LoaderArgs a = mParams.get(id);
            a.selectionArgs = selectionArgs;
            mManager.restartLoader(id, null, this);
        }
    }

    public void reexecute(int id, Query query, String...selectionArgs) {
        if (mParams != null && mManager != null) {
            LoaderArgs a = mParams.get(id);
            if (a.query.type != query.type)
                throw new ClassCastException("Query Type mismatch");
            a.query = query;
            a.selectionArgs = selectionArgs;
            mManager.restartLoader(id, null, this);
        }
    }

    public void destroyQuery(int id) {
        if (mManager != null)
            mManager.destroyLoader(id);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mParams != null) {
            LoaderArgs a = mParams.get(id);
            return a.query.executeSupportLoader(a.selectionArgs, a.orderBy);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mParams != null) {
            LoaderArgs a = mParams.get(loader.getId());
            a.onAvailable(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mParams != null) {
            LoaderArgs a = mParams.get(loader.getId());
            a.onAvailable(null);
        }
    }

    public void onDestroy() {
        mManager = null;
        mParams = null;
    }

}
