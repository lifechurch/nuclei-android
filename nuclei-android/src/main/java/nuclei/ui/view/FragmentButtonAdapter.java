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
package nuclei.ui.view;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public abstract class FragmentButtonAdapter extends ButtonAdapter implements ButtonBarView.OnItemSelectedListener {

    private final FragmentManager mFragmentManager;
    private final int mContainerViewId;

    public FragmentButtonAdapter(FragmentManager fragmentManager, int containerViewId) {
        mFragmentManager = fragmentManager;
        mContainerViewId = containerViewId;
    }

    public abstract Fragment getFragment(int position);

    @Override
    public void onSelected(int position, boolean changed) {
        if (changed)
            mFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(mContainerViewId, getFragment(position))
                .commitNow();
    }

}
