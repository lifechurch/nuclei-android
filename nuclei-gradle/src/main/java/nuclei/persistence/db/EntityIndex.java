package nuclei3.persistence.db;

import java.util.List;

public class EntityIndex {

    public enum Type {
        UNIQUE,
        DEFAULT
    }

    String name;
    EntityModel model;
    List<EntityIndexProperty> properties;
    Type type;

    public String getTypeName() {
        return type == Type.UNIQUE ? "UNIQUE" : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EntityModel getModel() {
        return model;
    }

    public void setModel(EntityModel model) {
        this.model = model;
    }

    public List<EntityIndexProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<EntityIndexProperty> properties) {
        this.properties = properties;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityIndex index = (EntityIndex) o;

        if (name != null ? !name.equals(index.name) : index.name != null) return false;
        if (model != null ? !model.equals(index.model) : index.model != null) return false;
        if (properties != null ? !properties.equals(index.properties) : index.properties != null) return false;
        return type == index.type;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

}
