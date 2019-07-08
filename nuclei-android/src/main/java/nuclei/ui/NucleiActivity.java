/**
 * Copyright 2016 YouVersion
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityOptionsCompat;
import android.util.Log;

import nuclei.persistence.PersistenceList;
import nuclei.persistence.PersistenceLoaderImpl;
import nuclei.persistence.PersistenceObserver;
import nuclei.persistence.Query;
import nuclei.persistence.QueryArgs;
import nuclei.persistence.QueryManager;
import nuclei.persistence.adapter.PersistenceAdapterListener;
import nuclei.persistence.adapter.PersistenceListAdapter;
import nuclei.task.ContextHandle;

/**
 * Base Activity with easy hooks for managing PersistenceLists and ContextHandles
 */
@TargetApi(15)
public abstract class NucleiActivity extends Activity implements NucleiContext, QueryManager {

    private ContextHandle mHandle;
    private PersistenceLoaderImpl mLoader;
    private ActivityOptionsCompat mOptions;
    private LifecycleManager mLifecycleManager;
    private Handler mHandler;

    public void registerObserver(Uri uri, PersistenceObserver observer) {
        if (mHandler == null)
            mHandler = new Handler(Looper.getMainLooper());
        registerObserver(uri, mHandler, observer);
    }

    public void registerObserver(Uri uri, Handler handler, PersistenceObserver observer) {
        ContentObserverImpl contentObserver = new ContentObserverImpl(handler, this, observer);
        getContentResolver().registerContentObserver(uri, true, contentObserver);
        manage(contentObserver);
    }

    protected <T extends Destroyable> T manage(T destroyable) {
        if (mLifecycleManager == null)
            mLifecycleManager = new LifecycleManager(LifecycleManager.ACTIVITY);
        mLifecycleManager.manage(LifecycleManager.ACTIVITY, destroyable);
        return destroyable;
    }

    protected void destroy(Destroyable destroyable) {
        if (mLifecycleManager != null)
            mLifecycleManager.destroy(destroyable);
    }

    @Override
    public <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener, QueryArgs args) {
        if (mLoader == null)
            mLoader = PersistenceLoaderImpl.newLoaderManager(getLoaderManager());
        return mLoader.newLoaderBuilder(query, listener).execute(args);
    }

    @Override
    public <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener) {
        if (mLoader == null)
            mLoader = PersistenceLoaderImpl.newLoaderManager(getLoaderManager());
        return mLoader.newLoaderBuilder(query, listener).execute();
    }

    @Override
    public <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> listener, QueryArgs args) {
        if (mLoader == null)
            mLoader = PersistenceLoaderImpl.newLoaderManager(getLoaderManager());
        return mLoader.newLoaderBuilder(query, new PersistenceAdapterListener<T>(listener)).execute(args);
    }

    @Override
    public <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> listener) {
        if (mLoader == null)
            mLoader = PersistenceLoaderImpl.newLoaderManager(getLoaderManager());
        return mLoader.newLoaderBuilder(query, new PersistenceAdapterListener<T>(listener)).execute();
    }

    @Deprecated
    public <T> int executeQueryWithOrder(Query<T> query, PersistenceList.Listener<T> listener, String orderBy, String... selectionArgs) {
        try {
            if (mLoader == null)
                mLoader = PersistenceLoaderImpl.newLoaderManager(getLoaderManager());
            return mLoader.executeWithOrder(query, listener, orderBy, selectionArgs);
        } catch (IllegalStateException err) {
            Log.wtf("Error executing query", err);
            return -1;
        }
    }

    @Deprecated
    public <T> int executeQueryWithOrder(Query<T> query, PersistenceListAdapter<T> adapter, String orderBy, String... selectionArgs) {
        return executeQueryWithOrder(query, new PersistenceAdapterListener<T>(adapter), orderBy, selectionArgs);
    }

    @Deprecated
    public <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener, String... selectionArgs) {
        try {
            if (mLoader == null)
                mLoader = PersistenceLoaderImpl.newLoaderManager(getLoaderManager());
            return mLoader.execute(query, listener, selectionArgs);
        } catch (IllegalStateException err) {
            Log.wtf("Error executing query", err);
            return -1;
        }
    }

    @Deprecated
    public <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> adapter, String... selectionArgs) {
        return executeQuery(query, new PersistenceAdapterListener<T>(adapter), selectionArgs);
    }

    @Deprecated
    public void reexecuteQuery(int id, String... selectionArgs) {
        if (mLoader != null)
            mLoader.reexecute(id, selectionArgs);
    }

    @Deprecated
    public <T> void reexecuteQueryByName(int id, Query<T> query, String... selectionArgs) {
        if (mLoader != null) {
            mLoader.reexecute(id, query, selectionArgs);
        }
    }

    public void destroyQuery(int id) {
        if (mLoader != null)
            mLoader.destroyQuery(id);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        if (options != null || mOptions == null) {
            if (Build.VERSION.SDK_INT >= 16)
                super.startActivityForResult(intent, requestCode, options);
            else
                super.startActivityForResult(intent, requestCode);
        } else if (Build.VERSION.SDK_INT >= 16)
            super.startActivityForResult(intent, requestCode, mOptions.toBundle());
        else
            super.startActivityForResult(intent, requestCode);
        mOptions = null;
    }

    /**
     * Get a managed context handle.
     * <p>
     * When the context is destroyed, the handle will be released.
     *
     * @return The Context Handle
     */
    @Override
    public ContextHandle getContextHandle() {
        if (mHandle == null)
            mHandle = ContextHandle.obtain(this);
        return mHandle;
    }

    @Override
    public ContextHandle getViewContextHandle() {
        return getContextHandle();
    }

    @Override
    protected void onDestroy() {
        if (mLifecycleManager != null)
            mLifecycleManager.onDestroy(LifecycleManager.ACTIVITY);
        if (mHandle != null)
            mHandle.release();
        mHandle = null;
        super.onDestroy();
        if (mLoader != null)
            mLoader.onDestroy();
        mLoader = null;
        mHandler = null;
    }

}
