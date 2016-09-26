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
    }

    </#list>
}
