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
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

/**
 * A base adapter to help with paging items
 *
 * @param <T>
 * @param <L>
 * @param <VH>
 */
public abstract class PagingAdapter<T, L extends List<T>, VH extends PersistenceAdapter.ViewHolder<T>>
        extends ListAdapter<T, L, VH> {

    private static final String STATE_LOADING = PagingAdapter.class.getSimpleName() + ".LOADING";
    private static final String STATE_HAS_MORE = PagingAdapter.class.getSimpleName() + ".HAS_MORE";
    private static final String STATE_READY = PagingAdapter.class.getSimpleName() + ".READY";
    private static final String STATE_LAST_INDEX = PagingAdapter.class.getSimpleName() + ".LAST_INDEX";
    private static final String STATE_NEXT_INDEX = PagingAdapter.class.getSimpleName() + ".NEXT_INDEX";
    private static final String STATE_PAGE_SIZE = PagingAdapter.class.getSimpleName() + ".PAGE_SIZE";
    private static final String STATE_PREV_LOADING = PagingAdapter.class.getSimpleName() + ".PREV_LOADING";
    private static final String STATE_NEXT_LOADING = PagingAdapter.class.getSimpleName() + ".NEXT_LOADING";

    private boolean mLoading;
    boolean mHasMore = true;
    private boolean mReady;
    private boolean mSaveState = false;

    int mLastPageIndex = -1; // the last page index that was loaded
    int mNextPageIndex;
    long mPagedListUpdates;
    private int mPageSize;
    private int mPrevLoadingIndex; // the first item index that will be loading when traversing backwards
    private int mNextLoadingIndex; // the first item index that will be loading when traversing forwards

    final int mMoreViewType;
    final long mMoreId;

    public PagingAdapter(Context context) {
        super(context);
        mMoreViewType = getMoreViewType();
        mMoreId = getMoreItemId();
    }

    protected final int getNextPageIndex() {
        return mNextPageIndex;
    }

    protected final int getLastPageIndex() {
        return mLastPageIndex;
    }

    protected void setSaveState(boolean saveState) {
        mSaveState = saveState;
    }

    public void onSaveInstanceState(Bundle outState) {
        if (mSaveState) {
            outState.putBoolean(STATE_LOADING, mLoading);
            outState.putBoolean(STATE_HAS_MORE, mHasMore);
            outState.putBoolean(STATE_READY, mReady);
            outState.putInt(STATE_LAST_INDEX, mLastPageIndex);
            outState.putInt(STATE_NEXT_INDEX, mNextLoadingIndex);
            outState.putInt(STATE_PAGE_SIZE, mPageSize);
            outState.putInt(STATE_PREV_LOADING, mPrevLoadingIndex);
            outState.putInt(STATE_NEXT_LOADING, mNextLoadingIndex);
        }
    }

    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null && mSaveState) {
            mLoading = savedInstanceState.getBoolean(STATE_LOADING);
            mHasMore = savedInstanceState.getBoolean(STATE_HAS_MORE);
            mReady = savedInstanceState.getBoolean(STATE_READY);
            mLastPageIndex = savedInstanceState.getInt(STATE_LAST_INDEX);
            mNextPageIndex = savedInstanceState.getInt(STATE_NEXT_INDEX);
            mPageSize = savedInstanceState.getInt(STATE_PAGE_SIZE);
            mPrevLoadingIndex = savedInstanceState.getInt(STATE_PREV_LOADING);
            mNextLoadingIndex = savedInstanceState.getInt(STATE_NEXT_LOADING);
        }
    }

    protected int getMoreViewType() {
        return Integer.MAX_VALUE;
    }

    protected long getMoreItemId() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getItemId(int position) {
        if (mHasMore && position == super.getItemCount())
            return mMoreId;
        return super.getItemId(position);
    }

    /**
     * Does the adapter have more to load
     *
     * @return
     */
    public boolean isHasMore() {
        return mHasMore;
    }

    /**
     * Is the adapter ready to load more
     *
     * @return
     */
    public boolean isReady() {
        return mReady;
    }

    /**
     * Inform the adapter that there are more things to be loaded
     *
     * @param hasMore
     */
    public void setHasMore(boolean hasMore) {
        mHasMore = hasMore;
        notifyDataSetChanged();
    }

    /**
     * Get the previous item index that will need to be loaded when traversing backwards
     *
     * @param list
     * @return
     */
    protected int getPrevLoadingIndex(L list) {
        if (list == null || (mHasMore && list.size() == 0))
            return 0;
        return -1;
    }

    /**
     * Get the next item index that will need to be loaded when traversing forwards
     *
     * @param list
     * @return
     */
    protected int getNextLoadingIndex(L list) {
        if (list == null)
            return 0;
        return list.size();
    }

    /**
     * Get the page size
     *
     * @param list
     * @return
     */
    protected int getPageSize(L list) {
        return -1;
    }

    public void setList(L list) {
        mReady = true;
        mPageSize = getPageSize(list);
        mPrevLoadingIndex = getPrevLoadingIndex(list);
        mNextLoadingIndex = getNextLoadingIndex(list);
        super.setList(list);
    }

    @Override
    public int getItemViewType(int position) {
        if (mHasMore) {
            if (position == mPrevLoadingIndex || position == mNextLoadingIndex)
                return mMoreViewType;
            if (mPrevLoadingIndex > 0)
                position--;
        }
        return super.getItemViewType(position);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == mMoreViewType)
            return onCreateMoreViewHolder(mInflater, parent, viewType);
        return onCreateViewHolder(mInflater, parent, viewType);
    }

    protected abstract VH onCreateMoreViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType);

    public T getItem(int position) {
        if (mHasMore) {
            if (position == mPrevLoadingIndex || position == mNextLoadingIndex)
                return null;
            if (mPrevLoadingIndex > 0)
                position--;
        }
        return super.getItem(position);
    }

    protected void onLoadMore(int position) {
        if (!mLoading && mReady && mHasMore) {
            if (mPageSize > 0) {
                if (position == mPrevLoadingIndex)
                    loadPage(mPrevLoadingIndex / mPageSize);
                else
                    loadPage(mNextLoadingIndex / mPageSize);
            } else {
                if (position == mPrevLoadingIndex)
                    loadPage(Math.max(0, mLastPageIndex - 1));
                else
                    loadPage(mLastPageIndex + 1);
            }
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        if (holder.getItemViewType() == mMoreViewType) {
            onLoadMore(position);
        } else {
            holder.item = getItem(position);
            holder.onBind();
        }
    }

    protected void setReady(boolean ready) {
        mReady = ready;
        notifyDataSetChanged();
    }

    public final void loadPage(int pageIndex) {
        if (getList() == null)
            return;
        mLoading = true;
        mReady = false;
        mNextPageIndex = ((pageIndex + 1) * mPageSize) - 1;
        mLastPageIndex = pageIndex;
        mPagedListUpdates = getListUpdates();
        onLoadPage(pageIndex, mPageSize);
    }

    protected abstract void onLoadPage(int pageIndex, int pageSize);

    public void onLoadComplete(boolean hasMore, boolean ready) {
        onLoadComplete(hasMore, ready, mPagedListUpdates != getListUpdates());
    }

    public void onLoadComplete(boolean hasMore, boolean ready, boolean notifyChanged) {
        mLoading = false;
        mHasMore = hasMore;
        mReady = ready;
        mPageSize = getPageSize(mList);
        mPrevLoadingIndex = getPrevLoadingIndex(mList);
        mNextLoadingIndex = getNextLoadingIndex(mList);
        if (notifyChanged)
            notifyDataSetChanged();
    }

    protected int getActualCount() {
        L list = getList();
        if (list == null)
            return 0;
        return list.size();
    }

    @Override
    public int getItemCount() {
        int count = super.getItemCount();
        if (mHasMore) {
            if (mPrevLoadingIndex > 0)
                count++;
            if (mNextLoadingIndex == count)
                count++;
        }
        return count;
    }

}
