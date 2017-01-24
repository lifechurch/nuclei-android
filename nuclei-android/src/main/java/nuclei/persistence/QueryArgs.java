/**
 * Copyright 2016 YouVersion
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class QueryArgs {

    private final ArrayList<String> args;
    String orderBy;

    QueryArgs() {
        args = new ArrayList<>();
    }

    void validate(QueryBuilder builder) {
        validate(builder.query);
    }

    void validate(Query query) {
        if (query.placeholders != args.size()) {
            throw new IllegalArgumentException("Invalid number of selection args (" + args.size() + " != " + query.placeholders + ")");
        }
    }

    private QueryArgs add(String arg) {
        args.add(arg);
        return this;
    }

    public QueryArgs orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public QueryArgs arg(boolean x) {
        return add(Boolean.toString(x));
    }

    public QueryArgs arg(Boolean x) {
        return add(x == null ? null : x.toString());
    }

    public QueryArgs arg(int x) {
        return add(Integer.toString(x));
    }

    public QueryArgs arg(Integer x) {
        return add(x == null ? null : x.toString());
    }

    public QueryArgs arg(long x) {
        return add(Long.toString(x));
    }

    public QueryArgs arg(Long x) {
        return add(x == null ? null : x.toString());
    }

    public QueryArgs arg(float x) {
        return add(Float.toString(x));
    }

    public QueryArgs arg(Float x) {
        return add(x == null ? null : x.toString());
    }

    public QueryArgs arg(double x) {
        return add(Double.toString(x));
    }

    public QueryArgs arg(Double x) {
        return add(x == null ? null : x.toString());
    }

    public QueryArgs arg(String x) {
        return add(x);
    }

    public QueryArgs arg(Date x) {
        return add(x == null ? null : Long.toString(x.getTime()));
    }

    @Deprecated
    QueryArgs args(String[] args) {
        this.args.addAll(Arrays.asList(args));
        return this;
    }

    String[] args() {
        return args.toArray(new String[args.size()]);
    }

}
