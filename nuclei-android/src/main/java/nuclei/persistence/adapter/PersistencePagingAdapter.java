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
import android.os.Bundle;
import android.support.annotation.Nullable;

import nuclei.persistence.PersistenceList;
import nuclei.persistence.Query;

/**
 * A simple paged persistence adapter (for requesting more things be loaded into the table).
 *
 * @param <T>
 * @param <VH>
 */
public abstract class PersistencePagingAdapter<T, VH extends PersistenceAdapter.ViewHolder<T>>
        extends PagingAdapter<T, PersistenceList<T>, VH>
        implements PersistenceListAdapter<T> {

    Query<T> mQuery;

    private int mPageSize = -1;

    public PersistencePagingAdapter(Context context) {
        super(context);
    }

    public void setPageSize(int pageSize) {
        mPageSize = pageSize;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pageSize", mPageSize);
        outState.putInt("nextPagePos", mNextPageIndex);
    }

    @Override
    protected int getPageSize(PersistenceList<T> list) {
        return mPageSize;
    }

    public void reset() {
        mNextPageIndex = 0;
        mLastPageIndex = -1;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mPageSize = savedInstanceState.getInt("pageSize");
            mNextPageIndex = savedInstanceState.getInt("nextPagePos");
        }
    }

    @Override
    protected void onLoadMore(int position) {
        super.onLoadMore(position);
        if (mHasMore && mPageSize != -1 && position >= mNextPageIndex) {
            final int nextPage = ((position + 1) / mPageSize);
            final int lastPage = mLastPageIndex == -1 ? 0 : mLastPageIndex + 1;
            for (int p = lastPage; p <= nextPage; p++) {
                loadPage(p);
            }
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        super.onBindViewHolder(holder, position);
        onLoadMore(position);
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
