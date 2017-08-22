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

import java.util.*;

public class Version {

    static int ID = 0;

    private int id;
    private DbContext context;
    private String defaultPackageName;
    private List<EntityModel> models;
    private List<String> sql;
    private VersionType type;

    public Version(DbContext context, String defaultPackageName, VersionType type) {
        this.id = (++ID);
        this.context = context;
        this.defaultPackageName = defaultPackageName;
        this.models = new ArrayList<EntityModel>();
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public VersionType getType() {
        return type;
    }

    public List<EntityModel> getModels() {
        return models;
    }

    public EntityModel newModel(String name, boolean render) {
        return newModel(name, defaultPackageName, render);
    }

    public ViewEntityModel newViewModel(String name, boolean render) {
        return newViewModel(name, defaultPackageName, render);
    }

    public EntityModel newModel(String name, String packageName, boolean render) {
        EntityModel model = EntityModelRegistry.newModel(this, name, packageName, render);
        this.models.add(model);
        return model;
    }

    public ViewEntityModel newViewModel(String name, String packageName, boolean render) {
        ViewEntityModel model = EntityModelRegistry.newViewModel(this, name, packageName, render);
        this.models.add(model);
        return model;
    }

    public List<String> getSql() {
        return sql;
    }

    public void setSql(List<String> sql) {
        this.sql = sql;
    }

    public Diff diff(Version diffVersion) {
        Diff diff = new Diff();
        Version lastVersion = this;
        if (this != context.baseVersion()) {
            int vIx = context.getVersions().indexOf(diffVersion);
            if (vIx > 1) {
                diff(diff, context, context.baseVersion(), context.getVersion(vIx - 1));
                diff.clear();
            }
        }
        diff(diff, context, this, diffVersion);
        return diff;
    }

    private static Diff diff(Diff diff, DbContext context, Version baseVersion, Version diffVersion) {
        int startIx = 0;
        int endIx = context.getVersions().size();
        int ix = 0;
        for (Version v : context.getVersions()) {
            if (v == baseVersion) {
                startIx = ix;
            }
            if (v == diffVersion) {
                endIx = ix + 1;
                break;
            }
            ix++;
        }

        diff.models.addAll(baseVersion.models);

        for (int i = startIx + 1; i < endIx; i++) {
            Version version = context.getVersion(i);

            switch (version.getType()) {
                case ADD_MODELS:
                    diff.models.addAll(version.models);
                    diff.addedModels.addAll(version.models);
                    break;
                case DROP_MODELS:
                    diff.models.removeAll(version.models);
                    diff.removedModels.addAll(version.models);
                    break;
                case DIFF:
                    Version lastVersion = context.getVersion(i - 1);

                    Set<EntityModel> addedModels = new LinkedHashSet<EntityModel>();
                    Set<EntityModel> removedModels = new LinkedHashSet<EntityModel>();

                    Set<EntityModel> lastModels = new HashSet<EntityModel>(lastVersion.models);
                    Set<EntityModel> newModels = new HashSet<EntityModel>(version.models);

                    for (EntityModel model : version.models) {
                        if (!diff.models.contains(model))
                            addedModels.add(model);
                        if (!lastModels.contains(model))
                            addedModels.add(model);
                    }
                    for (EntityModel model : diff.models) {
                        if (!newModels.contains(model))
                            removedModels.add(model);
                    }
                    for (EntityModel model : lastVersion.models) {
                        if (!newModels.contains(model))
                            removedModels.add(model);
                    }

                    diff.removedModels.addAll(removedModels);
                    diff.addedModels.addAll(addedModels);

                    diff.models.removeAll(removedModels);
                    diff.models.addAll(addedModels);
                    break;
            }
        }

        return diff;
    }

    public Diff diff(Version version, EntityModel model) {
        Diff diff = new Diff();
        if (this != model.baseVersion().getVersion()) {
            int vIx = context.getVersions().indexOf(version);
            if (vIx > 1) {
                diff(diff, model, context.baseVersion(), context.getVersion(vIx - 1));
                diff.clear();
            }
        }
        diff(diff, model, this, version);
        return diff;
    }

    private static void diff(Diff diff, EntityModel model, Version baseVersion, Version diffVersion) {
        EntityProperty id = new EntityProperty(null);
        id.setName("_id");
        id.setModel(model);
        int startIx = 0;
        int endIx = model.getVersions().size();
        int ix = 0;
        for (EntityModelVersion modelVersion : model.getVersions()) {
            if (modelVersion.getVersion() == baseVersion) {
                startIx = ix;
            }
            if (modelVersion.getVersion() == diffVersion) {
                endIx = ix + 1;
                break;
            }
            ix++;
        }
        diff.properties.addAll(model.getVersions().get(startIx).getProperties());
        for (int i = startIx + 1; i < endIx; i++) {
            EntityModelVersion modelVersion = model.getVersions().get(i);

            switch (modelVersion.getVersion().getType()) {
                case ADD_MODELS:
                case ADD_PROPERTIES:
                case ADD_INDEXES:
                    diff.addedProperties.addAll(modelVersion.getProperties());
                    diff.addedIndexes.addAll(modelVersion.getIndexes());
                    diff.properties.addAll(modelVersion.getProperties());
                    diff.indexes.addAll(modelVersion.getIndexes());
                    break;
                case DROP_PROPERTIES:
                case DROP_INDEXES:
                    diff.removedProperties.addAll(modelVersion.getProperties());
                    diff.removedIndexes.addAll(modelVersion.getIndexes());
                    diff.properties.removeAll(modelVersion.getProperties());
                    diff.indexes.removeAll(modelVersion.getIndexes());
                    break;
                case DIFF:
                case DIFF_PROPERTIES:
                    EntityModelVersion lastVersion = model.getVersions().get(i - 1);

                    Set<EntityProperty> addedProperties = new LinkedHashSet<EntityProperty>();
                    Set<EntityIndex> addedIndexes = new LinkedHashSet<EntityIndex>();
                    Set<EntityProperty> removedProperties = new LinkedHashSet<EntityProperty>();
                    Set<EntityIndex> removedIndexes = new LinkedHashSet<EntityIndex>();

                    Set<EntityProperty> lastProps = new HashSet<EntityProperty>(lastVersion.getProperties());
                    Set<EntityProperty> newProps = new HashSet<EntityProperty>(modelVersion.getProperties());

                    Set<EntityIndex> lastIndexes = new HashSet<EntityIndex>(lastVersion.getIndexes());
                    Set<EntityIndex> newIndexes = new HashSet<EntityIndex>(modelVersion.getIndexes());

                    for (EntityIndex index : modelVersion.getIndexes()) {
                        if (!diff.indexes.contains(index))
                            addedIndexes.add(index);
                        if (!lastIndexes.contains(index))
                            addedIndexes.add(index);
                    }
                    for (EntityIndex index : diff.indexes) {
                        if (!newIndexes.contains(index))
                            removedIndexes.add(index);
                    }
                    for (EntityIndex index : lastVersion.getIndexes()) {
                        if (!newIndexes.contains(index))
                            removedIndexes.add(index);
                    }

                    for (EntityProperty property : modelVersion.getProperties()) {
                        if (!diff.properties.contains(property))
                            addedProperties.add(property);
                        if (!lastProps.contains(property))
                            addedProperties.add(property);
                    }
                    for (EntityProperty property : diff.properties) {
                        if (!newProps.contains(property))
                            removedProperties.add(property);
                    }
                    for (EntityProperty property : lastVersion.getProperties()) {
                        if (!newProps.contains(property))
                            removedProperties.add(property);
                    }

                    removedProperties.remove(id);

                    diff.addedProperties.addAll(addedProperties);
                    diff.removedProperties.addAll(removedProperties);

                    diff.addedIndexes.addAll(addedIndexes);
                    diff.removedIndexes.addAll(removedIndexes);

                    diff.properties.removeAll(removedProperties);
                    diff.properties.addAll(addedProperties);

                    diff.indexes.removeAll(removedIndexes);
                    diff.indexes.addAll(addedIndexes);
                    break;
            }
        }
    }

    public static class Diff {
        public final List<EntityModel> addedModels = new ArrayList<EntityModel>();
        public final List<EntityProperty> addedProperties = new ArrayList<EntityProperty>();
        public final List<EntityIndex> addedIndexes = new ArrayList<EntityIndex>();

        public final List<EntityModel> removedModels = new ArrayList<EntityModel>();
        public final List<EntityProperty> removedProperties = new ArrayList<EntityProperty>();
        public final List<EntityIndex> removedIndexes = new ArrayList<EntityIndex>();

        public final Set<EntityProperty> properties = new HashSet<EntityProperty>();
        public final Set<EntityIndex> indexes = new HashSet<EntityIndex>();
        public final Set<EntityModel> models = new HashSet<EntityModel>();

        private void clear() {
            addedModels.clear();
            addedProperties.clear();
            addedIndexes.clear();
            removedModels.clear();
            removedProperties.clear();
            removedIndexes.clear();
        }
    }

    public enum VersionType {
        DIFF,
        DIFF_PROPERTIES,
        ADD_PROPERTIES,
        DROP_PROPERTIES,
        ADD_MODELS,
        DROP_MODELS,
        ADD_INDEXES,
        DROP_INDEXES
    }

    @Override
    public String toString() {
        return "Version " + getId();
    }
}
