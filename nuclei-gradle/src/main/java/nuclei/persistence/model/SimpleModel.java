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

import javax.lang.model.element.Element;
import java.util.*;

public class SimpleModel {

    String id;
    int version;
    String fullName;
    String parentId;
    SimpleModel parent;
    List<Property> properties;
    boolean render;
    String serializer;
    String deserializer;
    Element element;

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public SimpleModel getParent() {
        return parent;
    }

    public void setParent(SimpleModel parent) {
        this.parent = parent;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isArray() {
        return this instanceof ArrayModel;
    }

    public String getType() {
        return null;
    }

    public String getTypeName() {
        return null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isRender() {
        return render;
    }

    public void setRender(boolean render) {
        this.render = render;
    }

    public String getSerializer() {
        return serializer;
    }

    public void setSerializer(String serializer) {
        this.serializer = serializer;
    }

    public String getDeserializer() {
        return deserializer;
    }

    public void setDeserializer(String deserializer) {
        this.deserializer = deserializer;
    }

    public List<Property> getProperties() {
        if (properties == null)
            properties = new ArrayList<>();
        return properties;
    }

    public String getPackageName() {
        int ix = fullName.lastIndexOf('.');
        return fullName.substring(0, ix);
    }

    public String getName() {
        int ix = fullName.lastIndexOf('.') + 1;
        return fullName.substring(ix, fullName.length());
    }

}
