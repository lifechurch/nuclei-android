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

public class EntityProperty {

    private Version version;
    private String name;
    private String alias;
    private Type type;
    private boolean nullable;
    private EntityModel model;
    private EntityModel originalModel;
    private String modelAlias;
    private String sql;

    public EntityProperty(Version version) {
        this.version = version;
    }

    public String getResolvedName() {
        return alias == null ? name : alias;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public EntityModel getOriginalModel() {
        return originalModel;
    }

    public void setOriginalModel(EntityModel originalModel) {
        this.originalModel = originalModel;
    }

    public String getModelAlias() {
        if (modelAlias == null)
            return model.getName();
        return modelAlias;
    }

    public void setModelAlias(String modelAlias) {
        this.modelAlias = modelAlias;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public String getUpperCaseName() {
        return getResolvedName().toUpperCase();
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public EntityModel getModel() {
        return model;
    }

    public void setModel(EntityModel model) {
        this.model = model;
        this.originalModel = model;
    }

    public String getSqlType() {
        String sqlType;
        switch (type) {
            case TEXT:
                sqlType = "TEXT";
                break;
            case DATE:
                sqlType = "DATE";
                break;
            case BOOLEAN:
                sqlType = "INTEGER";
                break;
            case INTEGER:
                sqlType = "INTEGER";
                break;
            case LONG:
                sqlType = "INTEGER";
                break;
            case FLOAT:
                sqlType = "REAL";
                break;
            case DOUBLE:
                sqlType = "REAL";
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }
        if (!nullable)
            return sqlType + " NOT NULL";
        return sqlType;
    }

    public String getJavaType() {
        switch (type) {
            case TEXT:
                return "String";
            case DATE:
                return "java.util.Date";
            case BOOLEAN:
                return nullable ? "Boolean" : "boolean";
            case INTEGER:
                return nullable ? "Integer" : "int";
            case LONG:
                return nullable ? "Long" : "long";
            case FLOAT:
                return nullable ? "Float" : "float";
            case DOUBLE:
                return nullable ? "Double" : "double";
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    public String getCursorMethod() {
        switch (type) {
            case TEXT:
                return "cursor.getString(" + getUpperCaseName() + ")";
            case DATE:
                if (isNullable())
                    return "cursor.isNull(" + getUpperCaseName() + ") ? null : new java.util.Date(cursor.getLong(" + getUpperCaseName() + "))";
                return "new java.util.Date(cursor.getLong(" + getUpperCaseName() + "))";
            case BOOLEAN:
                if (isNullable())
                    return "cursor.isNull(" + getUpperCaseName() + ") ? null : cursor.getInt(" + getUpperCaseName() + ") == 1";
                return "cursor.getInt(" + getUpperCaseName() + ") == 1";
            case INTEGER:
                if (isNullable())
                    return "cursor.isNull(" + getUpperCaseName() + ") ? null : cursor.getInt(" + getUpperCaseName() + ")";
                return "cursor.getInt(" + getUpperCaseName() + ")";
            case LONG:
                if (isNullable())
                    return "cursor.isNull(" + getUpperCaseName() + ") ? null : cursor.getLong(" + getUpperCaseName() + ")";
                return "cursor.getLong(" + getUpperCaseName() + ")";
            case DOUBLE:
                if (isNullable())
                    return "cursor.isNull(" + getUpperCaseName() + ") ? null : cursor.getDouble(" + getUpperCaseName() + ")";
                return "cursor.getDouble(" + getUpperCaseName() + ")";
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    public enum Type {
        TEXT,
        DATE,
        BOOLEAN,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        UNKNOWN
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityProperty property = (EntityProperty) o;

        if (name != null ? !name.equals(property.name) : property.name != null) return false;
        if (alias != null ? !alias.equals(property.alias) : property.alias != null) return false;
        if (model != null ? !model.equals(property.model) : property.model != null) return false;
        return modelAlias != null ? modelAlias.equals(property.modelAlias) : property.modelAlias == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (modelAlias != null ? modelAlias.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name + " " + type;
    }

}
