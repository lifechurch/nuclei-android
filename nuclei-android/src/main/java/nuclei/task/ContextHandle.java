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
package nuclei.task;

import android.app.Application;
import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * One of the most common errors that I have noticed are caused by Contexts having lost scope
 * when they are expected to still exist.  As a result, work is executed when it doesn't need to be
 * and Exceptions thrown due to null pointers.
 * <br />
 * This results in the need for lots of if (getActivity() != null) or if (getContext() != null).
 * <br />
 * The ContextHandle helps get around all of this when used with things like Results and Tasks.
 * <br />
 * You can also extend from the NeuronActivity and NeuronFragments to assist in the management
 * of the ContextHandles.
 * <br />
 * You should also call ContextHandle.initialize in your main application class to provide a
 * global, never released ContextHandle.
 */
public final class ContextHandle {

    private static ContextHandle applicationHandle;

    public static void initialize(Application application) {
        applicationHandle = new ContextHandle(application);
    }

    public static ContextHandle getApplicationHandle() {
        if (applicationHandle == null)
            throw new RuntimeException("ContextHandle#initialize not called");
        return applicationHandle;
    }

    public static ContextHandle obtain(Context context) {
        return new ContextHandle(context);
    }

    private WeakReference<Context> mContext;

    private ContextHandle(Context context) {
        mContext = new WeakReference<>(context);
    }

    public Context get() {
        return mContext.get();
    }

    public void attach(Context context) {
        if (applicationHandle == this)
            throw new RuntimeException("You cannot attach the application handle.");
        mContext = new WeakReference<>(context);
    }

    public void release() {
        if (applicationHandle == this)
            throw new RuntimeException("You cannot release the application handle.");
        mContext.clear();
    }

}
