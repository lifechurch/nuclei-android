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
package nuclei.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;

import nuclei.persistence.LoaderQueryBuilder;
import nuclei.persistence.PersistenceList;
import nuclei.persistence.Query;
import nuclei.persistence.QueryManager;
import nuclei.persistence.SupportPersistenceLoaderImpl;
import nuclei.persistence.adapter.PersistenceAdapterListener;
import nuclei.persistence.adapter.PersistenceListAdapter;
import nuclei.intent.IntentBuilderActivity;
import nuclei.task.ContextHandle;
import nuclei.logs.Log;
import nuclei.logs.Logs;
import nuclei.logs.Trace;

/**
 * Base Compat Activity with easy hooks for managing PersistenceLists and ContextHandles
 */
public abstract class NucleiCompatActivity extends AppCompatActivity implements IntentBuilderActivity, NucleiContext, QueryManager {

    static final Log LOG = Logs.newLog(NucleiCompatActivity.class);

    private SupportPersistenceLoaderImpl mLoader;
    private ActivityOptionsCompat mOptions;
    private LifecycleManager mLifecycleManager;

    protected <T extends Destroyable> T manage(T destroyable) {
        if (mLifecycleManager == null)
            mLifecycleManager = new LifecycleManager(LifecycleManager.ACTIVITY);
        mLifecycleManager.manage(LifecycleManager.ACTIVITY, destroyable);
        return destroyable;
    }

    protected void destroy(Destroyable destroyable) {
        if (mLifecycleManager != null)
            mLifecycleManager.destroy(destroyable);
        else
            destroyable.onDestroy();
    }

    @Override
    public <T> LoaderQueryBuilder<T> newQuery(Query<T> query, PersistenceList.Listener<T> listener) {
        if (mLoader == null)
            mLoader = SupportPersistenceLoaderImpl.newLoaderManager(this, getSupportLoaderManager());
        return mLoader.newLoaderBuilder(query, listener);
    }

    @Override
    public <T> LoaderQueryBuilder<T> newQuery(Query<T> query, PersistenceListAdapter<T> adapter) {
        if (mLoader == null)
            mLoader = SupportPersistenceLoaderImpl.newLoaderManager(this, getSupportLoaderManager());
        return mLoader.newLoaderBuilder(query, new PersistenceAdapterListener<T>(adapter));
    }

    @Deprecated
    public <T> int executeQueryWithOrder(Query<T> query, PersistenceList.Listener<T> listener, String orderBy, String...selectionArgs) {
        try {
            if (mLoader == null)
                mLoader = SupportPersistenceLoaderImpl.newLoaderManager(this, getSupportLoaderManager());
            return mLoader.executeWithOrder(query, listener, orderBy, selectionArgs);
        } catch (IllegalStateException err) {
            LOG.wtf("Error executing query", err);
            return -1;
        }
    }

    @Deprecated
    public <T> int executeQueryWithOrder(Query<T> query, PersistenceListAdapter<T> adapter, String orderBy, String...selectionArgs) {
        return executeQueryWithOrder(query, new PersistenceAdapterListener<T>(adapter), orderBy, selectionArgs);
    }

    @Deprecated
    public <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener, String...selectionArgs) {
        try {
            if (mLoader == null)
                mLoader = SupportPersistenceLoaderImpl.newLoaderManager(this, getSupportLoaderManager());
            return mLoader.execute(query, listener, selectionArgs);
        } catch (IllegalStateException err) {
            LOG.wtf("Error executing query", err);
            return -1;
        }
    }

    @Deprecated
    public <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> adapter, String...selectionArgs) {
        return executeQuery(query, new PersistenceAdapterListener<T>(adapter), selectionArgs);
    }

    @Deprecated
    public void reexecuteQuery(int id, String...selectionArgs) {
        if (mLoader != null)
            mLoader.reexecute(id, selectionArgs);
    }

    @Deprecated
    public <T> void reexecuteQueryByName(int id, Query<T> query, String...selectionArgs) {
        if (mLoader != null) {
            mLoader.reexecute(id, query, selectionArgs);
        }
    }

    public void destroyQuery(int id) {
        if (mLoader != null)
            mLoader.destroyQuery(id);
    }

    @Override
    public void setDefaultActivityOptions(ActivityOptionsCompat options) {
        mOptions = options;
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

    private ContextHandle mHandle;
    private Trace mTrace;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Logs.TRACE) {
            mTrace = new Trace();
            mTrace.onCreate(getClass());
        }
    }

    protected void trace(String message) {
        if (mTrace != null)
            mTrace.trace(getClass(), message);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTrace != null)
            mTrace.onPause(getClass());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTrace != null)
            mTrace.onResume(getClass());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mTrace != null)
            mTrace.onStop(getClass());
    }

    /**
     * Get a managed context handle.
     *
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
        if (mTrace != null)
            mTrace.onDestroy(getClass());
        mTrace = null;
        super.onDestroy();
        if (mLoader != null)
            mLoader.onDestroy();
        mLoader = null;
    }

}
