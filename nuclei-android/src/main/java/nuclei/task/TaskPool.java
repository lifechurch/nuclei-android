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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import nuclei.logs.Log;
import nuclei.logs.Logs;

/**
 * A task pool to execute a set of tasks.
 *
 * When two tasks with the same ID are attempted to run simultaneously, the first
 * executing task goes first, and once it's complete, the other task is allowed to run.
 */
public final class TaskPool implements Handler.Callback {

    public static final String DEFAULT_POOL = "NeuronDefault";
    public static final String HTTP_POOL = "NeuronHttp";

    static final int MESSAGE_RESULT = 1;
    static final int MESSAGE_QUEUE = 2;
    static final int MESSAGE_QUEUE_FAILED = 3;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    static final int DEFAULT_POOL_SIZE = CPU_COUNT * 2 + 1;

    static final Log LOG = Logs.newLog(TaskPool.class);

    static final Map<String, TaskPool> TASK_POOLS = new ConcurrentHashMap<>();

    static TaskPoolListener LISTENER;
    static Context CONTEXT;

    public static void initialize(Context context) {
        CONTEXT = context.getApplicationContext();
    }

    public static void setListener(TaskPoolListener listener) {
        LISTENER = listener;
    }

    private final Pools.SimplePool<TaskRunnable> taskRunnablePool;
    final Pools.SimplePool<Queue<TaskRunnable>> taskQueues;
    final Handler handler;
    private final ThreadPoolExecutor poolExecutor;

    private final String name;
    final List<TaskInterceptor> interceptors;

    private TaskListener listener;

