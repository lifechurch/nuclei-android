package nuclei.persistence;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;

@TargetApi(11)
public class SelectQueryBuilder<T> extends QueryBuilder<T> {

    SelectQueryBuilder(Context context, Query<T> query) {
        super(context, query);
    }

    public Cursor execute() {
        validate();
        return query.execute(context.getContentResolver(), args, orderBy);
    }

    public android.content.CursorLoader executeLoader() {
        validate();
        return query.executeLoader(context, args, orderBy);
    }

}
