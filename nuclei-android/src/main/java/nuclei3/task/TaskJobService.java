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
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.BaseBundle;
import androidx.collection.ArrayMap;

import nuclei3.task.http.Http;
import nuclei3.task.http.HttpTask;
import nuclei3.logs.Log;
import nuclei3.logs.Logs;

/**
 * For API 21+ Job Scheduling.  Will receive scheduled jobs and execute on the initialized
 * Task Pool.
 *
 * @see #initialize(TaskPool)
 */
@TargetApi(21)
public class TaskJobService extends JobService {

    static final Log LOG = Logs.newLog(TaskJobService.class);

    static TaskPool sTaskPool;

    /**
     * Initialize this Job Service with the supplied TaskPool.  This must be called
     * in order for scheduled Jobs to be executed.
     *
     * @param taskPool
     */
    public static void initialize(TaskPool taskPool) {
        sTaskPool = taskPool;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onStartJob(JobParameters params) {
        try {
            if (sTaskPool == null)
                throw new NullPointerException("TaskJobService TaskPool not set!");
            BaseBundle bundle = params.getExtras();
            String taskName = bundle.getString(TaskScheduler.TASK_NAME);
            Task task = (Task) Class.forName(taskName).newInstance();
            ArrayMap<String, Object> map = new ArrayMap<>(bundle.size());
            for (String key : bundle.keySet()) {
                map.put(key, bundle.get(key));
            }
            task.deserialize(map);
            if (task instanceof HttpTask)
                Http.execute((HttpTask)task).addCallback(new JobCallback(params));
            else
                sTaskPool.execute(task).addCallback(new JobCallback(params));
            return true;
        } catch (Exception err) {
            LOG.e("Error running task", err);
            jobFinished(params, true);
            return false;
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    class JobCallback extends Result.CallbackAdapter {
        final JobParameters params;

        public JobCallback(JobParameters params) {
            this.params = params;
        }

        @Override
        public void onResult(Object type) {
            jobFinished(params, false);
        }

        @Override
        public void onException(Exception err) {
            jobFinished(params, false);
        }

    }

}
