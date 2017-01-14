package nuclei.persistence.adapter.databinding;

import android.databinding.ViewDataBinding;
import android.view.View;

import nuclei.persistence.adapter.PersistenceAdapter;

public class SimpleViewHolder<T, V extends ViewDataBinding> extends PersistenceAdapter.ViewHolder<T> {

    final V binding;
    final Binder<T, V> binder;

    public SimpleViewHolder(View loadingView) {
        super(loadingView);
        this.binder = null;
        this.binding = null;
    }

    public SimpleViewHolder(Binder<T, V> binder, V binding) {
        super(binding.getRoot());
        this.binder = binder;
        this.binding = binding;
    }

    @Override
    public void onBind() {
        if (binder != null)
            binder.onBind(item, binding);
        if (binding != null)
            binding.executePendingBindings();
    }

}