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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.collection.LruCache;

import java.util.AbstractList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A list that manages a set of in-memory pages and the loading
 * of those pages.  It also maintains some meta information used
 * by the PagedListAdapter to determine where to place loading indicators.
 *
 * @param <T>
 */
public abstract class PagedList<T> extends AbstractList<T> {

    final Context mContext;
    Handler mHandler;
    final LruCache<Integer, List<T>> mPages;
    int mPageSize;
    int mSize;
    OnPageListener mListener;
    final Set<Integer> mLoading = new HashSet<>();
    int mMinPageIndex = -1;
    int mMaxPageIndex = 0;
    final int mMaxPages;
    boolean mMore;
    int mCurPageIndex;

    public PagedList(Context context, int maxPages, int pageSize) {
        mContext = context.getApplicationContext();
        mMaxPages = maxPages;
        mPages = new LruCache<Integer, List<T>>(maxPages) {
            @Override
            protected void entryRemoved(boolean evicted, Integer key, List<T> oldValue, List<T> newValue) {
                if (evicted)
                    calculatePageIndexes();
                if (mListener != null)
                    mListener.onPageEvicted(key);
            }
        };
        mPageSize = pageSize;
    }

    @Override
    public void clear() {
        mPages.evictAll();
        mMinPageIndex = -1;
        mMaxPageIndex = 0;
        mSize = 0;
        if (mListener != null)
            mListener.onPagesCleared();
    }

    /**
     * Get the page size
     *
     * @return
     */
    protected int getPageSize() {
        return mPageSize;
    }

    /**
     * It's best to only change this once (for instance, on the first page load).
     *
     * @param pageSize
     */
    protected void setPageSize(int pageSize) {
        mPageSize = pageSize;
    }

    /**
     * Check if there are pages that are being loaded
     *
     * @return
     */
    public boolean isPaging() {
        return mLoading.size() > 0;
    }

    /**
     * Check if a page is in memory
     *
     * @param page
     * @return
     */
    public boolean isPageLoaded(int page) {
        return mPages.get(page) != null;
    }

    /**
     * The first page that will need loaded when traversing backwards
     *
     * @return
     */
    public int getMinPageIndex() {
        return mMinPageIndex;
    }

    /**
     * The first page that will need to be loaded with traversing forwards
     *
     * @return
     */
    public int getMaxPageIndex() {
        return mMaxPageIndex;
    }

    /**
     * Listen for page events.  This is mostly for the purposes of the PagedListAdapter, so
     * that it can bind to this and be notified when to update the UI.
     *
     * @param listener
     */
    public void setListener(OnPageListener listener) {
        mListener = listener;
    }

    @Override
    public T get(int location) {
        if (mPageSize == 0)
            throw new ArrayIndexOutOfBoundsException("Page size is zero");
        int pageIndex = location / mPageSize;
        Integer ix = pageIndex;
        List<T> page = mPages.get(ix);
        if (page == null) {
            if (!mLoading.contains(ix)) {
                loadPage(pageIndex);
            }
            return null;
        }
        int itemIndex = location % mPageSize;
        return page.get(itemIndex);
    }

    /**
     * Can more pages be loaded
     *
     * @return
     */
    public boolean isMoreAvailable() {
        return mMore;
    }

    @Override
    public int size() {
        return mSize;
    }

    private void calculatePageIndexes() {
        int p = mMaxPages / 2;

        mMinPageIndex = mCurPageIndex - p;
        mMaxPageIndex = mCurPageIndex + p;

        if (mMinPageIndex < 0) {
            mMaxPageIndex += Math.abs(mMinPageIndex);
            mMinPageIndex = 0;
        }

        for (int i = mMinPageIndex - 1; i <= mCurPageIndex; i++) {
            if (mPages.get(i) == null) {
                mMinPageIndex = i;
                break;
            }
        }

        for (int i = mMaxPageIndex + 1; i >= mCurPageIndex; i--) {
            if (mPages.get(i) == null)
                mMaxPageIndex = i;
        }
    }

    /**
     * Load a page and recompute the meta data.
     *
     * @param pageIndex
     */
    private void loadPage(int pageIndex) {
        mLoading.add(pageIndex);

        if (mListener != null)
            mListener.onPageLoading(pageIndex);

        mCurPageIndex = pageIndex;
        calculatePageIndexes();

        onLoadPage(mContext, pageIndex);
    }

    /**
     * Ensure a page is in memory (or loading).
     *
     * It's best to do these with contiguous indexes.  As, the meta information
     * maintained expects that the min/max pages are contiguous.
     *
     * So, random requested pages will possibly do bad things to the UI if this is used with a
     * PagedListAdapter.
     *
     * @param pageIndex The index to ensure
     */
    public void ensurePage(final int pageIndex) {
        Integer ix = pageIndex;
        if (mPages.get(ix) == null) {
            if (!mLoading.contains(ix))
                loadPage(pageIndex);
        } else {
            if (mListener != null) {
                if (mHandler == null)
                    mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onPageLoaded(pageIndex);
                    }
                });
            }
        }
    }

    /**
     * Load a page
     *
     * @param pageIndex The page to load
     */
    protected abstract void onLoadPage(Context context, int pageIndex);

    /**
     * Notify the PagedList that a new page is ready.
     *
     * @param pageIndex The index that was loaded
     * @param page The page for the index
     * @param size The total size of the list
     * @param more Are there more pages available to traverse
     */
    protected void onPageLoaded(int pageIndex, List<T> page, int size, boolean more) {
        mPages.put(pageIndex, page);
        mLoading.remove(pageIndex);
        mSize = size;
        mMore = more;
        mCurPageIndex = pageIndex;
        calculatePageIndexes();
        if (mListener != null)
            mListener.onPageLoaded(pageIndex);
    }

    /**
     * Notify the PagedList that a new page failed to load.
     *
     * @param pageIndex The index that failed
     * @param err The exception
     */
    protected void onPageFailed(int pageIndex, Exception err) {
        mLoading.remove(pageIndex);
        if (mListener != null)
            mListener.onPageFailed(pageIndex, err);
    }

    /**
     * This will force an eviction of all memory pages
     */
    public void onLowMemory() {
        mPages.evictAll();
    }

    public interface OnPageListener {

        /**
         * A page has started loading
         *
         * @param pageIndex
         */
        void onPageLoading(int pageIndex);

        /**
         * A page finished loading
         *
         * @param pageIndex
         */
        void onPageLoaded(int pageIndex);

        /**
         * A page has evicted from memory
         *
         * @param pageIndex
         */
        void onPageEvicted(int pageIndex);

        /**
         * A page failed to load
         *
         * @param pageIndex
         * @param err
         */
        void onPageFailed(int pageIndex, Exception err);

        /**
         * The list has been cleared
         */
        void onPagesCleared();

    }

}
