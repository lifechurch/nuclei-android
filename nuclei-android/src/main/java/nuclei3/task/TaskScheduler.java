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
package nuclei3.task;

import android.content.Context;
import android.os.Build;
import androidx.collection.ArrayMap;
import androidx.work.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A class for scheduling tasks.  If API 21+, it will default to the JobScheduler service,
 * if Prior to API 21, it will fallback to GCM.
 *
 * Further configuration is required for this to work.
 *
 * See:<br />
 * https://developers.google.com/android/reference/com/google/android/gms/gcm/GcmNetworkManager<br />
 * And<br />
 * http://developer.android.com/reference/android/app/job/JobScheduler.html<br />
 * <br />
 * Two additional classes assist in these instructions:<br />
 * @see TaskWorker (for GCM support)
 * @see TaskJobService (for API 21+ support)
 */
public final class TaskScheduler {

    protected static final String TASK_NAME = "__task__name__";

    public static final int TASK_ONE_OFF = 1;
    public static final int TASK_PERIODIC = 2;

    public static final int BACKOFF_POLICY_LINEAR = 0;
    public static final int BACKOFF_POLICY_EXPONENTIAL = 1;

    private final Builder mBuilder;

    /**
     * Check to see if this device supports scheduling
     *
     * @return True if either API 21+ or Google Play Services are available
     */
    public static boolean supportsScheduling() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP || GoogleApiAvailability
                .getInstance()
                .isGooglePlayServicesAvailable(TaskPool.CONTEXT) == ConnectionResult.SUCCESS;
    }

    TaskScheduler(Builder builder) {
        mBuilder = builder;
    }

    public void schedule(Context context) {
        onScheduleJob(context);
    }

    public static void cancel(Context context, nuclei3.task.Task<?> task) {
        WorkManager.getInstance().cancelAllWorkByTag(task.getTaskTag());
    }

    private static void cancelPreL(Context context, nuclei3.task.Task<?> task) {
        WorkManager.getInstance().cancelAllWorkByTag(task.getTaskTag());
    }

    public static void cancelAll(Context context) {
        WorkManager.getInstance().cancelAllWork();
    }

    private void onScheduleJob(Context context) {
        WorkRequest.Builder<?, ?> builder;
        ArrayMap<String, Object> map = new ArrayMap<>();
        mBuilder.mTask.serialize(map);
        Data.Builder extras = new Data.Builder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object v = entry.getValue();
            if (v == null)
                continue;
            if (v instanceof Integer)
                extras.putInt(entry.getKey(), (int) v);
            else if (v instanceof Double)
                extras.putDouble(entry.getKey(), (double) v);
            else if (v instanceof Long)
                extras.putLong(entry.getKey(), (long) v);
            else if (v instanceof String)
                extras.putString(entry.getKey(), (String) v);
            else if (v instanceof String[])
                extras.putStringArray(entry.getKey(), (String[]) v);
            else if (v instanceof boolean[] && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                extras.putBooleanArray(entry.getKey(), (boolean[]) v);
            else if (v instanceof double[])
                extras.putDoubleArray(entry.getKey(), (double[]) v);
            else if (v instanceof long[])
                extras.putLongArray(entry.getKey(), (long[]) v);
            else if (v instanceof int[])
                extras.putIntArray(entry.getKey(), (int[]) v);
            else
                throw new IllegalArgumentException("Invalid Type: " + entry.getKey());
        }
        extras.putString(TASK_NAME, mBuilder.mTask.getClass().getName());

        switch (mBuilder.mTaskType) {
            case TASK_ONE_OFF:
                builder = new OneTimeWorkRequest.Builder(TaskWorker.class);
                ((OneTimeWorkRequest.Builder) builder).setInitialDelay(mBuilder.mPeriodInSeconds, TimeUnit.SECONDS);
                break;
            case TASK_PERIODIC:
                builder = new PeriodicWorkRequest.Builder(TaskWorker.class, mBuilder.mPeriodInSeconds, TimeUnit.SECONDS);
                break;
            default:
                throw new IllegalArgumentException();
        }
        builder.addTag(mBuilder.mTask.getTaskTag());
        builder.setInputData(extras.build());

        Constraints.Builder constraintsBuilder = new Constraints.Builder();
        if (mBuilder.mNetworkState != null) {
            constraintsBuilder.setRequiredNetworkType(mBuilder.mNetworkState);
        }
        constraintsBuilder.setRequiresCharging(mBuilder.mRequiresCharging);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            constraintsBuilder.setRequiresDeviceIdle(mBuilder.mRequiresDeviceIdle);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            switch (mBuilder.mBackoffPolicy) {
                case BACKOFF_POLICY_EXPONENTIAL:
                    builder.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.of(mBuilder.mInitialBackoffMillis, ChronoUnit.MILLIS));
                    break;
                case BACKOFF_POLICY_LINEAR:
                    builder.setBackoffCriteria(BackoffPolicy.LINEAR, Duration.of(mBuilder.mInitialBackoffMillis, ChronoUnit.MILLIS));
                    break;
            }
        }
        if (mBuilder.mUpdateCurrent)
            WorkManager.getInstance().cancelAllWorkByTag(mBuilder.mTask.getTaskTag());
        WorkManager.getInstance().enqueue(builder.build());
    }

    public static Builder newBuilder(nuclei3.task.Task task, int type) {
        return new Builder(task, type);
    }

    public static final class Builder {

        final nuclei3.task.Task mTask;
        NetworkType mNetworkState = null;
        boolean mRequiresDeviceIdle;
        boolean mRequiresCharging;
        boolean mUpdateCurrent;
        boolean mPersisted;
        int mTaskType = TASK_ONE_OFF;
        int mBackoffPolicy = -1;
        int mPeriodInSeconds;
        long mInitialBackoffMillis;

        Builder(nuclei3.task.Task task, int type) {
            mTask = task;
            mTaskType = type;
        }

        public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
            mRequiresDeviceIdle = requiresDeviceIdle;
            return this;
        }

        public Builder setRequiredNetwork(NetworkType state) {
            mNetworkState = state;
            return this;
        }

        public Builder setRequiresCharging(boolean requiresCharging) {
            mRequiresCharging = requiresCharging;
            return this;
        }

        public Builder setUpdateCurrent(boolean updateCurrent) {
            mUpdateCurrent = updateCurrent;
            return this;
        }

        public Builder setPersisted(boolean persisted) {
            mPersisted = persisted;
            return this;
        }

        public Builder setBackoffPolicy(int backoffPolicy) {
            mBackoffPolicy = backoffPolicy;
            return this;
        }

        public Builder setInitialBackoffMillis(long initialBackoffMillis) {
            mInitialBackoffMillis = initialBackoffMillis;
            return this;
        }

        public Builder setPeriodInSeconds(int periodInSeconds) {
            this.mPeriodInSeconds = periodInSeconds;
            return this;
        }

        public TaskScheduler build() {
            return new TaskScheduler(this);
        }

    }

}
