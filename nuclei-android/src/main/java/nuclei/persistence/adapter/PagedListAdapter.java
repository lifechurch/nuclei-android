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

public abstract class PagedListAdapter<T, VH extends ListAdapter.ViewHolder<T>>
        extends PagingAdapter<T, PagedList<T>, VH>
        implements PagedList.OnPageListener {

    public PagedListAdapter(Context context) {
        super(context);
    }

    @Override
    public void setList(PagedList<T> list) {
        PagedList<T> prev = getList();
        if (prev != null)
            prev.setListener(null);
        list.setListener(this);
        super.setList(list);
    }

    @Override
    protected int getPageSize(PagedList<T> list) {
        if (list == null)
            return 0;
        return list.mPageSize;
    }

    @Override
    protected int getPrevLoadingPosition(PagedList<T> list) {
        if (list == null)
            return 0;
        return list.mMinPageIndex * list.mPageSize;
    }

    @Override
    protected int getNextLoadingPosition(PagedList<T> list) {
        if (list == null)
            return 0;
        return list.mMaxPageIndex * list.mPageSize;
    }

    @Override
    public void onPageLoading(int pageIndex) {

    }

    @Override
    public void onPageLoaded(int pageIndex) {
        PagedList<T> list = getList();
        if (list == null)
            onLoadComplete(false, false, true);
        else
            onLoadComplete(list.mMore, !list.isPaging(), true);
    }

    @Override
    public void onPageEvicted(int pageIndex) {

    }

    @Override
    public void onPageFailed(int pageIndex, Exception err) {
        onLoadComplete(false, false, true);
    }

    @Override
    protected void onLoadPage(int pageIndex, int pageSize) {
        PagedList<T> list = getList();
        if (list != null)
            list.ensurePage(pageIndex);
    }

    @Override
    public void onPagesCleared() {
        onLoadComplete(true, true, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PagedList<T> list = getList();
        if (list != null)
            list.setListener(null);
    }

}
