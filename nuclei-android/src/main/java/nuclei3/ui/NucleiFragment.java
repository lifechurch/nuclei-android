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
package nuclei3.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.os.Bundle;
import android.support.annotation.Nullable;

import nuclei3.logs.Log;
import nuclei3.logs.Logs;
import nuclei3.logs.Trace;

/**
 * Base Fragment with easy hooks for managing PersistenceLists and ContextHandles
 */
@TargetApi(15)
public abstract class NucleiFragment extends Fragment implements LifecycleRegistryOwner {

    static final Log LOG = Logs.newLog(NucleiSupportFragment.class);

    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

    @Override
    public LifecycleRegistry getLifecycle() {
        return mRegistry;
    }

    private Trace mTrace;

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

    @Override
    public void onDestroy() {
        if (mTrace != null)
            mTrace.onDestroy(getClass());
        mTrace = null;
        super.onDestroy();
    }

}
