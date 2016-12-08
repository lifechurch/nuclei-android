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
import org.json.JSONArray;
import org.json.JSONObject;

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
        DbContext ctx = new DbContext(model.getString("defaultPackage"),
                authority == null
                        ? model.getString("authority")
                        : authority, model.getString("databaseName"), outputDir);

        JSONArray versions = model.getJSONArray("versions");
        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            JSONArray entities = version.getJSONArray("entities");

            for (int e = 0; e < entities.length(); e++) {
                JSONObject entity = entities.getJSONObject(e);
                processEntity(entity, ctx, ctx.currentVersion());
            }

            if (i + 1 < versions.length()) {
                version = versions.getJSONObject(i + 1);
                ctx.newVersion(
                        version.has("type")
                                ? Version.VersionType.valueOf(version.getString("type"))
                                : Version.VersionType.DIFF);
            }
        }

        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            JSONObject defaultQueries = version.has("queries") ? version.getJSONObject("queries") : null;
            JSONArray entities = version.getJSONArray("entities");

            for (int e = 0; e < entities.length(); e++) {
                JSONObject entity = entities.getJSONObject(e);
                EntityModel entityModel = ctx.getModel(entity.getString("name"));
                processQueries(defaultQueries, entityModel, ctx.currentVersion());
            }
        }

        return ctx;
    }

    static EntityModel processEntity(JSONObject entity, DbContext ctx, Version version) {
        if (entity.has("rootModel")) { // if there is a rootModel property, we're dealing with a view
            processEntityView(entity, ctx);
        } else { // otherwise an entity table
            EntityModelVersion v = ctx.currentVersion().newModel(entity.getString("name"),
                    !entity.has("render") || entity.getBoolean("render")).currentVersion();
            if (entity.has("properties")) {
                JSONArray properties = entity.getJSONArray("properties");
                for (int p = 0; p < properties.length(); p++) {
                    JSONObject property = properties.getJSONObject(p);
                    v.newProperty(
                            property.has("type")
                                    ? EntityProperty.Type.valueOf(property.getString("type"))
                                    : EntityProperty.Type.UNKNOWN,
                            property.getString("name"),
                            property.has("nullable") && property.getBoolean("nullable"));
                }
            }
        }

        EntityModel m = ctx.getModel(entity.getString("name"));
        if (m == null)
            throw new NullPointerException("Model " + entity.getString("name") + " not found");
        if (entity.has("extensions")) {
            JSONArray extensions = entity.getJSONArray("extensions");
            for (int p = 0; p < extensions.length(); p++) {
                m.getExtensions().add(extensions.getString(p));
            }
        }

        if (entity.has("queries"))
            processQueries(entity.getJSONObject("queries"), m, version);

        if (entity.has("indexes")) {
            JSONArray indexes = entity.getJSONArray("indexes");
            for (int i = 0; i < indexes.length(); i++) {
                JSONObject index = indexes.getJSONObject(i);

                List<EntityIndexProperty> properties = new ArrayList<EntityIndexProperty>();

                if (index.has("properties")) {
                    JSONArray indexProperties = index.getJSONArray("properties");
                    for (int p = 0; p < indexProperties.length(); p++) {
                        Object prop = indexProperties.get(p);
                        EntityIndexProperty property = new EntityIndexProperty();
                        if (prop instanceof String) {
                            property.setProperty(m.getProperty(m.currentVersion().getVersion(), (String) prop));
                            property.setOrder(EntityIndexProperty.Order.DEFAULT);
                        } else {
                            JSONObject o = (JSONObject) prop;
                            property.setProperty(m.getProperty(m.currentVersion().getVersion(), o.getString("name")));
                            property.setOrder(o.has("order")
                                    ? EntityIndexProperty.Order.valueOf(index.getString("order"))
                                    : EntityIndexProperty.Order.DEFAULT);
                        }
                        properties.add(property);
                    }
                }

                m.currentVersion()
                        .newIndex(index.has("type")
                                ? EntityIndex.Type.valueOf(index.getString("type"))
                                : EntityIndex.Type.DEFAULT,
                                index.getString("name"), properties);
            }
        }

        return m;
    }

    static void processEntityView(JSONObject entity, DbContext ctx) {
        ViewEntityModel viewModel = ctx.currentVersion().newViewModel(entity.getString("name"),
                !entity.has("render") || entity.getBoolean("render"));
        viewModel.setRootModel(ctx.getModel(entity.getString("rootModel")));
        viewModel.setRootAlias(entity.getString("rootAlias"));
        if (entity.has("groupBy"))
            viewModel.setGroupBy(entity.getString("groupBy"));
        if (entity.has("selection"))
            viewModel.setSelection(entity.getString("selection"));

        EntityModelVersion v = viewModel.currentVersion();

        if (entity.has("models")) {
            JSONArray models = entity.getJSONArray("models");
            // add the models
            for (int m = 0; m < models.length(); m++) {
                JSONObject child = models.getJSONObject(m);
                EntityModel childModel = ctx.getModel(child.getString("name"));
                ViewEntityModel.Relationship relationship = ViewEntityModel.Relationship.valueOf(child.getString("type"));
                String alias = child.has("alias") ? child.getString("alias") : child.getString("name");
                viewModel.addModel(childModel, relationship, alias, child.getString("selection"));
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
        JSONArray properties = entity.getJSONArray("properties");
        for (int p = 0; p < properties.length(); p++) {
            JSONObject property = properties.getJSONObject(p);
            EntityModel child;
            String alias;

            if (property.has("model")) {
                String model = property.getString("model");
                if (model.equals(viewModel.getRootModel().getName())) {
                    child = viewModel.getRootModel();
                    alias = viewModel.getRootAlias();
                } else {
                    ViewEntityModel.EntityRelationship relationship = viewModel.getModel(property.getString("model"));
                    child = relationship.getModel();
                    alias = relationship.getAlias();
                }
            } else {
                child = viewModel;
                alias = viewModel.getRootAlias();
            }

            String propertyName = property.getString("name");
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
                        childProperty == null ? EntityProperty.Type.valueOf(property.getString("type")) : childProperty.getType(),
                        property.getString("name"),
                        !property.has("nullable") || property.getBoolean("nullable"),
                        property.has("alias") ? property.getString("alias") : null,
                        property.has("sql") ? property.getString("sql") : null);
                cp.setModelAlias(alias);
                if (childProperty != null)
                    cp.setOriginalModel(childProperty.getModel());
            }
        }
    }

    static void processQueries(JSONObject queries, EntityModel m, Version version) {
        if (queries != null) {
            if (queries.has("select")) {
                JSONArray select = queries.getJSONArray("select");
                for (int s = 0; s < select.length(); s++) {
                    JSONObject query = select.getJSONObject(s);
                    List<EntityProperty> props = getProperties(query, m, version);
                    m.newSelectQuery(query.getString("id"), query.isNull("selection") ? null : query.getString("selection"),
                            query.isNull("orderBy") ? null : query.getString("orderBy"), props);
                }
            }
            if (queries.has("update")) {
                JSONArray update = queries.getJSONArray("update");
                for (int s = 0; s < update.length(); s++) {
                    JSONObject query = update.getJSONObject(s);
                    List<EntityProperty> props = getProperties(query, m, version);
                    m.newUpdateQuery(query.getString("id"), query.isNull("selection") ? null : query.getString("selection"), props);
                }
            }
            if (queries.has("delete")) {
                JSONArray delete = queries.getJSONArray("delete");
                for (int s = 0; s < delete.length(); s++) {
                    JSONObject query = delete.getJSONObject(s);
                    m.newDeleteQuery(query.getString("id"), query.isNull("selection") ? null : query.getString("selection"));
                }
            }
        }
    }

    static List<EntityProperty> getProperties(JSONObject query, EntityModel m, Version version) {
        List<EntityProperty> props = null;
        if (query.has("properties")) {
            props = new ArrayList<>();
            JSONArray names = query.getJSONArray("properties");
            for (int n = 0; n < names.length(); n++) {
                props.add(m.getProperty(version, names.getString(n)));
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
