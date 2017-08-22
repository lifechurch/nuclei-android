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
package nuclei3.persistence.db;

import java.util.ArrayList;
import java.util.List;

public class Query {

    private EntityModel entityModel;
    private String name;
    private String selection;
    private String orderBy;
    private List<EntityProperty> properties;
    private int placeholders;

    public Query(String name, String selection, String orderBy, EntityModel model, List<EntityProperty> properties) {
        this.name = name;
        this.selection = selection;
        this.orderBy = orderBy;
        if (properties != null)
            this.properties = new ArrayList<>(properties);
        this.entityModel = model;

        if (selection != null)
            for (char c : selection.toCharArray()) {
                if (c == '?')
                    placeholders++;
            }
    }

    public EntityModel getModel() {
        return entityModel;
    }

    public int getPlaceholders() {
        return placeholders;
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
        if (properties == null)
            return entityModel.getAllProperties(entityModel.currentVersion().getVersion());
        return properties;
    }

    public boolean hasProperties() {
        return properties != null && properties.size() > 0;
    }

    public void setProperties(List<EntityProperty> properties) {
        this.properties = properties;
    }

    public Query clone() {
        return new Query(name, selection, orderBy, entityModel, properties);
    }

}
