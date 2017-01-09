package nuclei.persistence;

import android.content.Context;
import android.database.Cursor;

public class SupportSelectQueryBuilder<T> extends QueryBuilder<T> {

    SupportSelectQueryBuilder(Context context, Query<T> query) {
        super(context, query);
    }

    public Cursor execute() {
        validate();
        return query.execute(context.getContentResolver(), args, orderBy);
    }

    public android.support.v4.content.CursorLoader executeLoader() {
        validate();
        return query.executeSupportLoader(context, args, orderBy);
    }

}
