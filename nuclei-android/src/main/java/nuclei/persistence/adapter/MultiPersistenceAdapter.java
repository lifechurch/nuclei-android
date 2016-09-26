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
import android.view.View;

import nuclei.persistence.MultiPersistenceListImpl;
import nuclei.persistence.PersistenceList;
import nuclei.persistence.Query;

/**
 * A simple persistence adapter
 *
 * @param <T>
 * @param <VH>
 */
public abstract class MultiPersistenceAdapter<T, VH extends MultiPersistenceAdapter.MultiViewHolder<T>>
        extends ListAdapter<T, PersistenceList<T>, VH>
        implements PersistenceListAdapter<T> {

    MultiPersistenceListImpl<T> mMultiList;

    public MultiPersistenceAdapter(Context context, Query<T>...queries) {
        super(context);
        mMultiList = new MultiPersistenceListImpl<T>(queries);
        super.setList(mMultiList);
    }

    protected MultiPersistenceListImpl.QueryList<T> getList(int position) {
        return mMultiList.getList(position);
    }

    @Override
    public void setList(PersistenceList<T> list) {
        mMultiList.setList(list.getQuery(), list);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        MultiPersistenceListImpl.QueryList<T> list = mMultiList.getList(position);
        holder.item = list.get(position);
        holder.query = list.query;
        holder.itemSize = list.size();
        holder.offset = list.offset();
        holder.onBind();
    }

    protected void onRecycleItem(Query<T> query, T item) {
        if (mList != null)
            mList.recycle(query, item);
    }

    @Override
    public void onViewRecycled(VH holder) {
        if (holder.item != null)
            onRecycleItem(holder.query, holder.item);
    }

    public static class MultiViewHolder<T> extends ListAdapter.ViewHolder<T> {

        protected Query<T> query;
        protected int itemSize;
        protected int offset;

        public MultiViewHolder(View view) {
            super(view);
        }

        public Query<T> getQuery() {
            return query;
        }

        public int getOffset() {
            return offset;
        }

        public int getItemSize() {
            return itemSize;
        }
    }

}
