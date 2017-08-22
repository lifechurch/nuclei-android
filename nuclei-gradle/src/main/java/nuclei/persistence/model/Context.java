package nuclei3.persistence.model;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Context {

    Collection<SimpleModel> models;
    String packageName;
    File outDir;

    public Context(Collection<SimpleModel> models, String packageName, File outDir) {
        this.models = models;
        this.packageName = packageName;
        this.outDir = outDir;
    }

    public void render(String modelType) throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setClassLoaderForTemplateLoading(SimpleModel.class.getClassLoader(), "/");
        if (!outDir.getParentFile().exists())
            outDir.getParentFile().mkdirs();
        String modelTypeName = modelType.substring(0, 1).toUpperCase() + modelType.substring(1);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("packageName", packageName);
        params.put("models", models);
        writeFile(packageName, modelTypeName, getTemplate(cfg, modelType), params);

        for (SimpleModel model : models) {
            if (model.isRender()) {
                params = new HashMap<>();
                params.put("model", model);
                writeFile(model.getPackageName(), model.getName(), getModelTemplate(cfg), params);
            }
        }
    }

    public void render(Configuration cfg, String modelType, Filer filer) throws IOException {
        String modelTypeName = modelType.substring(0, 1).toUpperCase() + modelType.substring(1);
        Map<String, Object> params = new HashMap<>();
        params.put("packageName", packageName);
        params.put("models", models);
        writeFile(filer, packageName, modelTypeName, getTemplate(cfg, modelType), params);

        for (SimpleModel model : models) {
            if (model.isRender()) {
                params = new HashMap<>();
                params.put("model", model);
                writeFile(filer, model.getPackageName(), model.getName(), getModelTemplate(cfg), params);
            }
        }
    }

    void writeFile(String packageName, String name, Template template, Object dataModel) throws IOException {
        File directory = new File(outDir.getAbsolutePath() + "/" + packageName.replace('.', '/'));
        if (!directory.exists())
            directory.mkdirs();
        File file = new File(directory, name + ".java");
        if (file.exists())
            file.delete();
        FileWriter writer = new FileWriter(file);
        try {
            template.process(dataModel, writer);
        } catch (TemplateException err) {
            throw new IOException(err);
        }
        writer.close();
    }

    void writeFile(Filer filer, String packageName, String name, Template template, Object dataModel) throws IOException {
        JavaFileObject file = filer.createSourceFile(packageName + "." + name);
        Writer writer = file.openWriter();
        try {
            template.process(dataModel, writer);
        } catch (TemplateException err) {
            throw new IOException(err);
        } finally {
            writer.close();
        }
    }

    Template getTemplate(Configuration configuration, String type) throws IOException {
        return configuration.getTemplate("templates/java/" + type + ".ftl");
    }

    Template getModelTemplate(Configuration configuration) throws IOException {
        return configuration.getTemplate("templates/java/jsonbinarymodel.ftl");
    }

    public static Context newContext(JSONObject model, String packageName, File outDir) throws Exception {
        int version = model.has("version") ? model.getInt("version") : 1;
        JSONArray models = model.getJSONArray("models");
        Map<String, SimpleModel> allModels = new HashMap<String, SimpleModel>();
        for (int i = 0; i < models.length(); i++) {
            JSONObject modelDetails = models.getJSONObject(i);
            Object modelObject = modelDetails.get("model");
            SimpleModel m;
            if (modelObject instanceof JSONArray) {
                Object details = ((JSONArray) modelObject).get(0);
                ArrayModel a = new ArrayModel();
                if (details instanceof JSONObject) {
                    JSONObject arrayModelDetails = (JSONObject) details;
                    if (arrayModelDetails.has("id"))
                        a.setModelId(arrayModelDetails.getString("id"));
                    if (arrayModelDetails.has("type"))
                        a.setType(arrayModelDetails.getString("type"));
                } else {
                    a.setType(details.toString());
                }
                m = a;
            } else {
                m = new SimpleModel();
                JSONObject jsonModel = (JSONObject) modelObject;
                for (Object key : jsonModel.keySet()) {
                    Property p = new Property();
                    p.setName(key.toString());
                    Object val = jsonModel.get(key.toString());
                    if (val instanceof JSONArray) {
                        p.setArray(true);
                        val = ((JSONArray) val).get(0);
                    }
                    if (val instanceof JSONObject) {
                        JSONObject obj = (JSONObject) val;
                        if (obj.has("id"))
                            p.setModelId(obj.getString("id"));
                        if (obj.has("type"))
                            p.setType(obj.getString("type"));
                        if (obj.has("alias"))
                            p.setAlias(obj.getString("alias"));
                        if (obj.has("deserializer"))
                            p.setCustomDeserializer(obj.getString("deserializer"));
                        if (obj.has("serializer"))
                            p.setCustomSerializer(obj.getString("serializer"));
                    } else {
                        p.setType(val.toString());
                    }
                    p.setOwner(m);
                    m.getProperties().add(p);
                }
            }

            m.setId(modelDetails.getString("id"));
            if (modelDetails.has("fullName")) {
                m.setFullName(modelDetails.getString("fullName"));
                m.setRender(modelDetails.has("render") && modelDetails.getBoolean("render"));
            } else {
                m.setFullName(m.getType());
            }
            if (modelDetails.has("deserializer"))
                m.setDeserializer(modelDetails.getString("deserializer"));
            if (modelDetails.has("serializer"))
                m.setSerializer(modelDetails.getString("serializer"));
            m.setVersion(version);

            if (modelDetails.has("extends"))
                m.setParentId(modelDetails.getString("extends"));

            allModels.put(m.getId(), m);
        }

        JSONObject deserializers = model.getJSONObject("deserializers");
        JSONObject serializers = model.getJSONObject("serializers");

        for (SimpleModel simpleModel : allModels.values()) {
            if (simpleModel instanceof ArrayModel) {
                ArrayModel arrayModel = (ArrayModel) simpleModel;
                ((ArrayModel) simpleModel).setModel(allModels.get(arrayModel.getModelId()));
                continue;
            }
            if (simpleModel.getParentId() != null) {
                simpleModel.setParent(allModels.get(simpleModel.getParentId()));
                Set<Property> unique = new HashSet<>(simpleModel.getProperties());
                for (Property property : simpleModel.parent.getProperties()) {
                    if (!unique.contains(property))
                        simpleModel.getProperties().add(property);
                }
            }
            for (Property property : simpleModel.getProperties()) {
                if (property.getModelId() != null) {
                    property.setModel(allModels.get(property.getModelId()));
                    if (property.getModel() == null)
                        throw new Exception("Model (" + property.getModelId() + ") not found");
                }
                if (deserializers.has(property.getType())) {
                    property.setCustomDeserializer(deserializers.getString(property.getType()));
                } else if (deserializers.has(property.getCustomDeserializer())) {
                    property.setCustomDeserializer(deserializers.getString(property.getCustomDeserializer()));
                }
                if (serializers.has(property.getType())) {
                    property.setCustomSerializer(serializers.getString(property.getType()));
                } else if (serializers.has(property.getCustomSerializer())) {
                    property.setCustomSerializer(serializers.getString(property.getCustomSerializer()));
                }
            }
        }

        return new Context(allModels.values(), packageName == null ? model.getString("packageName") : packageName, outDir);
    }

}
