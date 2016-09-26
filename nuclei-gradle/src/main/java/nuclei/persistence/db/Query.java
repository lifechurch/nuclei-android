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
package nuclei.persistence.db;

import java.util.ArrayList;
import java.util.List;

public class Query {

    private String name;
    private String selection;
    private String orderBy;
    private List<EntityProperty> properties;

    public Query(String name, String selection, String orderBy, List<EntityProperty> properties) {
        this.name = name;
        this.selection = selection;
        this.orderBy = orderBy;
        if (properties != null)
            this.properties = new ArrayList<>(properties);
        else
            this.properties = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getUpperCaseName() {
        return name.toUpperCase();
    }

    public String getSelection() {
        return selection;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void addProperty(EntityProperty property) {
        this.properties.add(property);
    }

    public void addAllProperties(List<EntityProperty> properties) {
        this.properties.addAll(properties);
    }

    public List<EntityProperty> getProperties() {
        return properties;
    }

}
