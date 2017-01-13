<#--
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
-->
package ${model.packageName}.model;

import nuclei.persistence.Query;
import nuclei.persistence.Query.MapperEntity;
import nuclei.persistence.ModelObject;
import ${defaultPackageName}.providers.Schemas;

<#if model.view>
public class ${model.name} extends ${model.rootModel.fullName} {

    public static final MapperEntity<${model.name}> INSERT = Schemas.${model.name}.INSERT;
    <#list model.selectQueries as query>
    public static final Query<${model.name}> SELECT_${query.upperCaseName} = Schemas.${model.name}.SELECT_${query.upperCaseName};
    </#list>
    <#list model.updateQueries as query>
    public static final Query<${model.name}> UPDATE_${query.upperCaseName} = Schemas.${model.name}.UPDATE_${query.upperCaseName};
    </#list>
    <#list model.deleteQueries as query>
    public static final Query<${model.name}> DELETE_${query.upperCaseName} = Schemas.${model.name}.DELETE_${query.upperCaseName};
    </#list>

    <#list properties as property>
    <#if property.originalModel != model.rootModel>
    public ${property.javaType} ${property.resolvedName};
    <#else>
    /* public ${property.originalModel.fullName}.${property.javaType} ${property.resolvedName}; */
    </#if>
    </#list>
}
<#else>
public class ${model.name} implements ModelObject {

    public static final MapperEntity<${model.name}> INSERT = Schemas.${model.name}.INSERT;
    <#list model.selectQueries as query>
    public static final Query<${model.name}> SELECT_${query.upperCaseName} = Schemas.${model.name}.SELECT_${query.upperCaseName};
    </#list>
    <#list model.updateQueries as query>
    public static final Query<${model.name}> UPDATE_${query.upperCaseName} = Schemas.${model.name}.UPDATE_${query.upperCaseName};
    </#list>
    <#list model.deleteQueries as query>
    public static final Query<${model.name}> DELETE_${query.upperCaseName} = Schemas.${model.name}.DELETE_${query.upperCaseName};
    </#list>

    <#list properties as property>
    public ${property.javaType} ${property.resolvedName};
    </#list>
}
</#if>
