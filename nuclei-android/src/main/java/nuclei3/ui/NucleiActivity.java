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
package nuclei3.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.os.Bundle;

import nuclei3.logs.Log;
import nuclei3.logs.Logs;
import nuclei3.logs.Trace;

/**
 * Base Activity with easy hooks for managing PersistenceLists and ContextHandles
 */
@TargetApi(15)
public abstract class NucleiActivity extends Activity implements LifecycleRegistryOwner {

    static final Log LOG = Logs.newLog(NucleiActivity.class);

    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

    @Override
    public LifecycleRegistry getLifecycle() {
        return mRegistry;
    }

    private Trace mTrace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

    @Override
    protected void onDestroy() {
        if (mTrace != null)
            mTrace.onDestroy(getClass());
        mTrace = null;
        super.onDestroy();
    }

}
