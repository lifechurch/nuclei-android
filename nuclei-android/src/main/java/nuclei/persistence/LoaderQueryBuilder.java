package nuclei.persistence;

public class LoaderQueryBuilder<T> extends QueryBuilder<T> {

    private PersistenceLoader loader;
    private PersistenceList.Listener<T> listener;

    public LoaderQueryBuilder(Query<T> query, PersistenceLoader loader, PersistenceList.Listener<T> listener) {
        super(query);
        this.loader = loader;
        this.listener = listener;
    }

    public int execute() {
        return execute(null);
    }

    public int execute(QueryArgs args) {
        if (loader == null)
            throw new IllegalStateException("This builder has already been used");
        args(args);
        try {
            if (orderBy != null) {
                if (query.placeholders == 0)
                    return loader.executeWithOrder(query, listener, orderBy);
                else
                    return loader.executeWithOrder(query, listener, orderBy, argVals);
            } else {
                if (query.placeholders == 0)
                    return loader.execute(query, listener);
                else
                    return loader.execute(query, listener, argVals);
            }
        } finally {
            loader = null;
            listener = null;
        }
    }

}
