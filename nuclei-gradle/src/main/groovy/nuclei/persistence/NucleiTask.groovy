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
package nuclei.persistence

import nuclei.persistence.db.DbContext
import nuclei.persistence.model.Context
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class NucleiTask extends DefaultTask {

    @InputFiles
    List<File> modelFiles;

    @Input
    Map<File, String> modelAuthorities;

    @Input
    String defaultPackage;

    @OutputDirectory
    File outputDir

    @TaskAction
    def nuclei(IncrementalTaskInputs inputs) {
        if (!inputs.isIncremental() && outputDir.exists()) {
            outputDir.delete()
        }
        outputDir.mkdirs()

        for (File configFile : modelFiles) {
            logger.info("Nuclei Nuclei Config File: " + configFile)

            def input = new FileInputStream(configFile);
            byte[] buf = new byte[input.available()];
            input.read(buf);
            input.close();
            JSONObject model = new JSONObject(new String(buf, "UTF-8"));

            Object typeObj = model.get("type");
            String[] types;

            if (typeObj instanceof String)
                types = [typeObj.toString()];
            else {
                JSONArray array = (JSONArray) typeObj;
                types = new String[array.length()];
                for (int i = 0; i < array.length(); i++) {
                    types[i] = array.getString(i);
                }
            }

            if (defaultPackage != null && defaultPackage.length() == 0)
                defaultPackage = null;

            for (String type : types) {
                if ("db".equals(type)) {
                    String authority = modelAuthorities.get(configFile);
                    DbContext.newContext(model, outputDir, defaultPackage, authority).render();
                } else if ("json".equals(type) || "binary".equals(type)) {
                    Context.newContext(model, defaultPackage, outputDir).render(type);
                } else
                    throw new IllegalArgumentException("Invalid Type: " + model.getString("type"));
            }
        }
    }

}
