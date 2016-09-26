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

public interface TaskInterceptor {

    /**
     * An opportunity to evaluate a Task before running.
     *
     * Return null to ignore Task
     *
     * @param task The Task to evaluate, must be of the same type as the original Task (or the exact same)
     * @return The Task that should be run
     */
    Task intercept(Task task);

}
