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
        TaskPool.initialize(application);
    }

    public static TaskPool get() {
        return sDefault;
    }

    public static TaskPool get(String name) {
        return TaskPool.TASK_POOLS.get(name);
    }

    public static <T> Result<T> execute(Task<T> task) {
        return sDefault.execute(task);
    }

    public static <T> T executeNow(Task<T> task) {
        return sDefault.executeNow(task);
    }

    public static <T> Result<T> executeNowResult(Task<T> task) {
        return sDefault.executeNowResult(task);
    }

    public static <T> Result<T> execute(String id, TaskRunnable<T> runnable) {
        RunnableTask<T> task = new RunnableTask<>(id, runnable);
        return sDefault.execute(task);
    }

    public static <T> T executeNow(String id, TaskRunnable<T> runnable) {
        RunnableTask<T> task = new RunnableTask<>(id, runnable);
        return sDefault.executeNow(task);
    }

    public static <T> Result<T> executeNowResult(String id, TaskRunnable<T> runnable) {
        RunnableTask<T> task = new RunnableTask<>(id, runnable);
        return sDefault.executeNowResult(task);
    }

}
