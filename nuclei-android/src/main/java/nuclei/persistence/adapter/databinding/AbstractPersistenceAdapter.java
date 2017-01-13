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
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import nuclei.persistence.adapter.ListAdapter;
import nuclei.persistence.adapter.PersistenceAdapter;

public abstract class AbstractPersistenceAdapter<T, V extends ViewDataBinding> extends PersistenceAdapter<T, AbstractPersistenceAdapter.SimpleViewHolder<T, V>> {

    private final Binder<T, V> mBinder;

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
    protected AbstractPersistenceAdapter.SimpleViewHolder<T, V> onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return new AbstractPersistenceAdapter.SimpleViewHolder<T, V>(mBinder, mBinder.newBinding(inflater, parent, viewType));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBinder != null)
            mBinder.onDestroy();
    }

    static class SimpleViewHolder<T, V extends ViewDataBinding> extends ListAdapter.ViewHolder<T> {

        final V binding;
        final Binder<T, V> binder;

        SimpleViewHolder(Binder<T, V> binder, V binding) {
            super(binding.getRoot());
            this.binder = binder;
            this.binding = binding;
        }

        @Override
        public void onBind() {
            binder.onBind(item, binding);
            binding.executePendingBindings();
        }

    }

    public static abstract class AbstractBinder<T, V extends ViewDataBinding> implements Binder<T, V> {

        private final int mLayoutId;

        protected AbstractBinder(int layoutId) {
            mLayoutId = layoutId;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public long getId(T item) {
            return RecyclerView.NO_ID;
        }

        public abstract void onBind(T item, V binding);

        @Override
        public V newBinding(LayoutInflater inflater, ViewGroup parent, int viewType) {
            return DataBindingUtil.inflate(inflater, mLayoutId, parent, false);
        }

        @Override
        public void onDestroy() {
        }

    }

}
