package nuclei.persistence;

import android.content.Context;

public class LoaderQueryBuilder<T> extends QueryBuilder<T> {

    private PersistenceLoader loader;
    private PersistenceList.Listener<T> listener;

    public LoaderQueryBuilder(Context context, Query<T> query, PersistenceLoader loader, PersistenceList.Listener<T> listener) {
        super(context, query);
        this.loader = loader;
        this.listener = listener;
    }

    public int execute() {
        if (loader == null)
            throw new IllegalStateException("This builder has already been used");
        validate();
        try {
            if (orderBy != null) {
                if (query.placeholders == 0)
                    return loader.executeWithOrder(query, listener, orderBy);
                else
                    return loader.executeWithOrder(query, listener, orderBy, args);
            } else {
                if (query.placeholders == 0)
                    return loader.execute(query, listener);
                else
                    return loader.execute(query, listener, args);
            }
        } finally {
            loader = null;
            listener = null;
        }
    }

}
