package nuclei3.persistence.db;

public class EntityIndexProperty {

    public enum Order {
        DEFAULT,
        ASC,
        DESC
    }

    EntityProperty property;
    Order order;

    public String getName() {
        return property.getName();
    }

    public String getType() {
        if (order == Order.DEFAULT)
            return "";
        return order == Order.ASC ? "ASC" : "DESC";
    }

    public EntityProperty getProperty() {
        return property;
    }

    public void setProperty(EntityProperty property) {
        this.property = property;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

}
