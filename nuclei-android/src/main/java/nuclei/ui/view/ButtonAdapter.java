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

import android.database.DataSetObserver;

public abstract class ButtonAdapter {

    private DataSetObserver mViewObserver;

    public abstract int getTitle(int position);
    public abstract int getDrawable(int position);
    public abstract int getCount();

    public void notifyDataSetChanged() {
        synchronized (this) {
            if (mViewObserver != null)
                mViewObserver.onChanged();
        }
    }

    public void setViewObserver(DataSetObserver viewObserver) {
        synchronized (this) {
            mViewObserver = viewObserver;
        }
    }

}
