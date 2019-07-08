package nuclei.persistence.adapter.databinding;

import androidx.databinding.ViewDataBinding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class AbstractPagingBinder<T, V extends ViewDataBinding> extends AbstractBinder<T, V> implements PagingBinder<T, V> {

    private final int mLoadingLayoutId;

    public AbstractPagingBinder(int layoutId, int loadingLayoutId) {
        super(layoutId);
        mLoadingLayoutId = loadingLayoutId;
    }

    @Override
    public View newLoadingBinding(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return inflater.inflate(mLoadingLayoutId, parent, false);
    }

    @Override
    public void onLoadingBind(View view) {

    }

}
