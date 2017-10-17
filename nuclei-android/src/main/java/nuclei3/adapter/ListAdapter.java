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
package nuclei3.adapter;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Base List Adapter
 *
 * @param <T> The type for the adapter
 * @param <L> The type of List
 * @param <VH> The type of ViewHolder
 */
public abstract class ListAdapter<T, L extends List<T>, VH extends ListAdapter.ViewHolder<T>>
        extends RecyclerView.Adapter<VH> implements LifecycleObserver {

    WeakReference<Context> mContext;
    WeakReference<LayoutInflater> mInflater;
    L mList;
    long mListUpdates;

    public ListAdapter(Context context) {
        mContext = new WeakReference<>(context);
        mInflater = new WeakReference<>(LayoutInflater.from(context));
    }

    protected Context getContext() {
        return mContext.get();
    }

    /**
     * Get the number of times setList has been called.
     * @return The # of times set list is called
     */
    public long getListUpdates() {
        return mListUpdates;
    }

    public L getList() {
        return mList;
    }

    public void setList(L list) {
        mList = list;
        mListUpdates++;
        notifyDataSetChanged();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = mInflater.get();
        if (inflater == null) {
            Context context = mContext.get();
            if (context != null) {
                inflater = LayoutInflater.from(context);
                mInflater = new WeakReference<>(inflater);
            }
        }
        return onCreateViewHolder(inflater, parent, viewType);
    }

    protected abstract VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType);

    public T getItem(int position) {
        if (mList == null || position < 0)
            return null;
        return mList.get(position);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.item = getItem(position);
        holder.onBind();
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    public static class ViewHolder<T> extends RecyclerView.ViewHolder {

        public T item;

        public ViewHolder(View view) {
            super(view);
        }

        public T getItem() {
            return item;
        }

        public void onBind() {
        }

    }

}
