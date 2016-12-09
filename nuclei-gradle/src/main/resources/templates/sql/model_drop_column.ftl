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
BEGIN TRANSACTION;
alter table ${model.name} rename to ${model.name}_old;
create table ${model.name} (_id INTEGER PRIMARY KEY<#list properties as property><#if property.name != '_id'>,${property.name} ${property.sqlType}</#if></#list>);
insert into ${model.name} (_id<#list properties as property><#if property.name != '_id'>,${property.name}</#if></#list>) select _id<#list properties as property><#if property.name != '_id'>,${property.name}</#if></#list> from ${model.name}_old;
drop table ${model.name}_old;
COMMIT;