    TaskPool(Looper mainLooper, final String name, int maxThreads, List<TaskInterceptor> interceptors) {
        this.name = name;
        TASK_POOLS.put(name, this);
        this.interceptors = interceptors;
        handler = new Handler(mainLooper, this);
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(128);
        taskRunnablePool = new Pools.SimplePool<>(maxThreads);
        taskQueues = new Pools.SimplePool<>(10);
        maxThreads = Math.max(CORE_POOL_SIZE, maxThreads);
        poolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, maxThreads, 1,
                TimeUnit.SECONDS, workQueue, new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);
            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, name + " #" + mCount.incrementAndGet());
            }
        });

        if (LISTENER != null)
            LISTENER.onCreated(this);
    }

    public String getName() {
        return name;
    }

    public void setListener(TaskListener listener) {
        this.listener = listener;
    }

    public static int getAllRunningCount() {
        int count = 0;
        for (TaskPool pool : TASK_POOLS.values()) {
            count += pool.getRunningCount();
        }
        return count;
    }

    public static int getAllPendingCount() {
        int count = 0;
        for (TaskPool pool : TASK_POOLS.values()) {
            count += pool.getPendingCount();
        }
        return count;
    }

    public static void setAllListener(TaskListener listener) {
        for (TaskPool pool : TASK_POOLS.values()) {
            pool.setListener(listener);
        }
    }

    public Set<String> getRunningIds() {
        synchronized (runningIds) {
            return new HashSet<>(runningIds.keySet());
        }
    }

    public int getRunningCount() {
        synchronized (runningIds) {
            return runningIds.size();
        }
    }

    public int getPendingCount() {
        int count = 0;
        synchronized (runningIds) {
            for (int i = 0; i < pendingTasks.size(); i++) {
                Queue<TaskRunnable> pending = pendingTasks.valueAt(i);
                if (pending != null)
                    count += pending.size();
            }
        }
        return count;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_QUEUE: {
                TaskRunnable runnable;
                if (msg.obj instanceof TaskRunnable) {
                    runnable = (TaskRunnable) msg.obj;
                } else {
                    runnable = toRunnable((Task<?>) msg.obj);
                }
                try {
                    poolExecutor.execute(runnable);
                } catch (RejectedExecutionException err) {
                    LOG.e("Error dispatching", err);
                    runnable.task.onException(err);
                    runnable.task.deliverResult(runnable);
                    taskRunnablePool.release(runnable);
                }
                break;
            }
            case MESSAGE_QUEUE_FAILED: {
                TaskRunnable runnable = (TaskRunnable) msg.obj;
                if (runnable.task != null)
                    runnable.task.deliverResult(runnable);
                taskRunnablePool.release(runnable);
                break;
            }
            case MESSAGE_RESULT: {
                TaskRunnable runnable = (TaskRunnable) msg.obj;
                if (runnable.task != null)
                    runnable.task.deliverResult(runnable);
                taskRunnablePool.release(runnable);
                break;
            }
        }
        return true;
    }

    private final TaskRunnable EMPTY = new TaskRunnable();

    /**
     * Execute the task on the current thread and without validation that it is unique.
     *
     * This is most useful if you need to execute a task from within a task.
     */
    public <T> T executeNow(Task<T> task) {
        return executeNowResult(task).get();
    }

    /**
     * Execute the task on the current thread and without validation that it is unique.
     *
     * This is most useful if you need to execute a task from within a task.
     */
    public <T> Result<T> executeNowResult(Task<T> task) {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new IllegalStateException("Cannot run on main thread");
        final String logKey = task.getLogKey();
        if (LOG.isLoggable(Log.INFO))
            LOG.i("Running task NOW (" + logKey + ")");
        long start = System.currentTimeMillis();
        Result<T> result = task.attach(this);
        task.run();
        if (LOG.isLoggable(Log.INFO)) {
            LOG.i("Took " + (System.currentTimeMillis() - start)
                    + "ms to run NOW, " + (System.currentTimeMillis() - start) + "ms total to execute (" + logKey + ")");
        }
        task.deliverResult(EMPTY);
        return result;
    }

    public <T> Result<T> execute(Task<T> task) {
        Result<T> result = task.attach(this);
        if (handler.getLooper() != Looper.myLooper()) {
            handler.obtainMessage(MESSAGE_QUEUE, task).sendToTarget();
            return result;
        }
        TaskRunnable runnable = toRunnable(task);
        try {
            poolExecutor.execute(runnable);
        } catch (RejectedExecutionException err) {
            LOG.e("Error dispatching", err);
            task.onException(err);
            handler.obtainMessage(MESSAGE_QUEUE_FAILED, runnable).sendToTarget();
        }
        return result;
    }

    private TaskRunnable toRunnable(Task<?> task) {
        TaskRunnable runnable = taskRunnablePool.acquire();
        if (runnable == null)
            runnable = new TaskRunnable();
        runnable.task = task;
        runnable.start = System.currentTimeMillis();
        return runnable;
    }

    public boolean isShutdown() {
        return poolExecutor.isShutdown();
    }

    public void shutdown() {
        poolExecutor.shutdown();
        TASK_POOLS.remove(name);
        if (LISTENER != null)
            LISTENER.onShutdown(this);
    }

    public void shutdownNow() {
        poolExecutor.shutdownNow();
        TASK_POOLS.remove(name);
        if (LISTENER != null)
            LISTENER.onShutdown(this);
    }

    public static Builder newBuilder(String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final String name;
        private int maxThreads = DEFAULT_POOL_SIZE;
        private Looper mainLooper = Looper.getMainLooper();
        private List<TaskInterceptor> interceptors;

        Builder(String name) {
            this.name = name;
        }

        /**
         * Specify the max threads this pool can use
         *
         * @param max The max number of threads
         */
        public Builder withThreads(int max) {
            maxThreads = max;
            return this;
        }

        /**
         * Specify which looper dispatching of tasks should be handled on
         *
         * @param mainLooper
         */
        public Builder withLooper(Looper mainLooper) {
            this.mainLooper = mainLooper;
            return this;
        }

        /**
         * Specify a list of interceptors
         *
         * @param interceptors
         */
        public Builder withInterceptors(List<TaskInterceptor> interceptors) {
            if (this.interceptors != null)
                this.interceptors.addAll(interceptors);
            else
                this.interceptors = new ArrayList<>(interceptors);
            return this;
        }

        /**
         * Specify an interceptor
         *
         * @param interceptor
         */
        public Builder withInterceptor(TaskInterceptor interceptor) {
            if (interceptors == null)
                interceptors = new ArrayList<>();
            interceptors.add(interceptor);
            return this;
        }

        /**
         * Build the TaskPool
         */
        public TaskPool build() {
            return new TaskPool(mainLooper, name, maxThreads, interceptors);
        }

    }

    final ArrayMap<String, Task<?>> runningIds = new ArrayMap<>();
    final ArrayMap<String, Queue<TaskRunnable>> pendingTasks = new ArrayMap<>();

    public static boolean isRunning(TaskPool pool, Task task) {
        if (pool == null || task == null)
            return false;
        synchronized (pool.runningIds) {
            return pool.runningIds.containsKey(task.getId());
        }
    }

    public static boolean isRunning(Task task) {
        if (task == null)
            return false;
        for (TaskPool pool : TASK_POOLS.values()) {
            synchronized (pool.runningIds) {
                if (pool.runningIds.containsKey(task.getId()))
                    return true;
            }
        }
        return false;
    }

    class TaskRunnable implements Runnable {

        Task task;
        long start;

        TaskRunnable() {
        }

        @Override
        public void run() {
            final String taskId = task.getId();
            final String logKey = task.getLogKey();
            if (LOG.isLoggable(Log.INFO))
                LOG.i("Running task (" + logKey + ")");
            synchronized (runningIds) {
                if (runningIds.containsKey(taskId)) {
                    if (LOG.isLoggable(Log.INFO))
                        LOG.i("Already pending (" + logKey + "), queuing request");
                    Queue<TaskRunnable> tasks = pendingTasks.get(taskId);
                    if (tasks == null) {
                        tasks = taskQueues.acquire();
                        if (tasks == null)
                            tasks = new ArrayDeque<>();
                        pendingTasks.put(taskId, tasks);
                    }
                    tasks.add(this);
                    return;
                }
                runningIds.put(taskId, task);
                if (listener != null)
                    listener.onStart(task);
            }
            long start = System.currentTimeMillis();
            if (interceptors != null) {
                for (TaskInterceptor interceptor : interceptors) {
                    Task intercepted = interceptor.intercept(task);
                    if (intercepted != task) {
                        if (intercepted != null) {
                            task.onIntercepted(intercepted);
                            Task oldTask = task;
                            task = intercepted;
                            synchronized (runningIds) {
                                runningIds.put(taskId, task);
                                if (listener != null)
                                    listener.onIntercepted(oldTask, task);
                            }
                        } else {
                            task.onDiscarded(this);
                            synchronized (runningIds) {
                                if (listener != null)
                                    listener.onDiscard(task);
                            }
                            task = null;
                            break;
                        }
                    }
                }
            }
            if (task != null) {
                try {
                    task.run();
                } finally {
                    synchronized (runningIds) {
                        if (listener != null)
                            listener.onFinish(task);
                    }
                }
                if (LOG.isLoggable(Log.INFO)) {
                    LOG.i("Took " + (System.currentTimeMillis() - start)
                            + "ms to run, " + (System.currentTimeMillis() - this.start) + "ms total to execute (" + logKey + ")");
                }
            }
            TaskRunnable next = null;
            synchronized (runningIds) {
                Queue<TaskRunnable> pending = pendingTasks.get(taskId);
                if (pending != null) {
                    next = pending.poll();
                    if (pending.isEmpty()) {
                        pendingTasks.remove(taskId);
                        taskQueues.release(pending);
                    }
                    runningIds.remove(taskId);
                } else {
                    runningIds.remove(taskId);
                }
            }
            if (next != null) {
                if (LOG.isLoggable(Log.INFO))
                    LOG.i("Already pending (" + logKey + "), sending request");
                handler.obtainMessage(MESSAGE_QUEUE, next).sendToTarget();
            }
            handler.obtainMessage(MESSAGE_RESULT, this).sendToTarget();
        }

    }

}
