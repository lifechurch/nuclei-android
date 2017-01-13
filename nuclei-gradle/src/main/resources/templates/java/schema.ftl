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
package ${packageName};

import android.net.Uri;
import android.provider.BaseColumns;
import nuclei.persistence.Query;
import nuclei.persistence.Query.MapperEntity;

public final class Schemas {

    public static final String AUTHORITY = "${authority}";

    <#list models as model>
    public interface ${model.name} extends BaseColumns {
        <#list model.getAllProperties(version) as property> <#if property.resolvedName != "_id">
        String ${property.upperCaseName} = "${property.resolvedName}";</#if></#list>

        String CONTENT_TYPE = "${model.contentType}";
        String CONTENT_ITEM_TYPE = "${model.contentItemType}";

        Uri CONTENT_URI = Uri.parse("content://${authority}/${model.name}");
        Uri CONTENT_ID_URI_BASE = Uri.parse("content://${authority}/${model.name}/");

        <#list model.selectQueries as query>
        String QUERY_${query.upperCaseName} = "SELECT_${model.name}_${query.name}";
        </#list>
        <#list model.updateQueries as query>
        String QUERY_UPDATE_${query.upperCaseName} = "UPDATE_${model.name}_${query.name}";
        </#list>
        <#list model.deleteQueries as query>
        String QUERY_DELETE_${query.upperCaseName} = "DELETE_${model.name}_${query.name}";
        </#list>

        public static final MapperEntity<${model.packageName}.model.${model.name}> INSERT = new MapperEntity<${model.packageName}.model.${model.name}>(CONTENT_URI, new ${model.packageName}.mapper.content.insert.${model.name}Mapper());
        <#list model.selectQueries as query>
        Query<${model.packageName}.model.${model.name}> SELECT_${query.upperCaseName} =
            new Query<${model.packageName}.model.${model.name}>(QUERY_${query.upperCaseName}, Query.QUERY_OPERATION_SELECT, CONTENT_URI, ${model.packageName}.model.${model.name}.class, new ${model.packageName}.mapper.cursor.${model.name}${query.name}Mapper(), ${query.placeholders},
                <#if query.selection??>"${query.selection}"<#else>null</#if>, <#if query.orderBy??>"${query.orderBy}"<#else>null</#if><#list query.properties as property>, ${property.upperCaseName}</#list>);
        </#list>
        <#list model.updateQueries as query>
        Query<${model.packageName}.model.${model.name}> UPDATE_${query.upperCaseName} =
            new Query<${model.packageName}.model.${model.name}>(QUERY_UPDATE_${query.upperCaseName}, Query.QUERY_OPERATION_UPDATE, CONTENT_URI, ${model.packageName}.model.${model.name}.class, new ${model.packageName}.mapper.content.update.${model.name}${query.name}Mapper(), ${query.placeholders}, <#if query.selection??>"${query.selection}"<#else>null</#if>);
        </#list>
        <#list model.deleteQueries as query>
        Query<${model.packageName}.model.${model.name}> DELETE_${query.upperCaseName} =
            new Query<${model.packageName}.model.${model.name}>(${model.name}.QUERY_DELETE_${query.upperCaseName}, Query.QUERY_OPERATION_DELETE, CONTENT_URI, ${model.packageName}.model.${model.name}.class, null, ${query.placeholders}, <#if query.selection??>"${query.selection}"<#else>null</#if>);
        </#list>
    }

    </#list>
}
