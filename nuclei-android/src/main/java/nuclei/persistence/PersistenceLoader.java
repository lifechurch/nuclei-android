package nuclei.persistence;

public interface PersistenceLoader {

    <T> LoaderQueryBuilder<T> newBuilder(Query<T> query, PersistenceList.Listener<T> listener);

    @Deprecated
    <T> int execute(Query<T> query, PersistenceList.Listener<T> listener, String...selectionArgs);

    @Deprecated
    <T> int executeWithOrder(Query<T> query, PersistenceList.Listener<T> listener, String orderBy, String...selectionArgs);

    @Deprecated
    void reexecute(int id, String...selectionArgs);

    @Deprecated
    void reexecute(int id, Query query, String...selectionArgs);

    void destroyQuery(int id);

    void onDestroy();

}
