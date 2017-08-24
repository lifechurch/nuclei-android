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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import nuclei.persistence.JSONArray;
import nuclei.persistence.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbContext {

    private String defaultPackageName;
    private String authority;
    private String databaseName;
    private List<Version> versions;
    private File outDir;

    public DbContext(String defaultPackageName, String authority, String databaseName, File outDir) {
        this.defaultPackageName = defaultPackageName;
        this.authority = authority;
        this.databaseName = databaseName;
        this.versions = new ArrayList<Version>();
        this.outDir = outDir;
        newVersion(Version.VersionType.DIFF);
    }

    public String getDefaultPackageName() {
        return defaultPackageName;
    }

    public String getAuthority() {
        return authority;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public List<Version> getVersions() {
        return versions;
    }

    public File getOutDir() {
        return outDir;
    }

    public Version newVersion(Version.VersionType type) {
        Version version = new Version(this, defaultPackageName, type);
        versions.add(version);
        return version;
    }

    public Version baseVersion() {
        return versions.get(0);
    }

    public Version currentVersion() {
        if (versions.size() == 0)
            return null;
        return versions.get(versions.size() - 1);
    }

    public Version getVersion(int ix) {
        return versions.get(ix);
    }

    public List<EntityModel> getAllModels() {
        Version.Diff diff = baseVersion().diff(currentVersion());
        return new ArrayList<EntityModel>(diff.models);
    }

    public EntityModel getModel(String name) {
        return getModel(name, defaultPackageName);
    }

    public EntityModel getModel(String name, String packageName) {
        return EntityModelRegistry.getModel(name, packageName);
    }

    public void render() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/");
        renderPersistence(cfg);
        renderContentProvider(cfg);
        renderDbHelper(cfg);
        renderModels(cfg);
        renderMappers(cfg);
    }

    Template getTemplate(Configuration configuration, String type, String name) throws IOException {
        return configuration.getTemplate("templates/" + type + "/" + name + ".ftl");
    }

    void renderPersistence(Configuration cfg) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("models", getAllModels());
        params.put("package", defaultPackageName);
        writeFile(defaultPackageName + ".persistence", "Persistence", getTemplate(cfg, "java", "persistencemanager"), params);
    }

    void renderContentProvider(Configuration cfg) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("models", getAllModels());
        params.put("package", defaultPackageName + ".providers");
        params.put("defaultPackage", defaultPackageName);
        writeFile(defaultPackageName + ".providers", "NucleiContentProvider", getTemplate(cfg, "java", "contentprovider"), params);
    }

    void diffProperties(List<String> sql, Version lastVersion, Version version, Version.Diff diff, Configuration cfg) throws IOException {
        for (EntityModel model : diff.models) {
            diff = lastVersion.diff(version, model);
            for (EntityProperty removed : diff.removedProperties) {
                sql.add(renderDropProperty(removed, cfg));
            }
            for (EntityProperty added : diff.addedProperties) {
                sql.add(renderAddProperty(added, cfg));
            }
        }
    }

    void renderDbHelper(Configuration cfg) throws IOException {
        Version lastVersion = null;
        for (Version version : versions) {
            if (lastVersion != null) {
                List<String> sql = new ArrayList<String>();
                switch (version.getType()) {
                    case DIFF: {
                        Version.Diff diff = lastVersion.diff(version);
                        for (EntityModel removed : diff.removedModels) {
                            if (removed instanceof ViewEntityModel)
                                sql.add(renderDropStructure(removed, cfg));
                        }
                        for (EntityModel removed : diff.removedModels) {
                            if (!(removed instanceof ViewEntityModel))
                                sql.add(renderDropStructure(removed, cfg));
                        }
                        for (EntityIndex removed : diff.removedIndexes) {
                            sql.add(renderDropIndex(removed, cfg));
                        }
                        for (EntityModel added : diff.addedModels) {
                            if (!(added instanceof ViewEntityModel))
                                sql.add(renderCreateStructure(added, added.getProperties(version), cfg));
                        }
                        for (EntityModel added : diff.addedModels) {
                            if (added instanceof ViewEntityModel)
                                sql.add(renderCreateStructure(added, added.getProperties(version), cfg));
                        }
                        for (EntityIndex added : diff.addedIndexes) {
                            sql.add(renderCreateIndex(added, cfg));
                        }
                        diffProperties(sql, lastVersion, version, diff, cfg);
                        break;
                    }
                    case DIFF_PROPERTIES: {
                        Version.Diff diff = lastVersion.diff(version);
                        diffProperties(sql, lastVersion, version, diff, cfg);
                        break;
                    }
                    case ADD_MODELS:
                        for (EntityModel model : version.getModels()) {
                            sql.add(renderCreateStructure(model, model.getProperties(version), cfg));
                            for (EntityIndex index : model.getIndexes(version)) {
                                sql.add(renderCreateIndex(index, cfg));
                            }
                        }
                        break;
                    case DROP_MODELS:
                        for (EntityModel removed : version.getModels()) {
                            if (removed instanceof ViewEntityModel)
                                sql.add(renderDropStructure(removed, cfg));
                        }
                        for (EntityModel removed : version.getModels()) {
                            if (!(removed instanceof ViewEntityModel))
                                sql.add(renderDropStructure(removed, cfg));
                        }
                        break;
                    case ADD_PROPERTIES: {
                        for (EntityModel model : version.getModels()) {
                            for (EntityProperty property : model.getProperties(version)) {
                                sql.add(renderAddProperty(property, cfg));
                            }
                        }
                        break;
                    }
                    case ADD_INDEXES: {
                        for (EntityModel model : version.getModels()) {
                            for (EntityIndex index : model.getIndexes(version)) {
                                sql.add(renderCreateIndex(index, cfg));
                            }
                        }
                        break;
                    }
                    case DROP_PROPERTIES: {
                        for (EntityModel model : version.getModels()) {
                            for (EntityProperty property : model.getProperties(version)) {
                                sql.add(renderDropProperty(property, cfg));
                            }
                        }
                        break;
                    }
                    case DROP_INDEXES: {
                        for (EntityModel model : version.getModels()) {
                            for (EntityIndex index : model.getIndexes(version)) {
                                sql.add(renderDropIndex(index, cfg));
                            }
                        }
                        break;
                    }
                }

                version.setSql(sql);
            } else {
                List<String> sql = new ArrayList<String>();
                for (EntityModel model : version.getModels()) {
                    sql.add(renderCreateStructure(model, model.getProperties(version), cfg));
                    for (EntityIndex index : model.getIndexes(version)) {
                        sql.add(renderCreateIndex(index, cfg));
                    }
                }
                version.setSql(sql);
            }
            lastVersion = version;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("baseVersion", baseVersion());
        params.put("versions", versions);
        params.put("versionCount", versions.size());
        params.put("packageName", defaultPackageName + ".providers");
        params.put("defaultPackage", defaultPackageName);
        params.put("databaseName", databaseName);
        writeFile(defaultPackageName + ".providers", "NucleiDbHelper", getTemplate(cfg, "java", "dbhelper"), params);
    }

    String renderCreateStructure(EntityModel model, List<EntityProperty> properties, Configuration cfg) throws IOException {
        StringWriter out = new StringWriter();
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("model", model);
            params.put("properties", properties);
            if (model instanceof ViewEntityModel)
                getTemplate(cfg, "sql", "model_create_view").process(params, out);
            else if (model.getExtensions().contains("fts3"))
                getTemplate(cfg, "sql", "model_create_fts3").process(params, out);
            else
                getTemplate(cfg, "sql", "model_create").process(params, out);
        } catch (TemplateException err) {
            throw new IOException(err);
        }
        return out.toString().replaceAll("\\n", "").replaceAll("\\r", "");
    }

    String renderAddProperty(EntityProperty property, Configuration cfg) throws IOException {
        StringWriter out = new StringWriter();
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("model", property.getModel());
            params.put("property", property);
            getTemplate(cfg, "sql", "model_add_column").process(params, out);
        } catch (TemplateException err) {
            throw new IOException(err);
        }
        return out.toString().replaceAll("\\n", "").replaceAll("\\r", "");
    }

    String renderCreateIndex(EntityIndex index, Configuration cfg) throws IOException {
        StringWriter out = new StringWriter();
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("index", index);
            getTemplate(cfg, "sql", "model_create_index").process(params, out);
        } catch (TemplateException err) {
            throw new IOException(err);
        }
        return out.toString().replaceAll("\\n", "").replaceAll("\\r", "");
    }

    String renderDropStructure(EntityModel model, Configuration cfg) throws IOException {
        StringWriter out = new StringWriter();
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("model", model);
            if (model instanceof ViewEntityModel)
                getTemplate(cfg, "sql", "model_drop_view").process(params, out);
            else if (model.getExtensions().contains("fts3"))
                getTemplate(cfg, "sql", "model_drop_fts3").process(params, out);
            else
                getTemplate(cfg, "sql", "model_drop").process(params, out);
        } catch (TemplateException err) {
            throw new IOException(err);
        }
        return out.toString().replaceAll("\\n", "").replaceAll("\\r", "");
    }

    String renderDropProperty(EntityProperty property, Configuration cfg) throws IOException {
        StringWriter out = new StringWriter();
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("model", property.getModel());
            params.put("property", property);
            getTemplate(cfg, "sql", "model_drop_column").process(params, out);
        } catch (TemplateException err) {
            throw new IOException(err);
        }
        return out.toString().replaceAll("\\n", "").replaceAll("\\r", "");
    }

    String renderDropIndex(EntityIndex index, Configuration cfg) throws IOException {
        StringWriter out = new StringWriter();
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("index", index);
            getTemplate(cfg, "sql", "model_drop_index").process(params, out);
        } catch (TemplateException err) {
            throw new IOException(err);
        }
        return out.toString().replaceAll("\\n", "").replaceAll("\\r", "");
    }

    void renderModels(Configuration cfg) throws IOException {
        List<EntityModel> allModels = getAllModels();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("models", allModels);
        params.put("packageName", defaultPackageName + ".providers");
        params.put("authority", authority);
        params.put("version", currentVersion());
        writeFile(defaultPackageName + ".providers", "Schemas", getTemplate(cfg, "java", "schema"), params);
        for (EntityModel model : allModels) {
            if (!model.isRender())
                continue;
            params = new HashMap<String, Object>();
            params.put("packageName", defaultPackageName + ".model");
            params.put("defaultPackageName", defaultPackageName);
            params.put("model", model);
            params.put("version", currentVersion());
            params.put("properties", model.getAllProperties(null));
            writeFile(model.getPackageName() + ".model", model.getName(),
                    getTemplate(cfg, "java", "model"), params);
        }
    }

    void renderMappers(Configuration cfg) throws IOException {
        for (EntityModel model : getAllModels()) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("model", model);
            params.put("version", currentVersion());
            for (Query query : model.getSelectQueries()) {
                params.put("query", query);
                writeFile(defaultPackageName + ".mapper.cursor", model.getName() + query.getName() + "Mapper",
                        getTemplate(cfg, "java", "cursorobjectmapper"), params);
            }
            params.put("prefix", "insert");
            params.put("suffix", "");
            writeFile(defaultPackageName + ".mapper.content.insert", model.getName() + "Mapper",
                    getTemplate(cfg, "java", "contentvaluesmapper"), params);
            for (Query query : model.getUpdateQueries()) {
                params.put("prefix", "update");
                params.put("suffix", query.getName());
                writeFile(defaultPackageName + ".mapper.content.update", model.getName() + query.getName() + "Mapper",
                        getTemplate(cfg, "java", "contentvaluesmapper"), params);
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

    public static DbContext newContext(JSONObject model, File outputDir, String defaultPackage, String authority) throws IOException {
        EntityModelRegistry.clear();
        DbContext ctx = new DbContext(model.get("defaultPackage").toString(),
                authority == null
                        ? model.get("authority").toString()
                        : authority, model.get("databaseName").toString(), outputDir);

        JSONArray versions = (JSONArray) model.get("versions");
        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = (JSONObject) versions.getJSONObject(i);
            JSONArray entities = (JSONArray) version.get("entities");

            for (int e = 0; e < entities.length(); e++) {
                JSONObject entity = (JSONObject) entities.get(e);
                processEntity(entity, ctx, ctx.currentVersion());
            }

            if (i + 1 < versions.length()) {
                version = (JSONObject) versions.get(i + 1);
                ctx.newVersion(
                        version.has("type")
                                ? Version.VersionType.valueOf(version.get("type").toString())
                                : Version.VersionType.DIFF);
            }
        }

        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = (JSONObject) versions.get(i);
            JSONObject defaultQueries = version.has("queries") ? (JSONObject) version.get("queries") : null;
            JSONArray entities = (JSONArray) version.get("entities");

            for (int e = 0; e < entities.length(); e++) {
                JSONObject entity = (JSONObject) entities.get(e);
                EntityModel entityModel = ctx.getModel(entity.get("name").toString());
                processQueries(defaultQueries, entityModel, ctx.currentVersion());
            }
        }

        return ctx;
    }

    static EntityModel processEntity(JSONObject entity, DbContext ctx, Version version) {
        if (entity.has("rootModel")) { // if there is a rootModel property, we're dealing with a view
            processEntityView(entity, ctx);
        } else { // otherwise an entity table
            EntityModelVersion v = ctx.currentVersion().newModel(entity.get("name").toString(),
                    !entity.has("render") || entity.getBoolean("render")).currentVersion();
            if (entity.has("properties")) {
                JSONArray properties = (JSONArray) entity.get("properties");
                for (int p = 0; p < properties.length(); p++) {
                    JSONObject property = (JSONObject) properties.get(p);
                    v.newProperty(
                            property.has("type")
                                    ? EntityProperty.Type.valueOf(property.get("type").toString())
                                    : EntityProperty.Type.UNKNOWN,
                            property.get("name").toString(),
                            property.has("nullable") && property.getBoolean("nullable"));
                }
            }
        }

        EntityModel m = ctx.getModel(entity.get("name").toString());
        if (m == null)
            throw new NullPointerException("Model " + entity.get("name").toString() + " not found");
        if (entity.has("extensions")) {
            JSONArray extensions = (JSONArray) entity.get("extensions");
            for (int p = 0; p < extensions.length(); p++) {
                m.getExtensions().add(extensions.get(p).toString());
            }
        }

        if (entity.has("queries"))
            processQueries((JSONObject) entity.get("queries"), m, version);

        if (entity.has("indexes")) {
            JSONArray indexes = entity.getJSONArray("indexes");
            for (int i = 0; i < indexes.length(); i++) {
                JSONObject index = (JSONObject) indexes.get(i);

                List<EntityIndexProperty> properties = new ArrayList<EntityIndexProperty>();

                if (index.has("properties")) {
                    JSONArray indexProperties = (JSONArray) index.get("properties");
                    for (int p = 0; p < indexProperties.length(); p++) {
                        Object prop = indexProperties.get(p);
                        EntityIndexProperty property = new EntityIndexProperty();
                        if (prop instanceof String) {
                            property.setProperty(m.getProperty(m.currentVersion().getVersion(), (String) prop));
                            property.setOrder(EntityIndexProperty.Order.DEFAULT);
                        } else {
                            JSONObject o = (JSONObject) prop;
                            property.setProperty(m.getProperty(m.currentVersion().getVersion(), o.get("name").toString()));
                            property.setOrder(o.has("order")
                                    ? EntityIndexProperty.Order.valueOf(index.get("order").toString())
                                    : EntityIndexProperty.Order.DEFAULT);
                        }
                        properties.add(property);
                    }
                }

                m.currentVersion()
                        .newIndex(index.has("type")
                                ? EntityIndex.Type.valueOf(index.get("type").toString())
                                : EntityIndex.Type.DEFAULT,
                                index.get("name").toString(), properties);
            }
        }

        return m;
    }

    static void processEntityView(JSONObject entity, DbContext ctx) {
        ViewEntityModel viewModel = ctx.currentVersion().newViewModel(entity.get("name").toString(),
                !entity.has("render") || entity.getBoolean("render"));
        viewModel.setRootModel(ctx.getModel(entity.get("rootModel").toString()));
        viewModel.setRootAlias(entity.get("rootAlias").toString());
        if (entity.has("groupBy"))
            viewModel.setGroupBy(entity.get("groupBy").toString());
        if (entity.has("selection"))
            viewModel.setSelection(entity.get("selection").toString());

        EntityModelVersion v = viewModel.currentVersion();

        if (entity.has("models")) {
            JSONArray models = (JSONArray) entity.get("models");
            // add the models
            for (int m = 0; m < models.length(); m++) {
                JSONObject child = (JSONObject) models.get(m);
                EntityModel childModel = ctx.getModel(child.get("name").toString());
                ViewEntityModel.Relationship relationship = ViewEntityModel.Relationship.valueOf(child.get("type").toString());
                String alias = child.has("alias") ? child.get("alias").toString() : child.get("name").toString();
                viewModel.addModel(childModel, relationship, alias, child.get("selection").toString());
            }
        }
        // process model properties
        processViewModel(ctx, entity, viewModel, v);
    }

    static void processViewModel(DbContext ctx, JSONObject entity, ViewEntityModel viewModel, EntityModelVersion version) {
        processProperties(entity, viewModel, ctx, version);

        for (EntityProperty property : viewModel.getAllProperties(version.getVersion())) {
            if ("_id".equals(property.getName()) && (property.getModelAlias() == null || property.getModel() == null)) {
                property.setModel(viewModel.getRootModel());
                property.setModelAlias(viewModel.getRootAlias());
            }
        }
    }

    static void processProperties(JSONObject entity, ViewEntityModel viewModel, DbContext ctx, EntityModelVersion version) {
        JSONArray properties = (JSONArray) entity.get("properties");
        for (int p = 0; p < properties.length(); p++) {
            JSONObject property = (JSONObject) properties.get(p);
            EntityModel child;
            String alias;

            if (property.has("model")) {
                String model = property.get("model").toString();
                if (model.equals(viewModel.getRootModel().getName())) {
                    child = viewModel.getRootModel();
                    alias = viewModel.getRootAlias();
                } else {
                    ViewEntityModel.EntityRelationship relationship = viewModel.getModel(property.get("model").toString());
                    child = relationship.getModel();
                    alias = relationship.getAlias();
                }
            } else {
                child = viewModel;
                alias = viewModel.getRootAlias();
            }

            String propertyName = property.get("name").toString();
            if ("*".equals(propertyName)) {
                for (EntityProperty childProperty : child.getAllProperties(version.getVersion())) {
                    String name = childProperty.getName();
                    if (childProperty.getModel() instanceof ViewEntityModel && childProperty.getAlias() != null)
                        name = childProperty.getAlias();
                    EntityProperty cp = version.newProperty(
                            childProperty.getType(),
                            name,
                            childProperty.isNullable(),
                            null,
                            null);
                    cp.setModelAlias(alias);
                    cp.setOriginalModel(childProperty.getModel());
                }
            } else {
                EntityProperty childProperty = null;
                try {
                    childProperty = child.getProperty(version.getVersion(), propertyName);
                } catch (IllegalArgumentException ignore) {}
                EntityProperty cp = version.newProperty(
                        childProperty == null ? EntityProperty.Type.valueOf(property.get("type").toString()) : childProperty.getType(),
                        property.get("name").toString(),
                        !property.has("nullable") || property.getBoolean("nullable"),
                        property.has("alias") ? property.get("alias").toString() : null,
                        property.has("sql") ? property.get("sql").toString() : null);
                cp.setModelAlias(alias);
                if (childProperty != null)
                    cp.setOriginalModel(childProperty.getModel());
            }
        }
    }

    static void processQueries(JSONObject queries, EntityModel m, Version version) {
        if (queries != null) {
            if (queries.has("select")) {
                JSONArray select = (JSONArray) queries.get("select");
                for (int s = 0; s < select.length(); s++) {
                    JSONObject query = (JSONObject) select.get(s);
                    List<EntityProperty> props = getProperties(query, m, version);
                    m.newSelectQuery(query.get("id").toString(), query.isNull("selection") ? null : query.get("selection").toString(),
                            query.isNull("orderBy") ? null : query.get("orderBy").toString(), props);
                }
            }
            if (queries.has("update")) {
                JSONArray update = (JSONArray) queries.get("update");
                for (int s = 0; s < update.length(); s++) {
                    JSONObject query = (JSONObject) update.get(s);
                    List<EntityProperty> props = getProperties(query, m, version);
                    m.newUpdateQuery(query.get("id").toString(), query.isNull("selection") ? null : query.get("selection").toString(), props);
                }
            }
            if (queries.has("delete")) {
                JSONArray delete = (JSONArray) queries.get("delete");
                for (int s = 0; s < delete.length(); s++) {
                    JSONObject query = (JSONObject) delete.get(s);
                    m.newDeleteQuery(query.get("id").toString(), query.isNull("selection") ? null : query.get("selection").toString());
                }
            }
        }
    }

    static List<EntityProperty> getProperties(JSONObject query, EntityModel m, Version version) {
        List<EntityProperty> props = null;
        if (query.has("properties")) {
            props = new ArrayList<>();
            JSONArray names = (JSONArray) query.get("properties");
            for (int n = 0; n < names.length(); n++) {
                props.add(m.getProperty(version, names.get(n).toString()));
            }
        }
        return props;
    }

    public static void main(String[] args) throws IOException {
        FileInputStream input = new FileInputStream(args[0]);
        byte[] buf = new byte[input.available()];
        input.read(buf);
        input.close();
        JSONObject model = new JSONObject(new String(buf, "UTF-8"));
        DbContext.newContext(model, new File(args[1]), null, null).render();
    }

}
