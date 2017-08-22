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
package nuclei3.logs;

public class Trace {

    private static final Log LOG = Logs.newLog(Trace.class);

    public void trace(Class clazz, String message) {
        LOG.i(clazz.getName() + " :: " + message);
    }

    public void onCreate(Class clazz) {
        LOG.i(clazz.getName() + ".onCreate()");
    }

    public void onPause(Class clazz) {
        LOG.i(clazz.getName() + ".onPause()");
    }

    public void onStop(Class clazz) {
        LOG.i(clazz.getName() + ".onStop()");
    }

    public void onResume(Class clazz) {
        LOG.i(clazz.getName() + ".onResume()");
    }

    public void onDestroy(Class clazz) {
        LOG.i(clazz.getName() + ".onDestroy()");
    }

}
