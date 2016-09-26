package nuclei.task;

import android.app.Application;

public final class Tasks {

    private static TaskPool sDefault;

    private Tasks() {
    }

    public static void initialize(TaskPool pool) {
        if (!pool.getName().equals(TaskPool.DEFAULT_POOL))
            throw new IllegalArgumentException("Invalid Pool Name: " + pool.getName());
        sDefault = pool;
    }

    public static void initialize(Application application) {
        sDefault = TaskPool.newBuilder(TaskPool.DEFAULT_POOL).build();
    }

    public static TaskPool get(String name) {
        return TaskPool.TASK_POOLS.get(name);
    }

    public static <T> Result<T> execute(Task<T> task) {
        return sDefault.execute(task);
    }

    public static <T> Result<T> execute(ContextHandle handle, Task<T> task) {
        return sDefault.execute(handle, task);
    }

    public static <T> T executeNow(Task<T> task) {
        return sDefault.executeNow(task);
    }

    public static <T> Result<T> executeNowResult(Task<T> task) {
        return sDefault.executeNowResult(task);
    }

    public static <T> Result<T> execute(TaskRunnable<T> runnable) {
        RunnableTask<T> task = new RunnableTask<>(runnable);
        return sDefault.execute(task);
    }

    public static <T> T executeNow(TaskRunnable<T> runnable) {
        RunnableTask<T> task = new RunnableTask<>(runnable);
        return sDefault.executeNow(task);
    }

    public static <T> Result<T> executeNowResult(TaskRunnable<T> runnable) {
        RunnableTask<T> task = new RunnableTask<>(runnable);
        return sDefault.executeNowResult(task);
    }

}
