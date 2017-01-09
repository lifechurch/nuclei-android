package nuclei.persistence;

import android.content.Context;

public class UpdateQueryBuilder<T> extends QueryBuilder<T> {

    UpdateQueryBuilder(Context context, Query<T> query) {
        super(context, query);
    }

    public int update(T object) {
        validate();
        if (query.placeholders == 0)
            return query.update(context, object);
        else
            return query.update(context, object, args);
    }

    public int delete() {
        validate();
        if (query.placeholders == 0)
            return query.delete(context);
        else
            return query.delete(context, args);
    }

}
