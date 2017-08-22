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
package nuclei3.persistence.model;

import java.util.List;

public class ArrayModel extends SimpleModel {

    SimpleModel model;
    String modelId;
    String type;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getType() {
        return type;
    }

    public String getTypeName() {
        String name = type.substring(0, 1).toUpperCase() + type.substring(1, type.length());
        if ("Integer".equals(name))
            return "Int";
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean isRender() {
        return false;
    }

    public SimpleModel getModel() {
        return model;
    }

    public void setModel(SimpleModel model) {
        this.model = model;
    }

    @Override
    public String getFullName() {
        if (model == null)
            return getType();
        return model.getFullName();
    }

    @Override
    public List<Property> getProperties() {
        return model.getProperties();
    }

    @Override
    public String getPackageName() {
        return model.getPackageName();
    }

    @Override
    public String getName() {
        return model.getName();
    }
}
