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
package ${model.packageName};

import nuclei.persistence.ModelObject;
import java.util.*;

public class ${model.name}<#if model.parent??> extends ${model.parent.fullName}</#if> implements ModelObject {
<#list model.properties as property>
    <#if property.isOwner(model)>
    <#if property.isArray()>
    <#if property.isModel()>
    public List<${property.getModel().fullName}> ${property.alias};
    <#else>
    public List<${property.type}> ${property.alias};
    </#if>
    <#else>
    <#if property.isModel()>
    public ${property.getModel().fullName} ${property.alias};
    <#else>
    public ${property.type} ${property.alias};
    </#if>
    </#if>
    </#if>
</#list>
}
