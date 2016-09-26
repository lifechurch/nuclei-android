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
--->
package ${packageName};

import android.content.Context;

import android.support.BinaryReader;
import android.support.BinaryWriter;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class Binary {
    <#list models as model>

    public static class ${model.id} {

        <#if model.array>
        public static void serialize(Context context, BinaryWriter writer, List<${model.fullName}> objects) throws IOException {
            if (objects == null) {
                writer.nullValue();
                return;
            }
            writer.value((int) ${model.version});
            writer.value(objects.size());
            for (${model.fullName} object : objects) {
                if (object == null) {
                    writer.nullValue();
                    continue;
                }
                <#if model.type??>
                writer.value(object);
                <#else>
                Binary.${model.modelId}.serialize(context, writer, object);
                </#if>
            }
        }
        <#else>
        public static void serialize(Context context, BinaryWriter writer, ${model.fullName} object) throws IOException {
            if (object == null) {
                writer.nullValue();
                return;
            }
            <#if model.serializer??>
            object = ${model.serializer}(context, object);
            </#if>
            writer.value((int) ${model.version});
            <#list model.properties as property>
            <#if property.isArray()>
            if (object.${property.alias} == null)
                writer.nullValue();
            else {
                writer.value(object.${property.alias}.size());
                <#if property.isModel()>
                for (${property.getModel().fullName} val : object.${property.alias}) {
                    <#if property.getCustomSerializer()??>
                    ${property.customSerializer}(context, writer, val);
                    <#else>
                    Binary.${property.getModel().id}.serialize(context, writer, val);
                    </#if>
                }
                <#else>
                for (${property.type} val : object.${property.alias}) {
                    <#if property.getCustomSerializer()??>
                    ${property.customSerializer}(context, writer, val);
                    <#else>
                    if (val == null) {
                        writer.nullValue();
                        continue;
                    }
                    writer.value(val);
                    </#if>
                }
                </#if>
            }
            <#else>
            <#if property.isModel()>
            Binary.${property.getModel().id}.serialize(context, writer, object.${property.alias});
            <#else>
            <#if property.getCustomSerializer()??>
            ${property.customSerializer}(context, writer, object.${property.alias});
            <#else>
            <#if property.nullable>
            if (object.${property.alias} != null)
                writer.value(object.${property.alias});
            else
                writer.nullValue();
            <#else>
            writer.value(object.${property.alias});
            </#if>
            </#if>
            </#if>
            </#if>
            </#list>
        }
        </#if>

        <#if model.array>
        public static List<${model.fullName}> deserialize(Context context, BinaryReader reader) throws IOException {
            if (reader.isNull())
                return null;
            if (reader.nextInt() != ${model.version})
                throw new IllegalStateException("Invalid Version");
            int len = reader.nextInt();
            List<${model.fullName}> objects = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                <#if model.type??>
                if (reader.isNull()) {
                    objects.add(null);
                    continue;
                }
                objects.add(reader.next${model.typeName}());
                <#else>
                objects.add(Binary.${model.modelId}.deserialize(context, reader));
                </#if>
            }
            return objects;
        }
        <#else>
        public static ${model.fullName} deserialize(Context context, BinaryReader reader) throws IOException {
            if (reader.isNull())
                    return null;
            if (reader.nextInt() != ${model.version})
                throw new IllegalStateException("Invalid Version");
            ${model.fullName} object = new ${model.fullName}();
            <#list model.properties as property>
            <#if property.isArray()>
            if (!reader.isNull()) {
                object.${property.alias} = new ArrayList<>();
                int len = reader.nextInt();
                for (int i = 0; i < len; i++) {
                    <#if property.getCustomDeserializer()??>
                    object.${property.alias}.add(${property.customDeserializer}(context, reader));
                    <#else>
                    <#if property.isModel()>
                    object.${property.alias}.add(Binary.${property.getModel().id}.deserialize(context, reader));
                    <#else>
                    if (reader.isNull()) {
                        object.${property.alias}.add(null);
                        continue;
                    }
                    object.${property.alias}.add(reader.next${property.typeName}());
                    </#if>
                    </#if>
                }
            }
            <#else>
            <#if property.getCustomDeserializer()??>
            object.${property.alias} = ${property.customDeserializer}(context, reader);
            <#else>
            <#if property.isModel()>
            object.${property.alias} = Binary.${property.getModel().id}.deserialize(context, reader);
            <#else>
            if (!reader.isNull())
                object.${property.alias} = reader.next${property.typeName}();
            </#if>
            </#if>
            </#if>
            </#list>
            <#if model.deserializer??>
            object = ${model.deserializer}(context, object);
            </#if>
            return object;
        }
        </#if>

    }
    </#list>

}

