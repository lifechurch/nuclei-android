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
package nuclei.persistence.intent;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class IntentProperty {

    final Element element;
    final String name;
    final String type;
    final String getterName;
    final boolean array;
    final boolean primitive;

    public IntentProperty(Element element, ProcessingEnvironment environment) {
        this.element = element;
        this.name = element.getSimpleName().toString();

        if (element.asType().getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) element.asType();
            TypeMirror compontentType = arrayType.getComponentType();
            if (compontentType.getKind().isPrimitive()) {
                type = environment.getTypeUtils().getPrimitiveType(compontentType.getKind()).toString();
                primitive = true;
            } else {
                Element e = environment.getTypeUtils().asElement(compontentType);
                type = e.getSimpleName().toString();
                primitive = false;
            }
            array = true;
        } else if (element.asType().getKind().isPrimitive()) {
            array = false;
            type = environment.getTypeUtils().getPrimitiveType(element.asType().getKind()).toString();
            primitive = true;
        } else {
            primitive = false;
            array = false;
            Element e = environment.getTypeUtils().asElement(element.asType());

            if (e.getSimpleName().toString().equals("ArrayList")) {
                if (element.asType() instanceof DeclaredType) {
                    List<? extends TypeMirror> types = ((DeclaredType) element.asType()).getTypeArguments();
                    if (types.size() != 1)
                        throw new IllegalArgumentException("Invalid ArrayList");
                    Element t = environment.getTypeUtils().asElement(types.get(0));
                    type = t.getSimpleName().toString() + e.getSimpleName().toString();
                } else
                    throw new IllegalArgumentException("Invalid ArrayList");
            } else
                type = e.getSimpleName().toString();
        }
        if (type.equals("Integer"))
            getterName = "int";
        else
            getterName = type;
    }

    public Element getElement() {
        return element;
    }

    public String getName() {
        return name;
    }

    String getName(String name) {
        if (name.length() <= 1)
            return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public String getBundleGetter() {
        /*if (int.class.equals(field.getType()) || Integer.class.equals(field.getType())) {
            value = bundle.getInt(name);
        } else if (long.class.equals(field.getType()) || Long.class.equals(field.getType())) {
            value = bundle.getLong(name);
        } else if (Date.class.equals(field.getType())) {
            long time = bundle.getLong(name);
            value = new Date(time);
        } else if (float.class.equals(field.getType()) || Float.class.equals(field.getType())) {
            value = bundle.getFloat(name);
        } else if (double.class.equals(field.getType()) || Double.class.equals(field.getType())) {
            value = bundle.getDouble(name);
        } else if (String.class.equals(field.getType())) {
            value = bundle.getString(name);
        } else if (String[].class.equals(field.getType())) {
            value = bundle.getStringArray(name);
        } else if (int[].class.equals(field.getType())) {
            value = bundle.getIntArray(name);
        } else if (long[].class.equals(field.getType())) {
            value = bundle.getLongArray(name);
        } else if (double[].class.equals(field.getType())) {
            value = bundle.getDoubleArray(name);
        } else if (float[].class.equals(field.getType())) {
            value = bundle.getFloatArray(name);
        } else if (boolean.class.equals(field.getType()) || Boolean.class.equals(field.getType())) {
            value = bundle.getBoolean(name);
        }*/
        if (array)
            return "get" + getName(getterName) + "Array(\"" + name + "\");";
        return "get" + getName(getterName) + "(\"" + name + "\");";
    }

    public String getBundleSetter() {
        /*if (int.class.equals(field.getType()) || Integer.class.equals(field.getType())) {
            value = bundle.getInt(name);
        } else if (long.class.equals(field.getType()) || Long.class.equals(field.getType())) {
            value = bundle.getLong(name);
        } else if (Date.class.equals(field.getType())) {
            long time = bundle.getLong(name);
            value = new Date(time);
        } else if (float.class.equals(field.getType()) || Float.class.equals(field.getType())) {
            value = bundle.getFloat(name);
        } else if (double.class.equals(field.getType()) || Double.class.equals(field.getType())) {
            value = bundle.getDouble(name);
        } else if (String.class.equals(field.getType())) {
            value = bundle.getString(name);
        } else if (String[].class.equals(field.getType())) {
            value = bundle.getStringArray(name);
        } else if (int[].class.equals(field.getType())) {
            value = bundle.getIntArray(name);
        } else if (long[].class.equals(field.getType())) {
            value = bundle.getLongArray(name);
        } else if (double[].class.equals(field.getType())) {
            value = bundle.getDoubleArray(name);
        } else if (float[].class.equals(field.getType())) {
            value = bundle.getFloatArray(name);
        } else if (boolean.class.equals(field.getType()) || Boolean.class.equals(field.getType())) {
            value = bundle.getBoolean(name);
        }*/
        if (array)
            return "put" + getName(getterName) + "Array";
        return "put" + getName(getterName);
    }

}
