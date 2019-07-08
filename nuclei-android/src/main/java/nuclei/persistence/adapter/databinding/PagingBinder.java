package nuclei.persistence.adapter.databinding;

import androidx.databinding.ViewDataBinding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public interface PagingBinder<T, V extends ViewDataBinding> extends Binder<T, V> {

    View newLoadingBinding(LayoutInflater inflater, ViewGroup parent, int viewType);
    void onLoadingBind(View view);

}
