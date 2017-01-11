/**
 * Copyright 2016 YouVersion
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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

import java.util.List;

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
    private boolean mUpdatePageIndex;

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
        mHasMore = true;
    }

    public void notifyListSizeChanged() {
        mUpdatePageIndex = true;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mPageSize = savedInstanceState.getInt("pageSize");
            mNextPageIndex = savedInstanceState.getInt("nextPagePos");
        }
    }

    /**
     * Load new pages based upon last viewed position, as
     * opposed to the last time the loading indicator was viewed (which is what onLoadMore is for).
     * @param position The currently binding view position
     */
    protected void onLoadBind(int position) {
        if (mHasMore && mPageSize != -1) {
            final int curPageIndex = position / mPageSize;
            if (curPageIndex >= mNextPageIndex) {
                LOG.d("onLoadBind");
                for (int p = mLastPageIndex + 1; p <= curPageIndex; p++) {
                    loadPage(p);
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        super.onBindViewHolder(holder, position);
        onLoadBind(position);
    }

    @Override
    public void setList(PersistenceList<T> list) {
        mQuery = list.getQuery();
        if (mUpdatePageIndex) {
            mUpdatePageIndex = false;
            int size = list.size();
            int maxPageIx = size / mPageSize;
            if (mNextPageIndex > maxPageIx) {
                mNextPageIndex = maxPageIx;
                mHasMore = true;
            }
        }
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
