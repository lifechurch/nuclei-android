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

public class EntityModelVersion {

    private Version version;
    private EntityModel model;
    private List<EntityProperty> properties;
    private List<EntityIndex> indexes;

    public EntityModelVersion(Version version, EntityModel model) {
        this.version = version;
        this.model = model;
        properties = new ArrayList<>();
        indexes = new ArrayList<>();
    }

    public Version getVersion() {
        return version;
    }

    public List<EntityProperty> getProperties() {
        return properties;
    }

    public List<EntityIndex> getIndexes() {
        return indexes;
    }

    public EntityIndex newIndex(EntityIndex.Type type, String name, List<EntityIndexProperty> properties) {
        EntityIndex index = new EntityIndex();
        index.setModel(model);
        index.setType(type);
        index.setName(name);
        index.setProperties(properties);
        indexes.add(index);
        return index;
    }

    public EntityProperty newProperty(EntityProperty.Type type, String name, boolean nullable) {
        EntityProperty property = new EntityProperty(version);
        property.setModel(model);
        property.setType(type);
        property.setName(name);
        property.setNullable(nullable);
        properties.add(property);
        return property;
    }

    public EntityProperty newProperty(EntityProperty.Type type, String name, boolean nullable, String alias, String sql) {
        EntityProperty property = new EntityProperty(version);
        property.setModel(model);
        property.setType(type);
        property.setName(name);
        property.setNullable(nullable);
        property.setAlias(alias);
        property.setSql(sql);
        properties.add(property);
        return property;
    }

}
