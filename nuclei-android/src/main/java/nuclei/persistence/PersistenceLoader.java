/**
 * Copyright 2017 YouVersion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.persistence;

public interface PersistenceLoader {

    <T> LoaderQueryBuilder<T> newLoaderBuilder(Query<T> query, PersistenceList.Listener<T> listener);

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
