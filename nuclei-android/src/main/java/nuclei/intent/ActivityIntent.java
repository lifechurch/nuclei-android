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
package nuclei.intent;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;

/**
 * ActivityIntent makes starting an activity easy.
 * <br />
 * It provides ways to consistently start activities based upon Intent Bindings.
 * <br />
 * It provides ways to use transitions from fragments when starting for result.
 *
 * @see Binding
 * @param <T> The type of the model used to bind to the intent
 */
public final class ActivityIntent<T> {

    private Fragment supportFragment;
    private android.app.Fragment fragment;
    private Activity activity;
    private AbstractBinding<T> binding;
    private ActivityOptionsCompat options;
    private int requestCode;
    private int resultCode;
    private T model;

    private ActivityIntent(T model, Fragment supportFragment, android.app.Fragment fragment, Activity activity,
                           ActivityOptionsCompat options, AbstractBinding<T> binding, int requestCode, int resultCode) {
        this.model = model;
        this.options = options;
        this.supportFragment = supportFragment;
        this.fragment = fragment;
        this.activity = activity;
        this.binding = binding;
        this.requestCode = requestCode;
        this.resultCode = resultCode;
    }

    public Activity getActivity() {
        return activity;
    }

    public AbstractBinding<T> getBinding() {
        return binding;
    }

    private boolean supportStartActivityForResult(Intent intent) {
        if (supportFragment != null) {
            if (options != null && activity instanceof IntentBuilderActivity) {
                ((IntentBuilderActivity) activity).setDefaultActivityOptions(options);
            }
            supportFragment.startActivityForResult(intent, requestCode);
            return true;
        }
        return false;
    }

    @TargetApi(11)
    private boolean startActivityForResult(Intent intent) {
        if (fragment != null) {
            if (Build.VERSION.SDK_INT >= 16) {
                if (options != null)
                    fragment.startActivityForResult(intent, requestCode, options.toBundle());
                else
                    fragment.startActivityForResult(intent, requestCode);
            } else {
                if (options != null && activity instanceof IntentBuilderActivity)
                    ((IntentBuilderActivity) activity).setDefaultActivityOptions(options);
                fragment.startActivityForResult(intent, requestCode);
            }
            return true;
        } else {
            return supportStartActivityForResult(intent);
        }
    }

    private boolean supportStartActivity(Intent intent) {
        if (supportFragment != null) {
            if (options != null && activity instanceof IntentBuilderActivity)
                ((IntentBuilderActivity) activity).setDefaultActivityOptions(options);
            supportFragment.startActivity(intent);
            return true;
        }
        return false;
    }

    @TargetApi(11)
    private boolean startActivity(Intent intent) {
        if (fragment != null) {
            if (options != null && activity instanceof IntentBuilderActivity)
                ((IntentBuilderActivity) activity).setDefaultActivityOptions(options);
            fragment.startActivity(intent);
            return true;
        } else {
            return supportStartActivity(intent);
        }
    }

    /**
     * Start an activity
     *
     * @return The generated Intent that was used to start the activity
     */
    public Intent startActivity() {
        Intent intent = binding.toIntent(activity, model);
        if (!binding.filter(activity, intent))
            return intent;
        if (Build.VERSION.SDK_INT >= 11) {
            if (startActivity(intent))
                return intent;
        } else {
            if (supportStartActivity(intent))
                return intent;
        }
        if (options != null)
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        else
            activity.startActivity(intent);
        return intent;
    }

    /**
     * Starts an activity for a result
     *
     * @return The generated Intent that was used to start the activity
     */
    public Intent startActivityForResult() {
        Intent intent = binding.toIntent(activity, model);
        if (!binding.filter(activity, intent))
            return intent;
        if (Build.VERSION.SDK_INT >= 11) {
            if (startActivityForResult(intent))
                return intent;
        } else {
            if (supportStartActivityForResult(intent))
                return intent;
        }
        if (options != null)
            ActivityCompat.startActivityForResult(activity, intent, requestCode, options.toBundle());
        else
            activity.startActivityForResult(intent, requestCode);
        return intent;
    }

    /**
     * Starts an Activity.  If a requestCode was used during the building of the ActivityIntent,
     * startActivityForResult is called.  Otherwise startActivity is called.
     *
     * @return The Intent used to start the activity
     */
    public Intent start() {
        if (requestCode > 0)
            return startActivityForResult();
        return startActivity();
    }

    private boolean supportFinishForResult(Intent intent) {
        if (supportFragment != null) {
            if (supportFragment.getTargetFragment() != null)
                supportFragment.getTargetFragment()
                    .onActivityResult(supportFragment.getTargetRequestCode(), resultCode, intent);
            else {
                activity.setResult(resultCode, intent);
                ActivityCompat.finishAfterTransition(activity);
            }
            return true;
        }
        return false;
    }

    @TargetApi(11)
    private boolean finishForResult(Intent intent) {
        if (fragment != null) {
            if (fragment.getTargetFragment() != null)
                fragment.getTargetFragment()
                    .onActivityResult(fragment.getTargetRequestCode(), resultCode, intent);
            else
                supportFinishForResult(intent);
            return true;
        } else {
            return supportFinishForResult(intent);
        }
    }

    /**
     * Finish the current activity or fragment.  Using the model to bind to the Intent
     * that is used as the data result.
     *
     * @param model The model used to as the data result
     * @return The Intent used as the data result
     */
    public Intent finishForResult(T model) {
        Intent intent = binding.toIntent(activity, model);
        if (!binding.filter(activity, intent))
            return intent;
        if (Build.VERSION.SDK_INT >= 11) {
            if (finishForResult(intent))
                return intent;
        } else {
            if (supportFinishForResult(intent))
                return intent;
        }
        activity.setResult(resultCode, intent);
        ActivityCompat.finishAfterTransition(activity);
        return intent;
    }

    public static final class Builder<T> {

        private Fragment supportFragment;
        private android.app.Fragment fragment;
        private Activity activity;
        private AbstractBinding<T> binding;
        private int requestCode;
        private int resultCode;
        private T model;
        private ActivityOptionsCompat options;

        public Builder(Activity activity, AbstractBinding<T> binding) {
            this.activity = activity;
            this.binding = binding;
        }

        public Builder(Fragment fragment, AbstractBinding<T> binding) {
            this.supportFragment = fragment;
            this.activity = fragment.getActivity();
            this.binding = binding;
        }

        @TargetApi(11)
        public Builder(android.app.Fragment fragment, AbstractBinding<T> binding) {
            this.fragment = fragment;
            this.activity = fragment.getActivity();
            this.binding = binding;
        }

        public Builder<T> options(ActivityOptionsCompat options) {
            this.options = options;
            return this;
        }

        public Builder<T> requestCode(int requestCode) {
            this.requestCode = requestCode;
            return this;
        }

        public Builder<T> resultCode(int resultCode) {
            this.resultCode = resultCode;
            return this;
        }

        public Builder<T> model(T model) {
            this.model = model;
            return this;
        }

        public ActivityIntent<T> intent() {
            return new ActivityIntent<T>(model, supportFragment, fragment, activity, options, binding, requestCode, resultCode);
        }

    }


}
