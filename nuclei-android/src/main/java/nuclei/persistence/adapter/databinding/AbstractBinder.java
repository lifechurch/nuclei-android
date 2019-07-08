package nuclei.persistence.adapter.databinding;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public abstract class AbstractBinder<T, V extends ViewDataBinding> implements Binder<T, V> {

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