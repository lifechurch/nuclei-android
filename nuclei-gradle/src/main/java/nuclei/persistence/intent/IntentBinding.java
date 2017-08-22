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
package nuclei3.persistence.intent;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nuclei3.persistence.apt.AptProcessor;

public class IntentBinding {

    final Element element;
    final String name;
    final String fullName;
    final String packageName;
    final List<IntentProperty> properties;
    final Map<String, String> attributes;

    public IntentBinding(Element element, ProcessingEnvironment environment) {
        if (!(element instanceof TypeElement))
            throw new IllegalArgumentException("TypeElement required");
        attributes = new HashMap<>();

        for (AnnotationMirror m : environment.getElementUtils().getAllAnnotationMirrors(element)) {
            Element a = m.getAnnotationType().asElement();
            if (a instanceof TypeElement) {
                if (((TypeElement) a).getQualifiedName().contentEquals(AptProcessor.BINDING)) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : m.getElementValues().entrySet()) {
                        if (e.getValue() != null && e.getValue().getValue() != null)
                            attributes.put(e.getKey().getSimpleName().toString(), e.getValue().getValue().toString());
                    }
                    break;
                }
            }
        }

        String fullName = environment.getElementUtils().getBinaryName((TypeElement) element).toString();
        this.fullName = fullName.replace('$', '.');

        int ix = fullName.lastIndexOf('.');
        if (ix == -1) {
            this.name = fullName.replace('$', '_');
            this.packageName = null;
        } else {
            this.name = fullName.substring(ix + 1, fullName.length()).replace('$', '_');
            this.packageName = fullName.substring(0, ix);
        }
        this.element = element;
        this.properties = new ArrayList<>();
        for (Element child : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : child.getAnnotationMirrors()) {
                Element a = mirror.getAnnotationType().asElement();
                if (a instanceof TypeElement) {
                    if (((TypeElement) a).getQualifiedName().contentEquals(AptProcessor.BINDINGPROPERTY)) {
                        properties.add(new IntentProperty(child, environment));
                    }
                }
            }
        }
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Element getElement() {
        return element;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<IntentProperty> getProperties() {
        return properties;
    }
}
