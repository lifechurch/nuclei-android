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
create view ${model.name} as select
<#list properties as property><#if second??>,</#if> <#if property.sql??>${property.sql}<#else>${property.modelAlias}.${property.name}</#if><#if property.alias??> as ${property.alias}<#else></#if>
<#assign second=true>
</#list> from ${model.rootModel.name} as ${model.rootAlias}
<#list model.models as m>
 ${m.relationship} JOIN ${m.model.name} as ${m.alias} on (${m.selection})
</#list>
<#if model.groupBy??>
 group by ${model.groupBy}
</#if>
<#if model.orderBy??>
 order by ${model.orderBy}
</#if>
<#if model.selection??>
 where ${model.selection}
</#if>