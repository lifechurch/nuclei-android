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
package nuclei.persistence.model;

public class Property {

    private boolean array;
    private String modelId;
    private SimpleModel owner;
    private SimpleModel model;
    private String alias;
    private String name;
    private String type;
    private String customDeserializer;
    private String customSerializer;

    public SimpleModel getOwner() {
        return owner;
    }

    public void setOwner(SimpleModel owner) {
        this.owner = owner;
    }

    public boolean isOwner(SimpleModel model) {
        return owner.equals(model);
    }

    public String getAlias() {
        if (alias == null || alias.length() == 0)
            return name;
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isArray() {
        return array;
    }

    public void setArray(boolean array) {
        this.array = array;
    }

    public boolean isModel() {
        return model != null;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public SimpleModel getModel() {
        return model;
    }

    public void setModel(SimpleModel model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeName() {
        return type.substring(0, 1).toUpperCase() + type.substring(1, type.length());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCustomDeserializer() {
        return customDeserializer;
    }

    public void setCustomDeserializer(String customDeserializer) {
        this.customDeserializer = customDeserializer;
    }

    public String getCustomSerializer() {
        return customSerializer;
    }

    public void setCustomSerializer(String customSerializer) {
        this.customSerializer = customSerializer;
    }

    public boolean isNullable() {
        if (type == null)
            return true;
        switch (type) {
            case "Boolean":
            case "Integer":
            case "Long":
            case "Double":
            case "Float":
            case "String":
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Property property = (Property) o;

        return name.equals(property.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
