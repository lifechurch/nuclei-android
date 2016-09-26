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
package ${model.packageName}.mapper.content.${prefix};

import nuclei.persistence.Query;
import android.content.ContentValues;

import ${model.packageName}.model.${model.name};

public class ${model.name}${suffix}Mapper implements Query.ContentValuesMapper<${model.name}> {

    @Override
    public ContentValues map(${model.name} object) {
        ContentValues values = new ContentValues();
        <#list model.getAllProperties(version) as property>
        <#if property.resolvedName == "_id">
        if (object.${property.resolvedName} > 0)
            values.put("${property.resolvedName}", object.${property.resolvedName});
        <#else>
        <#if property.javaType == "java.util.Date">
        if (object.${property.resolvedName} != null)
            values.put("${property.resolvedName}", object.${property.resolvedName}.getTime());
        else
            values.put("${property.resolvedName}", (Long) null);
        <#else>
        values.put("${property.resolvedName}", object.${property.resolvedName});
        </#if></#if></#list>
        return values;
    }

}
