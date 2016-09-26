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
<#if binding.packageName??>
package ${binding.packageName};

</#if>
import nuclei.intent.AbstractBinding;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;

public class ${binding.name}Binding extends AbstractBinding<${binding.fullName}> {

    static final ${binding.name}Binding INSTANCE = new ${binding.name}Binding();

    public static ${binding.name}Binding instance() {
        return INSTANCE;
    }<#if cyto>

    public static nuclei.intent.ActivityIntent.Builder<${binding.fullName}> binding(android.app.Activity activity) {
        return new nuclei.intent.ActivityIntent.Builder(activity, INSTANCE);
    }

    public static nuclei.intent.ActivityIntent.Builder<${binding.fullName}> binding(android.support.v4.app.Fragment fragment) {
        return new nuclei.intent.ActivityIntent.Builder(fragment, INSTANCE);
    }

    public static nuclei.intent.ActivityIntent.Builder<${binding.fullName}> binding(android.app.Fragment fragment) {
        return new nuclei.intent.ActivityIntent.Builder(fragment, INSTANCE);
    }</#if>

    @Override
    public ${binding.fullName} toModel(Bundle bundle) {
        ${binding.fullName} model = new ${binding.fullName}();
        if (bundle != null) {
            <#list binding.properties as property>
            <#if !property.primitive>
            if (bundle.containsKey("${property.name}"))
                model.${property.name} = bundle.${property.bundleGetter}
            <#else>
            model.${property.name} = bundle.${property.bundleGetter}
            </#if>
            </#list>
        }
        return model;
    }

    public Intent toIntent(Context context, ${binding.fullName} model) {
    <#if binding.attributes.activity??>
        Intent intent = new Intent(context, ${binding.attributes.activity}.class);
        if (model != null)
            bind(intent, model);
        return intent;
    <#else>
        throw new UnsupportedOperationException("Activity not defined on annotation");
    </#if>
    }<#if binding.attributes.action??>

    public Intent toIntent(${binding.fullName} model) {
        Intent intent = new Intent("${binding.attributes.action}");
        if (model != null)
            bind(intent, model);
        return intent;
    }
    </#if><#if binding.attributes.filter??>

    static final nuclei.persistence.intent.IntentFilter<T> FILTER = new nuclei.persistence.intent.IntentFilter<>();</#if>

    @Override
    public boolean filter(Context context, Intent intent) { <#if binding.attributes.filter??>
        return FILTER.filter(context, activityIntent, intent);<#else>
        return true;</#if>
    }

    @Override
    public void bind(Intent intent, ${binding.fullName} model) {
        <#list binding.properties as property>
        <#if !property.primitive>
        if (model.${property.name} != null)
            intent.putExtra("${property.name}", model.${property.name});
        <#else>
        intent.putExtra("${property.name}", model.${property.name});
        </#if>
        </#list>
    }

    @Override
    public Bundle toBundle(${binding.fullName} model) {
        Bundle bundle = new Bundle();
        <#list binding.properties as property>
        <#if !property.primitive>
        if (model.${property.name} != null)
            bundle.${property.bundleSetter}("${property.name}", model.${property.name});
        <#else>
        bundle.${property.bundleSetter}("${property.name}", model.${property.name});
        </#if>
        </#list>
        return bundle;
    }

}