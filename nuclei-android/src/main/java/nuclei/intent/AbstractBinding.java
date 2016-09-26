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
package nuclei.intent;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public abstract class AbstractBinding<T> {

    public abstract T toModel(Bundle bundle);

    public abstract Bundle toBundle(T model);

    public abstract void bind(Intent intent, T model);

    public T toModel(android.support.v4.app.Fragment fragment) {
        if (fragment == null)
            return null;
        Bundle bundle = fragment.getArguments();
        if (bundle == null && fragment.getActivity() != null) {
            Activity activity = fragment.getActivity();
            if (activity.getIntent() != null)
                bundle = activity.getIntent().getExtras();
        }
        return toModel(bundle);
    }

    @TargetApi(11)
    public T toModel(android.app.Fragment fragment) {
        if (fragment == null)
            return null;
        Bundle bundle = fragment.getArguments();
        if (bundle == null && fragment.getActivity() != null) {
            Activity activity = fragment.getActivity();
            if (activity.getIntent() != null)
                bundle = activity.getIntent().getExtras();
        }
        return toModel(bundle);
    }

    public Intent toIntent(Context context, Class<? extends Activity> cls, T model) {
        Intent intent = new Intent(context, cls);
        if (model != null) {
            bind(intent, model);
        }
        return intent;
    }

    public abstract Intent toIntent(Context context, T model);

    public T toModel(Activity activity) {
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null)
                return toModel(intent.getExtras());
        }
        return null;
    }

    public abstract boolean filter(Context context, Intent intent);

}
