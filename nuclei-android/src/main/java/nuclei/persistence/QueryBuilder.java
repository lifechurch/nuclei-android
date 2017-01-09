package nuclei.persistence;

import android.content.Context;

import java.util.Date;

abstract class QueryBuilder<T> {

    final Context context;
    final Query<T> query;
    final String[] args;
    String orderBy;
    private int argsSet;

    QueryBuilder(Context context, Query<T> query) {
        this.context = context;
        this.query = query;
        this.orderBy = query.orderBy;
        if (query.placeholders > 0)
            this.args = new String[query.placeholders];
        else
            this.args = null;
    }

    public QueryBuilder<T> orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    private QueryBuilder<T> add(String arg) {
        if (args == null)
            throw new IllegalArgumentException("This query doesn't have any selection args");
        args[argsSet++] = arg;
        return this;
    }

    public QueryBuilder<T> arg(boolean x) {
        return add(Boolean.toString(x));
    }

    public QueryBuilder<T> arg(Boolean x) {
        return add(x == null ? null : x.toString());
    }

    public QueryBuilder<T> arg(int x) {
        return add(Integer.toString(x));
    }

    public QueryBuilder<T> arg(Integer x) {
        return add(x == null ? null : x.toString());
    }

    public QueryBuilder<T> arg(long x) {
        return add(Long.toString(x));
    }

    public QueryBuilder<T> arg(Long x) {
        return add(x == null ? null : x.toString());
    }

    public QueryBuilder<T> arg(float x) {
        return add(Float.toString(x));
    }

    public QueryBuilder<T> arg(Float x) {
        return add(x == null ? null : x.toString());
    }

    public QueryBuilder<T> arg(double x) {
        return add(Double.toString(x));
    }

    public QueryBuilder<T> arg(Double x) {
        return add(x == null ? null : x.toString());
    }

    public QueryBuilder<T> arg(String x) {
        return add(x);
    }

    public QueryBuilder<T> arg(Date x) {
        return add(x == null ? null : Long.toString(x.getTime()));
    }

    void validate() {
        if (args != null && query.placeholders != args.length) {
            throw new IllegalArgumentException("Invalid selection args");
        }
    }

}
