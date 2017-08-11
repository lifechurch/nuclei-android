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
package nuclei.task;

import android.content.Context;
import android.support.v4.util.ArrayMap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import nuclei.logs.Log;
import nuclei.logs.Logs;

/**
 * An asynchronous Task to be used with a TaskPool and an optional
 * ContextHandle.
 * <br />
 * This generates a Result instance when passed to a TaskPool#execute(Task)
 */
public abstract class Task<T> implements Runnable {

    private static final Log LOG = Logs.newLog(Task.class);

    private static final AtomicInteger sJobId = new AtomicInteger(1);

    private TaskPool pool;
    private Result<T> result;
    private T taskResult;
    private Exception taskException;
    private boolean fromCache;
    private boolean resultSet;

    private final AtomicBoolean interrupted = new AtomicBoolean(false);
    private Thread currentThread;

    /**
     * Ability to change how we represent this task in logs
     *
     * @return
     */
    protected String getLogKey() {
        return getId();
    }

    public boolean isRunning() {
        return TaskPool.isRunning(pool, this);
    }

    /**
     * Execute the Task, if the ContextHandle is still valid
     *
     * @hide
     */
    public final void run() {
        currentThread = Thread.currentThread();
        try {
            if (interrupted.get()) {
                onException(new InterruptedException());
                return;
            }
            try {
                run(TaskPool.CONTEXT);
                if (!resultSet)
                    throw new IllegalStateException("onComplete and onException not called, one is required");
            } catch (Exception err) {
                LOG.e("unhandled exception", err);
                onException(err);
            } catch (Throwable err) {
                LOG.e("unhandled throwable", err);
                onException(new Exception(err));
            }
        } finally {
            currentThread = null;
        }
    }

    /**
     * Task ID for API 21+ JobScheduler
     */
    public int getTaskId() {
        return sJobId.getAndIncrement();
    }

    /**
     * Task ID for GCM Job Scheduler
     */
    public String getTaskTag() {
        return getId();
    }

    /**
     * Get a unique Task ID.  This helps prevent multiple tasks with the same ID from running simultaneously.
     *
     * @return The unique ID
     */
    public abstract String getId();

    /**
     * Run the task
     *
     * @param context The context from the handle
     */
    public abstract void run(Context context) throws Exception;

    public boolean isInterrupted() {
        return interrupted.get();
    }

    public void interrupt() {
        if (!interrupted.get()) {
            interrupted.set(true);
            if (currentThread != null && !currentThread.isInterrupted())
                currentThread.interrupt();
        }
    }

    /**
     * Inform the task that it has completed.
     */
    protected final void onComplete() {
        onComplete(null, false);
    }

    /**
     * Inform the task that it has completed.
     */
    protected final void onComplete(T result) {
        onComplete(result, false);
    }

    /**
     * Inform the task that it has completed.
     */
    protected final void onComplete(T result, boolean fromCache) {
        resultSet = true;
        this.taskResult = result;
        this.fromCache = fromCache;
    }

    /**
     * Inform the task that it has completed, but with an exception
     */
    protected final void onException(Exception exception) {
        resultSet = true;
        this.taskException = exception;
    }

    /**
     * Inform the task that it has been intercepted so that it can inform the intercepting task of
     * it's result and handle.
     *
     * @hide
     */
    protected final void onIntercepted(Task task) {
        LOG.i("Intercepted " + getLogKey() + " by " + task.getId());
        task.result = result;
        onDetach();
        onIntercepted();
    }

    /**
     * Inform the task that it has been discarded so that it can inform the intercepting task of
     * it's result and handle.
     *
     * @hide
     */
    protected final void onDiscarded(TaskPool.TaskRunnable runnable) {
        if (runnable == null)
            throw new NullPointerException("TaskRunnable can't be null");
        LOG.i("Discarding " + getLogKey());
        runnable.task = null;
        onDetach();
        onDiscarded();
    }

    protected final void deliverResult(TaskGcmService service) {
        if (service == null)
            throw new NullPointerException("TaskGcmService can't be null");
        onDetach();
        onResultDelivered();
    }

    /**
     * Deliver the task results to the Result
     *
     * @hide
     */
    protected final void deliverResult(TaskPool.TaskRunnable runnable) {
        if (runnable == null)
            throw new NullPointerException("TaskRunnable can't be null");
        runnable.task = null;
        if (!resultSet)
            throw new IllegalStateException("Result not set");
        if (taskException != null) {
            if (taskResult != null)
                result.onExceptionWithResult(taskException, taskResult, fromCache);
            else
                result.onException(taskException);
        } else
            result.onResult(taskResult, fromCache);
        onDetach();
        onResultDelivered();
    }

    protected void onIntercepted() {

    }

    protected void onDiscarded() {

    }

    protected void onResultDelivered() {

    }

    /**
     * Detach the task elements so the task can be recycled if necessary
     */
    private void onDetach() {
        result = null;
        resultSet = false;
        taskResult = null;
        taskException = null;
        fromCache = false;
        pool = null;
    }

    /**
     * Attach the ContextHandle to the Task and generate a Result
     *
     * @hide
     */
    protected final Result<T> attach(TaskPool pool) {
        if (this.pool != null)
            throw new IllegalStateException("Already attached");
        this.pool = pool;
        if (this.result == null)
            this.result = new Result<>(getId());
        return this.result;
    }

    final Result<T> deferredAttach() {
        this.result = new Result<>(getId());
        return this.result;
    }

    /**
     * Used to serialize data for scheduled Jobs
     *
     * @param bundle The Bundle that will hold the serialized data
     */
    protected void serialize(ArrayMap<String, Object> bundle) {

    }

    /**
     * Used to deserialize data for scheduled Jobs
     *
     * @param bundle The Bundle that will hold the serialized data
     */
    protected void deserialize(ArrayMap<String, Object> bundle) {

    }

}
