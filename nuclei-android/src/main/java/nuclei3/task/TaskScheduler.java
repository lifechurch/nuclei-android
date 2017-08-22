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
package nuclei3.task;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.v4.util.ArrayMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.*;
import com.google.android.gms.gcm.Task;

import java.io.Serializable;
import java.util.Map;

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
 * @see TaskGcmService (for GCM support)
 * @see TaskJobService (for API 21+ support)
 */
public final class TaskScheduler {

    protected static final String TASK_NAME = "__task__name__";

    public static final int NETWORK_STATE_ANY = 1;
    public static final int NETWORK_STATE_CONNECTED = 2;
    public static final int NETWORK_STATE_UNMETERED = 3;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !mBuilder.mForceGcm) {
            onScheduleJobL(context);
        } else {
            onSchedulePreL(context);
        }
    }

    public static void cancel(Context context, nuclei3.task.Task<?> task, boolean forceGcm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !forceGcm) {
            cancelL(context, task);
        } else {
            cancelPreL(context, task);
        }
    }

    public static void cancel(Context context, nuclei3.task.Task<?> task) {
        cancel(context, task, false);
    }

    private static void cancelPreL(Context context, nuclei3.task.Task<?> task) {
        GcmNetworkManager.getInstance(context)
                .cancelTask(task.getTaskTag(), TaskGcmService.class);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void cancelL(Context context, nuclei3.task.Task<?> task) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(task.getTaskId());
    }

    public static void cancelAll(Context context, boolean forceGcm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !forceGcm) {
            cancelAllL(context);
        } else {
            cancelAllPreL(context);
        }
    }

    public static void cancelAll(Context context) {
        cancelAll(context, false);
    }

    private static void cancelAllPreL(Context context) {
        GcmNetworkManager.getInstance(context)
                .cancelAllTasks(TaskGcmService.class);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void cancelAllL(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancelAll();
    }

    private void onSchedulePreL(Context context) {
        Task.Builder builder;

        switch (mBuilder.mTaskType) {
            case TASK_ONE_OFF:
                OneoffTask.Builder oneOffBuilder = new OneoffTask.Builder();
                builder = oneOffBuilder;
                if (mBuilder.mWindowStartDelaySecondsSet || mBuilder.mWindowEndDelaySecondsSet)
                    oneOffBuilder.setExecutionWindow(mBuilder.mWindowStartDelaySeconds, mBuilder.mWindowEndDelaySeconds);
                break;
            case TASK_PERIODIC:
                builder = new PeriodicTask.Builder()
                        .setFlex(mBuilder.mFlexInSeconds)
                        .setPeriod(mBuilder.mPeriodInSeconds);
                break;
            default:
                throw new IllegalArgumentException();
        }

        ArrayMap<String, Object> map = new ArrayMap<>();
        mBuilder.mTask.serialize(map);
        Bundle extras = new Bundle();
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
            else if (v instanceof boolean[])
                extras.putBooleanArray(entry.getKey(), (boolean[]) v);
            else if (v instanceof double[])
                extras.putDoubleArray(entry.getKey(), (double[]) v);
            else if (v instanceof long[])
                extras.putLongArray(entry.getKey(), (long[]) v);
            else if (v instanceof int[])
                extras.putIntArray(entry.getKey(), (int[]) v);
            else if (v instanceof Parcelable)
                extras.putParcelable(entry.getKey(), (Parcelable) v);
            else if (v instanceof Serializable)
                extras.putSerializable(entry.getKey(), (Serializable) v);
            else
                throw new IllegalArgumentException("Invalid Type: " + entry.getKey());
        }
        extras.putString(TASK_NAME, mBuilder.mTask.getClass().getName());

        builder.setExtras(extras)
                .setPersisted(mBuilder.mPersisted)
                .setRequiresCharging(mBuilder.mRequiresCharging)
                .setService(TaskGcmService.class)
                .setTag(mBuilder.mTask.getTaskTag())
                .setUpdateCurrent(mBuilder.mUpdateCurrent);

        switch (mBuilder.mNetworkState) {
            case NETWORK_STATE_ANY:
                builder.setRequiredNetwork(Task.NETWORK_STATE_ANY);
                break;
            case NETWORK_STATE_CONNECTED:
                builder.setRequiredNetwork(Task.NETWORK_STATE_CONNECTED);
                break;
            case NETWORK_STATE_UNMETERED:
                builder.setRequiredNetwork(Task.NETWORK_STATE_UNMETERED);
                break;
        }

        GcmNetworkManager.getInstance(context)
                .schedule(builder.build());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onScheduleJobL(Context context) {
        JobInfo.Builder builder = new JobInfo.Builder(mBuilder.mTask.getTaskId(), new ComponentName(context, TaskJobService.class));

        ArrayMap<String, Object> map = new ArrayMap<>();
        mBuilder.mTask.serialize(map);
        PersistableBundle extras = new PersistableBundle();
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
                if (mBuilder.mWindowStartDelaySecondsSet)
                    builder.setMinimumLatency(mBuilder.mWindowStartDelaySeconds * 1000);
                if (mBuilder.mWindowEndDelaySecondsSet)
                    builder.setOverrideDeadline(mBuilder.mWindowEndDelaySeconds * 1000);
                break;
            case TASK_PERIODIC:
                builder.setPeriodic(mBuilder.mPeriodInSeconds * 1000);
                break;
            default:
                throw new IllegalArgumentException();
        }

        builder.setExtras(extras)
                .setPersisted(mBuilder.mPersisted)
                .setRequiresCharging(mBuilder.mRequiresCharging)
                .setRequiresDeviceIdle(mBuilder.mRequiresDeviceIdle);

        switch (mBuilder.mNetworkState) {
            case NETWORK_STATE_ANY:
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);
                break;
            case NETWORK_STATE_CONNECTED:
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
                break;
            case NETWORK_STATE_UNMETERED:
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
                break;
        }

        switch (mBuilder.mBackoffPolicy) {
            case BACKOFF_POLICY_EXPONENTIAL:
                builder.setBackoffCriteria(mBuilder.mInitialBackoffMillis, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
                break;
            case BACKOFF_POLICY_LINEAR:
                builder.setBackoffCriteria(mBuilder.mInitialBackoffMillis, JobInfo.BACKOFF_POLICY_LINEAR);
                break;
        }

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }

    public static Builder newBuilder(nuclei3.task.Task task, int type) {
        return new Builder(task, type);
    }

    public static final class Builder {

        final nuclei3.task.Task mTask;
        int mNetworkState = NETWORK_STATE_ANY;
        boolean mRequiresDeviceIdle;
        boolean mRequiresCharging;
        boolean mUpdateCurrent;
        boolean mPersisted;
        long mWindowStartDelaySeconds = -1L;
        boolean mWindowStartDelaySecondsSet;
        long mWindowEndDelaySeconds = -1L;
        boolean mWindowEndDelaySecondsSet;
        long mFlexInSeconds;
        long mPeriodInSeconds;
        int mTaskType = TASK_ONE_OFF;
        int mBackoffPolicy = -1;
        long mInitialBackoffMillis;
        boolean mForceGcm;

        Builder(nuclei3.task.Task task, int type) {
            mTask = task;
            mTaskType = type;
        }

        public Builder setFlex(long flexInSeconds) {
            mFlexInSeconds = flexInSeconds;
            return this;
        }

        public Builder setPeriod(long periodInSeconds) {
            mPeriodInSeconds = periodInSeconds;
            return this;
        }

        public Builder setWindowDelay(long windowStartDelaySeconds, long windowEndDelaySeconds) {
            mWindowStartDelaySeconds = windowStartDelaySeconds;
            mWindowStartDelaySecondsSet = true;
            mWindowEndDelaySeconds = windowEndDelaySeconds;
            mWindowEndDelaySecondsSet = true;
            return this;
        }

        public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
            mRequiresDeviceIdle = requiresDeviceIdle;
            return this;
        }

        public Builder setRequiredNetwork(int state) {
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

        public Builder setForceGcm(boolean forceGcm) {
            mForceGcm = forceGcm;
            return this;
        }

        public TaskScheduler build() {
            return new TaskScheduler(this);
        }

    }

}
