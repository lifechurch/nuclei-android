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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import nuclei3.logs.Log;
import nuclei3.logs.Logs;

/**
 * For GCM Backed Job Scheduling.  Will receive scheduled Job and execute.
 */
public class TaskGcmService extends GcmTaskService {

    private static final Log LOG = Logs.newLog(TaskGcmService.class);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            return super.onStartCommand(intent, flags, startId);
        } catch (Exception err) {
            LOG.e("Error with task service", err);
            return START_NOT_STICKY;
        }
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        try {
            Bundle bundle = taskParams.getExtras();
            String taskName = bundle.getString(TaskScheduler.TASK_NAME);
            Task task = (Task) Class.forName(taskName).newInstance();
            ArrayMap<String, Object> map = new ArrayMap<>(bundle.size());
            for (String key : bundle.keySet()) {
                map.put(key, bundle.get(key));
            }
            task.deserialize(map);
            if (task.isRunning())
                return GcmNetworkManager.RESULT_RESCHEDULE;
            task.attach(null);
            task.run();
            task.deliverResult(this);
            return GcmNetworkManager.RESULT_SUCCESS;
        } catch (Exception err) {
            LOG.e("Error running task", err);
            return GcmNetworkManager.RESULT_FAILURE;
        }
    }

}
