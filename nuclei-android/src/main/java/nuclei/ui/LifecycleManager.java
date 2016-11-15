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

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class LifecycleManager {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ACTIVITY, FRAGMENT, VIEW})
    public @interface ManagedLifecycle {
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ACTIVITY, FRAGMENT})
    public @interface ContainerLifecycle {
    }

    public static final int ACTIVITY = 1;
    public static final int FRAGMENT = 2;
    public static final int VIEW = 3;

    private List<Destroyable> mViewDestroyables;
    private List<Destroyable> mContainerDestroyables;

    @ContainerLifecycle
    private int mContainerLifecycle;

    public LifecycleManager(@ContainerLifecycle int containerLifecycle) {
        mContainerLifecycle = containerLifecycle;
    }

    public void manage(@ManagedLifecycle int lifecycle, Destroyable destroyable) {
        if (lifecycle == VIEW) {
            if (mViewDestroyables == null)
                mViewDestroyables = new ArrayList<>();
            mViewDestroyables.add(destroyable);
        } else {
            if (lifecycle != mContainerLifecycle)
                throw new IllegalArgumentException("Invalid Lifecycle");
            if (mContainerDestroyables == null)
                mContainerDestroyables = new ArrayList<>();
            mContainerDestroyables.add(destroyable);
        }
    }

    public void onDestroy(@ManagedLifecycle int lifecycle) {
        if (lifecycle == VIEW) {
            if (mViewDestroyables != null) {
                for (int i = 0, len = mViewDestroyables.size(); i < len; i++) {
                    mViewDestroyables.get(i).onDestroy();
                }
                mViewDestroyables = null;
            }
        } else {
            if (lifecycle != mContainerLifecycle)
                throw new IllegalArgumentException("Invalid Lifecycle");
            if (mContainerDestroyables != null) {
                for (int i = 0, len = mContainerDestroyables.size(); i < len; i++) {
                    mContainerDestroyables.get(i).onDestroy();
                }
                mContainerDestroyables = null;
            }
        }
    }

}
