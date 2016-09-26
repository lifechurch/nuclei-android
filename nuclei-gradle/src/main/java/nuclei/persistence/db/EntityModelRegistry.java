package nuclei.persistence.db;

import java.util.HashMap;
import java.util.Map;

public class EntityModelRegistry {

    private static Map<String, EntityModel> MODELS = new HashMap<>();

    public static void clear() {
        MODELS.clear();
    }

    public static EntityModel newModel(Version version, String name, String packageName, boolean render) {
        EntityModel model = MODELS.get(packageName + ".model." + name);
        if (model == null) {
            model = new EntityModel(version, name, packageName, render);
            MODELS.put(model.getFullName(), model);
        } else {
            model.newVersion(version);
        }
        return model;
    }

    public static ViewEntityModel newViewModel(Version version, String name, String packageName, boolean render) {
        ViewEntityModel model = (ViewEntityModel) MODELS.get(packageName + ".model." + name);
        if (model == null) {
            model = new ViewEntityModel(version, name, packageName, render);
            MODELS.put(model.getFullName(), model);
        } else {
            model.newVersion(version);
        }
        return model;
    }

    public static EntityModel getModel(String name, String packageName) {
        return MODELS.get(packageName + ".model." + name);
    }

}
