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
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import nuclei3.logs.Log;
import nuclei3.logs.Logs;
import nuclei3.task.http.Http;
import nuclei3.task.http.HttpTask;

import java.util.Map;

/**
 * For GCM Backed Job Scheduling.  Will receive scheduled Job and execute.
 */
public class TaskWorker extends Worker {

    private static final Log LOG = Logs.newLog(TaskWorker.class);

    static TaskPool sTaskPool;

    public TaskWorker(@NonNull Context appContext, @NonNull WorkerParameters params) {
        super(appContext, params);
    }

    /**
     * Initialize this Job Service with the supplied TaskPool.  This must be called
     * in order for scheduled Jobs to be executed.
     *
     * @param taskPool
     */
    public static void initialize(TaskPool taskPool) {
        sTaskPool = taskPool;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            if (sTaskPool == null)
                throw new NullPointerException("TaskWorker TaskPool not set!");
            Map<String, Object> bundle = getInputData().getKeyValueMap();
            String taskName = (String) bundle.get(TaskScheduler.TASK_NAME);
            Task task = (Task) Class.forName(taskName).newInstance();
            ArrayMap<String, Object> map = new ArrayMap<>(bundle.size());
            for (String key : bundle.keySet()) {
                map.put(key, bundle.get(key));
            }
            task.deserialize(map);
            if (task instanceof HttpTask)
                Http.executeNow((HttpTask) task);
            else
                sTaskPool.executeNow(task);
            return Result.success();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
