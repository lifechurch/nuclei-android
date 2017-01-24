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

import android.content.ContentValues;

public class UpdateQueryBuilder<T> extends QueryBuilder<T> {

    UpdateQueryBuilder(Query<T> query) {
        super(query);
    }

    public int update(QueryArgs args, T object) {
        if (query.opType != Query.QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Query is not an update");
        args(args);
        if (query.placeholders == 0)
            return query.update(object, EMPTY);
        else
            return query.update(object, argVals);
    }

    public int update(QueryArgs args, ContentValues values) {
        if (query.opType != Query.QUERY_OPERATION_UPDATE)
            throw new IllegalArgumentException("Query is not an update");
        args(args);
        if (query.placeholders == 0)
            return query.update(values, EMPTY);
        else
            return query.update(values, argVals);
    }

    public int delete(QueryArgs args) {
        if (query.opType != Query.QUERY_OPERATION_DELETE)
            throw new IllegalArgumentException("Query is not a delete");
        args(args);
        if (query.placeholders == 0)
            return query.delete(EMPTY);
        else
            return query.delete(argVals);
    }

}
