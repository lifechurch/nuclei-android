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
package nuclei.persistence.adapter;

import android.content.Context;

import nuclei.persistence.PersistenceList;
import nuclei.persistence.Query;

/**
 * A simple persistence adapter
 *
 * @param <T>
 * @param <VH>
 */
public abstract class PersistenceAdapter<T, VH extends ListAdapter.ViewHolder<T>>
        extends ListAdapter<T, PersistenceList<T>, VH>
        implements PersistenceListAdapter<T> {

    Query<T> mQuery;

    public PersistenceAdapter(Context context) {
        super(context);
    }

    @Override
    public void notifyListSizeChanged() {
    }

    @Override
    public void setList(PersistenceList<T> list) {
        mQuery = list.getQuery();
        super.setList(list);
    }

    protected void onRecycleItem(T item) {
        if (mList != null)
            mList.recycle(mQuery, item);
    }

    @Override
    public void onViewRecycled(VH holder) {
        if (holder.item != null)
            onRecycleItem(holder.item);
    }

}
