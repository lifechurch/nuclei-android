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
package ${model.packageName}.mapper.cursor;

import android.database.Cursor;
import nuclei.persistence.PersistenceList;

import ${model.packageName}.model.${model.name};

public class ${model.name}${query.name}Mapper implements PersistenceList.CursorObjectMapper<${model.name}> {

    <#assign i = 0>
    <#list query.properties as property>
    public static final int ${property.upperCaseName} = ${i}; <#assign i = i + 1>
    </#list>

    @Override
    public void map(Cursor cursor, ${model.name} object) {
        <#list query.properties as property>
        object.${property.resolvedName} = ${property.cursorMethod};
        </#list>
    }

    @Override
    public ${model.name} newObject() {
        return new ${model.name}();
    }

}
