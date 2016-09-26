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
package nuclei.persistence.apt;

import com.google.auto.service.AutoService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import nuclei.persistence.intent.IntentBinding;
import nuclei.persistence.model.ArrayModel;
import nuclei.persistence.model.Context;
import nuclei.persistence.model.Property;
import nuclei.persistence.model.SimpleModel;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes({AptProcessor.BINDING, AptProcessor.BINDINGPROPERTY, AptProcessor.MODEL, AptProcessor.MODELPROPERTY})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedOptions({AptProcessor.DEFAULT_PACKAGE, AptProcessor.UI, AptProcessor.MODEL_TYPES})
public class AptProcessor extends AbstractProcessor {

    public static final String BINDING = "nuclei.intent.Binding";
    public static final String BINDINGPROPERTY = "nuclei.intent.BindingProperty";
    public static final String MODEL = "nuclei.persistence.model.Model";
    public static final String MODELPROPERTY = "nuclei.persistence.model.ModelProperty";

    public static final String DEFAULT_PACKAGE = "defaultPackage";
    public static final String UI = "nuclei.ui";
    public static final String MODEL_TYPES = "modelTypes";

    private Map<String, SimpleModel> allModels = new HashMap<>();
    private List<IntentBinding> bindings = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement annotation : annotations) {
                if (annotation.getQualifiedName().contentEquals(AptProcessor.BINDING)) {
                    for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                        IntentBinding binding = new IntentBinding(element, processingEnv);
                        bindings.add(binding);
                    }
                } else if (annotation.getQualifiedName().contentEquals(AptProcessor.MODEL)) {
                    for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                        processModel(element);
                    }
                }
            }

            renderModelBindings();

            allModels.clear();
            bindings.clear();

            return true;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    void renderModelBindings() throws Exception {
        if (bindings.size() == 0 && allModels.size() == 0)
            return;
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/");

        Template bindingTemplate = cfg.getTemplate("templates/java/binding.ftl");
        for (IntentBinding binding : bindings) {
            String name = (binding.getPackageName() != null ? binding.getPackageName() + "." : "")
                    + binding.getName() + "Binding";
            JavaFileObject file = this.processingEnv.getFiler()
                    .createSourceFile(name, binding.getElement());
            Writer writer = file.openWriter();
            try {
                Map<String, Object> model = new HashMap<>();
                model.put("binding", binding);
                model.put("cyto", "true".equals(processingEnv.getOptions().get(UI)));
                bindingTemplate.process(model, writer);
                writer.flush();
            } catch (TemplateException err) {
                throw new IOException(err);
            }
            writer.close();
        }

        if (allModels.size() > 0) {
            Element[] elements = new Element[allModels.size()];
            int ix = 0;
            for (SimpleModel simpleModel : allModels.values()) {
                elements[ix++] = simpleModel.getElement();
                if (simpleModel instanceof ArrayModel) {
                    ArrayModel arrayModel = (ArrayModel) simpleModel;
                    if (arrayModel.getModel() == null)
                        ((ArrayModel) simpleModel).setModel(allModels.get(arrayModel.getModelId()));
                    continue;
                }
                if (simpleModel.getParentId() != null && simpleModel.getParent() == null) {
                    simpleModel.setParent(allModels.get(simpleModel.getParentId()));
                    simpleModel.getProperties().addAll(simpleModel.getParent().getProperties());
                }
                for (Property property : simpleModel.getProperties()) {
                    if (property.getModelId() != null) {
                        property.setModel(allModels.get(property.getModelId()));
                        if (property.getModel() == null)
                            throw new Exception("Model (" + property.getModelId() + ") not found");
                    }
                }
            }

            String modelTypes = processingEnv.getOptions().get(MODEL_TYPES);
            if (modelTypes != null) {
                for (String modelType : modelTypes.split(",")) {
                    String packageName = processingEnv.getOptions().get(DEFAULT_PACKAGE);
                    if (packageName == null)
                        packageName = "nuclei.data";
                    Context context = new Context(allModels.values(), packageName, null);
                    context.render(cfg, modelType, processingEnv.getFiler());
                }
            }
        }
    }

    void processModel(Element element) {
        if (!(element instanceof TypeElement))
            throw new IllegalArgumentException("TypeElement required");
        TypeElement typeElement = (TypeElement) element;
        String fullName = processingEnv.getElementUtils().getBinaryName(typeElement).toString();

        boolean array = false;

        SimpleModel model = new SimpleModel();
        model.setId(fullName);
        model.setFullName(fullName);

        for (AnnotationMirror m : processingEnv.getElementUtils().getAllAnnotationMirrors(element)) {
            Element a = m.getAnnotationType().asElement();
            if (a instanceof TypeElement) {
                if (((TypeElement) a).getQualifiedName().contentEquals(AptProcessor.MODEL)) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : m.getElementValues().entrySet()) {
                        if (e.getValue() != null && e.getValue().getValue() != null)
                            switch (e.getKey().getSimpleName().toString()) {
                                case "id":
                                    model.setId(e.getValue().getValue().toString());
                                    break;
                                case "serializer":
                                    model.setSerializer(e.getValue().getValue().toString() + ".serializer");
                                    model.setDeserializer(e.getValue().getValue().toString() + ".deserializer");
                                    break;
                                case "version":
                                    model.setVersion(Integer.parseInt(e.getValue().getValue().toString()));
                                    break;
                                case "array":
                                    array = "true".equals(e.getValue().getValue().toString());
                                    break;
                            }
                    }
                }
                break;
            }
        }

        if (model.getId().contains("."))
            throw new IllegalArgumentException("Invalid id, it cannot contain .");

        for (Element child : element.getEnclosedElements()) {
            for (AnnotationMirror mirror : child.getAnnotationMirrors()) {
                Element a = mirror.getAnnotationType().asElement();
                if (a instanceof TypeElement) {
                    if (((TypeElement) a).getQualifiedName().contentEquals(AptProcessor.MODELPROPERTY)) {
                        Property property = processProperty(child, mirror);
                        property.setOwner(model);
                        model.getProperties().add(property);
                    }
                }
            }
        }

        if (array) {
            ArrayModel m = new ArrayModel();
            m.setId(model.getId() + "Array");
            m.setVersion(model.getVersion());
            m.setModel(model);
            m.setModelId(model.getId());
            allModels.put(m.getId(), m);
        }

        allModels.put(model.getFullName(), model);
    }

    Property processProperty(Element element, AnnotationMirror mirror) {
        Property property = new Property();
        property.setName(element.getSimpleName().toString());

        for (AnnotationMirror m : processingEnv.getElementUtils().getAllAnnotationMirrors(element)) {
            Element a = m.getAnnotationType().asElement();
            if (a instanceof TypeElement) {
                if (((TypeElement) a).getQualifiedName().contentEquals(AptProcessor.MODELPROPERTY)) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : mirror.getElementValues().entrySet()) {
                        if (e.getValue() != null && e.getValue().getValue() != null)
                            switch (e.getKey().getSimpleName().toString()) {
                                case "alias":
                                    property.setAlias(e.getValue().getValue().toString());
                                    break;
                                case "name":
                                    property.setName(e.getValue().getValue().toString());
                                    break;
                                case "type":
                                    property.setType(e.getValue().getValue().toString());
                                    break;
                                case "serializer":
                                    property.setCustomSerializer(e.getValue().getValue().toString() + ".serialize");
                                    property.setCustomDeserializer(e.getValue().getValue().toString() + ".deserialize");
                                    break;
                            }
                    }
                }
            }
        }

        if (element.asType().getKind() == TypeKind.ARRAY) {
            throw new UnsupportedOperationException("Use a java.util.List instead of an array");
        } else if (element.asType().getKind().isPrimitive()) {
            property.setType(processingEnv.getTypeUtils().getPrimitiveType(element.asType().getKind()).toString());
        } else {
            Element e = processingEnv.getTypeUtils().asElement(element.asType());
            if (e.getSimpleName().toString().equals("ArrayList") || e.getSimpleName().toString().equals("List")) {
                if (element.asType() instanceof DeclaredType) {
                    List<? extends TypeMirror> types = ((DeclaredType) element.asType()).getTypeArguments();
                    if (types.size() != 1)
                        throw new IllegalArgumentException("Invalid ArrayList");
                    Element t = processingEnv.getTypeUtils().asElement(types.get(0));
                    if (t instanceof TypeElement) {
                        property.setModelId(((TypeElement) t).getQualifiedName().toString());
                        property.setArray(true);
                    } else
                        throw new IllegalArgumentException("ArrayList type is invalid");
                } else
                    throw new IllegalArgumentException("Invalid ArrayList");
            } else {
                if (e instanceof TypeElement)
                    property.setModelId(((TypeElement) e).getQualifiedName().toString());
                else
                    throw new IllegalArgumentException("Element type is invalid");
            }
        }

        if (property.getModelId() != null && property.getModelId().startsWith("java.lang.")) {
            property.setType(property.getModelId().substring("java.lang.".length()));
            property.setModelId(null);
        }

        return property;
    }

}
