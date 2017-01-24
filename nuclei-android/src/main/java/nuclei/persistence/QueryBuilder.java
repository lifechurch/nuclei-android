package nuclei.persistence;

abstract class QueryBuilder<T> {

    static final String[] EMPTY = new String[0];

    final Query<T> query;
    String[] argVals = EMPTY;
    String orderBy;

    QueryBuilder(Query<T> query) {
        this.query = query;
        this.orderBy = query.orderBy;
    }

    void args(QueryArgs args) {
        if (args != null) {
            args.validate(this);
            argVals = args.args();
            if (args.orderBy != null)
                orderBy = args.orderBy;
        } else if (query.placeholders > 0)
            throw new IllegalArgumentException("Invalid number of selection args (0 != " + query.placeholders + ")");
    }

}
