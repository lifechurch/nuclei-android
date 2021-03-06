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
package nuclei.persistence.adapter.databinding;

import android.content.Context;
import android.databinding.ViewDataBinding;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import nuclei.persistence.adapter.PersistenceAdapter;

public abstract class AbstractPersistenceAdapter<T, V extends ViewDataBinding> extends PersistenceAdapter<T, SimpleViewHolder<T, V>> {

    private Binder<T, V> mBinder;

    public AbstractPersistenceAdapter(Context context, Binder<T, V> binder) {
        super(context);
        mBinder = binder;
        setHasStableIds(mBinder.hasStableIds());
    }

    @Override
    public long getItemId(int position) {
        T item = getItem(position);
        return mBinder.getId(item);
    }

    @Override
    protected SimpleViewHolder<T, V> onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return new SimpleViewHolder<T, V>(mBinder, mBinder.newBinding(inflater, parent, viewType));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBinder != null)
            mBinder.onDestroy();
        mBinder = null;
    }

}
