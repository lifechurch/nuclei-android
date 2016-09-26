/**
 * Copyright 2016 YouVersion
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.persistence.

import nuclei.persistence.apt.AptProcessor;

import org.junit.Test;

import com.google.testing.compile.JavaFileObjects;

import java.util.Arrays;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class IntentProcessorTests {

    @Test
    public void testIntents() {
        assert_().about(javaSources())
                .that(Arrays.asList(
                        JavaFileObjects.forSourceString("android.app.Activity",
                                "package android.app;\n" +
                                        "public class Activity {\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("android.annotation.TargetApi",
                                "package android.annotation;\n" +
                                        "public class TargetApi {\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("android.os.Bundle",
                                "package android.os;\n" +
                                        "public class Bundle {" +
                                        "   public boolean containsKey(String key) { return true; }" +
                                        "   public String getString(String key) { return null; }\n" +
                                        "   public java.util.ArrayList<String> getStringArrayList(String key) { return null; }\n" +
                                        "   public String[] getStringArray(String key) { return null; }\n" +
                                        "   public long getLong(String key) { return 0; }\n" +
                                        "   public void putLong(String key, long l) {  }\n" +
                                        "   public void putString(String key, String val) {}" +
                                        "   public void putStringArray(String key, String[] val) {}" +
                                        "   public void putStringArrayList(String key, java.util.ArrayList<String> val) {}" +
                                        "}"),
                        JavaFileObjects.forSourceString("android.content.Intent",
                                "package android.content;\n" +
                                        "public class Intent {\n" +
                                        "   public void putExtra(String key, String val) {}" +
                                        "   public void putExtra(String key, String[] val) {}" +
                                        "   public void putExtra(String key, java.util.ArrayList<String> val) {}" +
                                        "   public void putExtra(String key, long val) {}" +
                                        "}"),
                        JavaFileObjects.forSourceString("android.content.Context",
                                "package android.content;\n" +
                                        "public class Context {\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("android.support.JsonWriter",
                                "package android.support;\n" +
                                        "public class JsonWriter {\n" +
                                        "   public JsonWriter(Object r) {}" +
                                        "   public void nullValue() {}" +
                                        "   public void name(String n) {}" +
                                        "   public void value(Object v) {}" +
                                        "   public void beginObject() {}" +
                                        "   public void endObject() {}" +
                                        "   public void beginArray() {}" +
                                        "   public void endArray() {}" +
                                        "}"),
                        JavaFileObjects.forSourceString("android.support.JsonReader",
                                "package android.support;\n" +
                                        "public class JsonReader {\n" +
                                        "   public JsonReader(Object r) {}" +
                                        "   public void beginArray() {}" +
                                        "   public void endArray() {}" +
                                        "   public void beginObject() {}" +
                                        "   public void endObject() {}" +
                                        "   public void nextNull() {}" +
                                        "   public android.support.JsonToken peek() {return null;}" +
                                        "   public String nextString() {return null;}" +
                                        "   public long nextLong() {return 0;};" +
                                        "   public double nextDouble() {return 0;}" +
                                        "   public int nextInt() {return 0;}" +
                                        "   public int nextInteger() {return 0;}" +
                                        "   public boolean hasNext() {return true;}" +
                                        "   public String nextName() {return null;}" +
                                        "   public void skipValue() {}" +
                                        "}"),
                        JavaFileObjects.forSourceString("android.support.JsonToken",
                                "package android.support;\n" +
                                        "public class JsonToken {" +
                                        "   public static JsonToken NULL = new JsonToken();\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.persistence.intent.AbstractBinding",
                                "package nuclei.persistence.intent;\n" +
                                        "import android.annotation.TargetApi;\n" +
                                        "import android.app.Activity;\n" +
                                        "import android.content.Context;\n" +
                                        "import android.content.Intent;\n" +
                                        "import android.os.Bundle;\n" +
                                        "public abstract class AbstractBinding<T> {" +
                                        "   public abstract T toModel(Bundle bundle);\n" +
                                        "   public abstract Bundle toBundle(T model);\n" +
                                        "   public abstract void bind(Intent intent, T model);\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.persistence.activity.TestActivity",
                                "package nuclei.persistence.activity;\n" +
                                        "public class TestActivity extends android.app.Activity {\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.persistence.intent.Binding",
                                "package nuclei.persistence.intent;\n" +
                                        "import java.lang.annotation.Retention;\n" +
                                        "import java.lang.annotation.Target;\n" +
                                        "import static java.lang.annotation.ElementType.TYPE;\n" +
                                        "import static java.lang.annotation.ElementType.FIELD;\n" +
                                        "import static java.lang.annotation.RetentionPolicy.SOURCE;\n" +
                                        "import android.app.Activity;\n" +
                                        "@Retention(SOURCE)\n" +
                                        "@Target(TYPE)\n" +
                                        "public @interface Binding {\n" +
                                        "   Class<? extends Activity> activity() default Activity.class;\n" +
                                        "   String action() default \"\";\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.persistence.model.Model",
                                "package nuclei.persistence.model;\n" +
                                        "import java.lang.annotation.Retention;\n" +
                                        "import java.lang.annotation.Target;\n" +
                                        "import static java.lang.annotation.ElementType.TYPE;\n" +
                                        "import static java.lang.annotation.ElementType.FIELD;\n" +
                                        "import static java.lang.annotation.RetentionPolicy.SOURCE;\n" +
                                        "import android.app.Activity;\n" +
                                        "@Retention(SOURCE)\n" +
                                        "@Target(TYPE)\n" +
                                        "public @interface Model {" +
                                        "   String id();\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.persistence.model.ModelProperty",
                                "package nuclei.persistence.model;\n" +
                                        "import java.lang.annotation.Retention;\n" +
                                        "import java.lang.annotation.Target;\n" +
                                        "import static java.lang.annotation.ElementType.TYPE;\n" +
                                        "import static java.lang.annotation.ElementType.FIELD;\n" +
                                        "import static java.lang.annotation.RetentionPolicy.SOURCE;\n" +
                                        "import android.app.Activity;\n" +
                                        "@Retention(SOURCE)\n" +
                                        "@Target(FIELD)\n" +
                                        "public @interface ModelProperty {\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.persistence.intent.BindingProperty",
                                "package nuclei.persistence.intent;\n" +
                                        "import java.lang.annotation.Retention;\n" +
                                        "import java.lang.annotation.Target;\n" +
                                        "import static java.lang.annotation.ElementType.FIELD;\n" +
                                        "import static java.lang.annotation.RetentionPolicy.SOURCE;\n" +
                                        "@Retention(SOURCE)\n" +
                                        "@Target(FIELD)\n" +
                                        "public @interface BindingProperty {\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("HelloWorld",
                                "import nuclei.persistence.intent.Binding;\n" +
                                        "import nuclei.persistence.intent.BindingProperty;\n" +
                                        "@Binding(activity=nuclei.persistence.activity.TestActivity.class)\n" +
                                        "public class HelloWorld {\n" +
                                        "   @BindingProperty\n" +
                                        "   public String fieldString;\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.pkg.HelloWorld",
                                "package nuclei.pkg;\n" +
                                        "import nuclei.persistence.intent.Binding;\n" +
                                        "import nuclei.persistence.intent.BindingProperty;\n" +
                                        "@Binding\n" +
                                        "public class HelloWorld {\n" +
                                        "   @BindingProperty\n" +
                                        "   public String fieldString;\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("HelloWorldInner",
                                "import nuclei.persistence.intent.Binding;\n" +
                                        "import nuclei.persistence.intent.BindingProperty;\n" +
                                        "public class HelloWorldInner {\n" +
                                        "   @Binding\n" +
                                        "   public static class HelloWorldInnerClass {\n" +
                                        "       @BindingProperty\n" +
                                        "       public String fieldString;\n" +
                                        "   }\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.pkg.HelloWorldInner",
                                "package nuclei.pkg;\n" +
                                        "import nuclei.persistence.intent.Binding;\n" +
                                        "import nuclei.persistence.intent.BindingProperty;\n" +
                                        "public class HelloWorldInner {\n" +
                                        "   @Binding(activity=nuclei.persistence.activity.TestActivity.class)\n" +
                                        "   public static class HelloWorldInnerClass {\n" +
                                        "       @BindingProperty\n" +
                                        "       public String fieldString;\n" +
                                        "       @BindingProperty\n" +
                                        "       public String[] fieldStringArray;\n" +
                                        "       @BindingProperty\n" +
                                        "       public java.util.ArrayList<String> fieldStringList;\n" +
                                        "       @BindingProperty\n" +
                                        "       public long fieldLong;\n" +
                                        "       @BindingProperty\n" +
                                        "       public Long fieldLongObject;\n" +
                                        "   }\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.pkg.ModelObj",
                                "package nuclei.pkg;\n" +
                                        "import nuclei.persistence.model.Model;\n" +
                                        "import nuclei.persistence.model.ModelProperty;\n" +
                                        "@Model(id=\"ModelObjId\")\n" +
                                        "public class ModelObj {\n" +
                                        "   @ModelProperty\n" +
                                        "   public String fieldString;\n" +
                                        "   @ModelProperty\n" +
                                        "   public java.util.ArrayList<String> fieldStringArray;\n" +
                                        "   @ModelProperty\n" +
                                        "   public java.util.ArrayList<String> fieldStringList;\n" +
                                        "   @ModelProperty\n" +
                                        "   public long fieldLong;\n" +
                                        "   @ModelProperty\n" +
                                        "   public Long fieldLongObject;\n" +
                                        "   @ModelProperty\n" +
                                        "   public nuclei.pkg.ModelObj2 obj2;\n" +
                                        "}"),
                        JavaFileObjects.forSourceString("nuclei.pkg.ModelObj2",
                                "package nuclei.pkg;\n" +
                                        "import nuclei.persistence.model.Model;\n" +
                                        "import nuclei.persistence.model.ModelProperty;\n" +
                                        "@Model(id=\"ModelObj2Id\")\n" +
                                        "public class ModelObj2 {\n" +
                                        "   @ModelProperty\n" +
                                        "   public String fieldString;\n" +
                                        "   @ModelProperty\n" +
                                        "   public java.util.ArrayList<String> fieldStringArray;\n" +
                                        "   @ModelProperty\n" +
                                        "   public java.util.ArrayList<String> fieldStringList;\n" +
                                        "   @ModelProperty\n" +
                                        "   public long fieldLong;\n" +
                                        "   @ModelProperty\n" +
                                        "   public Long fieldLongObject;\n" +
                                        "}")))
                .processedWith(new AptProcessor())
                .compilesWithoutError();
    }

}
