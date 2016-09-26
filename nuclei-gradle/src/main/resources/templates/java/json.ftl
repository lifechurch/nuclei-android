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

import android.content.Context;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.JsonToken;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class Json {
    <#list models as model>

    public static class ${model.id} {

        <#if model.array>
        public static String serialize(Context context, List<${model.fullName}> object) throws IOException {
        <#else>
        public static String serialize(Context context, ${model.fullName} object) throws IOException {
        </#if>
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            JsonWriter writer = new JsonWriter(stringWriter);
            serialize(context, writer, object);
            return stringWriter.toString();
        }

        <#if model.array>
        public static void serialize(Context context, JsonWriter writer, List<${model.fullName}> objects) throws IOException {
            if (objects == null) {
                writer.nullValue();
                return;
            }
            writer.beginArray();
            for (${model.fullName} object : objects) {
                if (object == null) {
                    writer.nullValue();
                    continue;
                }
                <#if model.type??>
                writer.value(object);
                <#else>
                Json.${model.modelId}.serialize(context, writer, object);
                </#if>
            }
            writer.endArray();
        }
        <#else>
        public static void serialize(Context context, JsonWriter writer, ${model.fullName} object) throws IOException {
            if (object == null) {
                writer.nullValue();
                return;
            }
            <#if model.serializer??>
            object = ${model.serializer}(context, object);
            </#if>
            writer.beginObject();
            <#list model.properties as property>
            writer.name("${property.name}");
            <#if property.isArray()>
            if (object.${property.alias} == null)
                writer.nullValue();
            else {
                writer.beginArray();
                <#if property.isModel()>
                for (${property.getModel().fullName} val : object.${property.alias})
                    <#if property.getCustomSerializer()??>
                    ${property.customSerializer}(context, writer, val);
                    <#else>
                    Json.${property.getModel().id}.serialize(context, writer, val);
                    </#if>
                <#else>
                for (${property.type} val : object.${property.alias})
                    <#if property.getCustomSerializer()??>
                    ${property.customSerializer}(context, writer, val);
                    <#else>
                    <#if property.nullable>
                    if (val == null)
                        writer.nullValue();
                    else
                        writer.value(val);
                    <#else>
                    writer.value(val);
                    </#if>
                    </#if>
                </#if>
                writer.endArray();
            }
            <#else>
            <#if property.isModel()>
            Json.${property.getModel().id}.serialize(context, writer, object.${property.alias});
            <#else>
            <#if property.getCustomSerializer()??>
            ${property.customSerializer}(context, writer, object.${property.alias});
            <#else>
            <#if property.nullable>
            if (object.${property.alias} == null)
                writer.nullValue();
            else
                writer.value(object.${property.alias});
            <#else>
            writer.value(object.${property.alias});
            </#if>
            </#if>
            </#if>
            </#if>
            </#list>
            writer.endObject();
        }
        </#if>

        <#if model.array>
        public static List<${model.fullName}> deserialize(Context context, JsonReader reader) throws IOException {
            List<${model.fullName}> objects = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    objects.add(null);
                    continue;
                }
                <#if model.type??>
                objects.add(reader.next${model.typeName}());
                <#else>
                objects.add(Json.${model.modelId}.deserialize(context, reader));
                </#if>
            }
            reader.endArray();
            return objects;
        }
        <#else>
        public static ${model.fullName} deserialize(Context context, JsonReader reader) throws IOException {
            ${model.fullName} object = new ${model.fullName}();
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    continue;
                }
                switch (name) {
                    <#list model.properties as property>
                    case "${property.name}":
                        <#if property.isArray()>
                        object.${property.alias} = new ArrayList<>();
                        reader.beginArray();
                        while (reader.hasNext()) {
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull();
                                object.${property.alias}.add(null);
                                continue;
                            }
                            <#if property.getCustomDeserializer()??>
                            object.${property.alias}.add(${property.customDeserializer}(context, reader));
                            <#else>
                            <#if property.isModel()>
                            object.${property.alias}.add(Json.${property.getModel().id}.deserialize(context, reader));
                            <#else>
                            object.${property.alias}.add(reader.next${property.typeName}());
                            </#if>
                            </#if>
                        }
                        reader.endArray();
                        <#else>
                        <#if property.getCustomDeserializer()??>
                        object.${property.alias} = ${property.customDeserializer}(context, reader);
                        <#else>
                        <#if property.isModel()>
                        object.${property.alias} = Json.${property.getModel().id}.deserialize(context, reader);
                        <#else>
                        object.${property.alias} = reader.next${property.typeName}();
                        </#if>
                        </#if>
                        </#if>
                        break;
                    </#list>
                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();
            <#if model.deserializer??>
            object = ${model.deserializer}(context, object);
            </#if>
            return object;
        }
        </#if>

    }
    </#list>

}
