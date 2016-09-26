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

public class ViewEntityModel extends EntityModel {

    private EntityModel rootModel;
    private String rootAlias;
    private List<EntityRelationship> viewModels;
    private String orderBy;
    private String groupBy;
    private String selection;

    public ViewEntityModel(Version version, String name, String packageName, boolean render) {
        super(version, name, packageName, render);
        viewModels = new ArrayList<>();
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public String getRootAlias() {
        return rootAlias;
    }

    public void setRootAlias(String rootAlias) {
        this.rootAlias = rootAlias;
    }

    public EntityModel getRootModel() {
        return rootModel;
    }

    public void setRootModel(EntityModel rootModel) {
        this.rootModel = rootModel;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public List<EntityRelationship> getModels() {
        return viewModels;
    }

    public EntityRelationship getModel(String name) {
        for (EntityRelationship relationship : viewModels) {
            if (relationship.getModel().getName().equals(name) || relationship.getAlias().equals(name))
                return relationship;
        }
        throw new NullPointerException("Model not found (" + name + ")");
    }

    public EntityRelationship addModel(EntityModel model, Relationship relationship, String alias, String selection) {
        EntityRelationship r = new EntityRelationship(model, relationship, alias, selection);
        viewModels.add(r);
        return r;
    }

    public static class EntityRelationship {
        private EntityModel model;
        private Relationship relationship;
        private String alias;
        private String selection;

        public EntityRelationship(EntityModel model, Relationship relationship, String alias, String selection) {
            this.model = model;
            this.relationship = relationship;
            this.alias = alias;
            this.selection = selection;
        }

        public String getAlias() {
            return alias;
        }

        public EntityModel getModel() {
            return model;
        }

        public Relationship getRelationship() {
            return relationship;
        }

        public String getSelection() {
            return selection;
        }

    }

    public enum Relationship {
        INNER,
        LEFT,
        RIGHT
    }

}
