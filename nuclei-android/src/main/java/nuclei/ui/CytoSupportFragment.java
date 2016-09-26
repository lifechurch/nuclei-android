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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import nuclei.persistence.PersistenceList;
import nuclei.persistence.Query;
import nuclei.persistence.SupportPersistenceLoader;
import nuclei.persistence.adapter.PersistenceAdapterListener;
import nuclei.persistence.adapter.PersistenceListAdapter;
import nuclei.task.ContextHandle;
import nuclei.logs.Log;
import nuclei.logs.Logs;
import nuclei.logs.Trace;

/**
 * Base Fragment with easy hooks for managing PersistenceLists and ContextHandles
 */
public abstract class CytoSupportFragment extends Fragment implements NucleiContext {

    static final Log LOG = Logs.newLog(CytoSupportFragment.class);

    private ContextHandle mHandle;
    private Trace mTrace;
    private SupportPersistenceLoader mLoader;

    public <T> int executeQueryWithOrder(Query<T> query, PersistenceList.Listener<T> listener, String orderBy, String...selectionArgs) {
        try {
            if (mLoader == null)
                mLoader = SupportPersistenceLoader.newLoaderManager(getContext(), getLoaderManager());
            return mLoader.executeWithOrder(query, listener, orderBy, selectionArgs);
        } catch (IllegalStateException err) {
            LOG.wtf("Error executing query", err);
            return -1;
        }
    }

    public <T> int executeQueryWithOrder(Query<T> query, PersistenceListAdapter<T> adapter, String orderBy, String...selectionArgs) {
        return executeQueryWithOrder(query, new PersistenceAdapterListener<T>(adapter), orderBy, selectionArgs);
    }

    public <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener, String...selectionArgs) {
        try {
            if (mLoader == null)
                mLoader = SupportPersistenceLoader.newLoaderManager(getContext(), getLoaderManager());
            return mLoader.execute(query, listener, selectionArgs);
        } catch (IllegalStateException err) {
            LOG.wtf("Error executing query", err);
            return -1;
        }
    }

    public <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> adapter, String...selectionArgs) {
        return executeQuery(query, new PersistenceAdapterListener<T>(adapter), selectionArgs);
    }

    public void reexecuteQuery(int id, String...selectionArgs) {
        if (mLoader != null)
            mLoader.reexecute(id, selectionArgs);
    }

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
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
    public void onPause() {
        super.onPause();
        if (mTrace != null)
            mTrace.onPause(getClass());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTrace != null)
            mTrace.onResume(getClass());
    }

    @Override
    public void onStop() {
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
            mHandle = ContextHandle.obtain(getActivity());
        return mHandle;
    }

    @Override
    public void onDestroy() {
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
