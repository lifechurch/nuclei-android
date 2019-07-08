package nuclei.persistence.adapter.databinding;

import android.content.Context;
import androidx.databinding.ViewDataBinding;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import nuclei.persistence.adapter.PagedListAdapter;

public class AbstractPagedListAdapter<T, V extends ViewDataBinding> extends PagedListAdapter<T, SimpleViewHolder<T, V>> {

    private PagingBinder<T, V> mBinder;

    public AbstractPagedListAdapter(Context context, PagingBinder<T, V> binder) {
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
    protected SimpleViewHolder<T, V> onCreateMoreViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return new SimpleViewHolder<T, V>(mBinder.newLoadingBinding(inflater, parent, viewType));
    }

    @Override
    protected SimpleViewHolder<T, V> onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return new SimpleViewHolder<T, V>(mBinder, mBinder.newBinding(inflater, parent, viewType));
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder<T, V> holder, int position) {
        if (holder.getItemViewType() == getMoreViewType())
            mBinder.onLoadingBind(holder.itemView);
        super.onBindViewHolder(holder, position);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBinder != null)
            mBinder.onDestroy();
        mBinder = null;
    }

}
