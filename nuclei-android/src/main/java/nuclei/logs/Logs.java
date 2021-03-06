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
package nuclei.logs;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import java.io.File;

import nuclei.task.ContextHandle;

public final class Logs {

    public static boolean TRACE = false;
    public static boolean EXTRA = false;

    private Logs() {
    }

    public static Log newLog(Class<?> container) {
        String name = container.getSimpleName();
        if (name.length() > 23)
            name = name.substring(0, 23);
        return new Log(name);
    }

    protected static File newLogFile() {
        Context context = ContextHandle.getApplicationHandle().get();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
            return new File(context.getExternalFilesDir(null), "info.log");
        }
        return new File(context.getFilesDir(), "info.log");
    }

}
