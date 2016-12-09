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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityModel {

    private Version version;
    private String name;
    private String packageName;
    private String contentType;
    private String contentItemType;
    private List<EntityModelVersion> versions;
    private List<Query> selectQueries;
    private List<Query> updateQueries;
    private List<Query> deleteQueries;
    private List<String> extensions;
    private boolean render = true;

    public EntityModel(Version version, String name, String packageName, boolean render) {
        this.version = version;
        this.name = name;
        this.packageName = packageName;
        this.render = render;
        contentType = "vnd.android.cursor.dir/vnd." + packageName + "." + name.toLowerCase();
        contentItemType = "vnd.android.cursor.item/vnd." + packageName + "." + name.toLowerCase();
        versions = new ArrayList<>();
        if (!(this instanceof ViewEntityModel))
            newVersion(version)
                .newProperty(EntityProperty.Type.LONG, "_id", false);
        else
            newVersion(version);
        selectQueries = new ArrayList<>();
        updateQueries = new ArrayList<>();
        deleteQueries = new ArrayList<>();
        extensions = new ArrayList<>();
    }

    public boolean isView() {
        return this instanceof ViewEntityModel;
    }

    public boolean isRender() {
        return render;
    }

    public Version getVersion() {
        return version;
    }

    public String getFullName() {
        return packageName + ".model." + name;
    }

    public String getName() {
        return name;
    }

    public String getNameUpperCase() {
        return name.toUpperCase();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentItemType() {
        return contentItemType;
    }

    public List<EntityModelVersion> getVersions() {
        return versions;
    }

    public List<Query> getSelectQueries() {
        if (isView()) {
            ViewEntityModel model = (ViewEntityModel) this;
            EntityModel entityModel = model.getRootModel();
            Map<String, Query> queries = new HashMap<>();
            for (Query query : entityModel.getSelectQueries()) {
                query = query.clone();
                query.setProperties(getAllProperties(currentVersion().getVersion()));
                queries.put(query.getName(), query.clone());
            }
            for (Query query : selectQueries) {
                queries.put(query.getName(), query);
            }
            return new ArrayList<>(queries.values());
        }
        return selectQueries;
    }

    public List<Query> getUpdateQueries() {
        return updateQueries;
    }

    public List<Query> getDeleteQueries() {
        return deleteQueries;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public EntityModelVersion newVersion(Version version) {
        EntityModelVersion v = new EntityModelVersion(version, this);
        versions.add(v);
        return v;
    }

    public EntityModelVersion baseVersion() {
        return versions.get(0);
    }

    public EntityModelVersion currentVersion() {
        return versions.get(versions.size() - 1);
    }

    public List<EntityProperty> getBaseProperties() {
        return baseVersion().getProperties();
    }

    public List<EntityProperty> getAllProperties(Version version) {
        Version.Diff diff = baseVersion().getVersion()
                .diff(version, this);
        return new ArrayList<>(diff.properties);
    }

    public List<EntityIndex> getAllIndexes(Version version) {
        Version.Diff diff = baseVersion().getVersion()
                .diff(version, this);
        return new ArrayList<>(diff.indexes);
    }

    public List<EntityIndex> getIndexes(Version version) {
        for (EntityModelVersion v : versions) {
            if (v.getVersion() == version)
                return v.getIndexes();
        }
        throw new IllegalArgumentException("Invalid version");
    }

    public List<EntityProperty> getProperties(Version version) {
        for (EntityModelVersion v : versions) {
            if (v.getVersion() == version)
                return v.getProperties();
        }
        throw new IllegalArgumentException("Invalid version");
    }

    public EntityProperty getProperty(Version version, String name) {
        for (EntityProperty property : getAllProperties(version))
            if (property.getName().equals(name))
                return property;
        throw new IllegalArgumentException("Unknown property : " + name);
    }

    public Query newSelectQuery(String name, String selection, String orderBy, List<EntityProperty> properties) {
        for (Query e : selectQueries) {
            if (e.getName().equals(name))
                return e;
        }
        Query q = new Query(name, selection, orderBy, this, properties);
        selectQueries.add(q);
        return q;
    }

    public Query newUpdateQuery(String name, String selection, List<EntityProperty> properties) {
        for (Query e : updateQueries) {
            if (e.getName().equals(name))
                return e;
        }
        Query q = new Query(name, selection, null, this, properties);
        updateQueries.add(q);
        return q;
    }

    public Query newDeleteQuery(String name, String selection) {
        for (Query e : deleteQueries) {
            if (e.getName().equals(name))
                return e;
        }
        Query q = new Query(name, selection, null, this, null);
        deleteQueries.add(q);
        return q;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityModel model = (EntityModel) o;

        if (!name.equals(model.name)) return false;
        return packageName.equals(model.packageName);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + packageName.hashCode();
        return result;
    }
}
