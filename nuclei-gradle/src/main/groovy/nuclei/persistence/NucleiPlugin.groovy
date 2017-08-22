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
package nuclei3.persistence

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class NucleiPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('nuclei', NucleiPluginExtension)

        project.android.sourceSets.all { sourceSet ->
            sourceSet.extensions.create('nuclei', NucleiPluginExtension)
        }

        project.afterEvaluate {
            def variants

            if (project.android.hasProperty('applicationVariants')) {
                variants = project.android.applicationVariants
            } else if (project.android.hasProperty('libraryVariants')) {
                variants = project.android.libraryVariants
            } else {
                throw new IllegalStateException('Android project must have applicationVariants or libraryVariants!')
            }

            Map<File, String> authorities = new HashMap<>();
            List<File> files = new ArrayList<>();
            for (Map.Entry<String, String> model : project.nuclei.nucleiModels.entrySet()) {
                File file = new File(project.projectDir.absolutePath + "/" + model.getKey());
                files.add(file);
                authorities.put(file, model.getValue())
            }

            variants.all { variant ->
                Task nucleiTask = project.task("nucleiFor${variant.name.capitalize()}", type: NucleiTask) {
                    modelFiles = files
                    modelAuthorities = authorities
                    outputDir = project.file("$project.buildDir/generated/nuclei/models")
                    defaultPackage = project.nuclei.defaultPackage
                }
                variant.registerJavaGeneratingTask(nucleiTask, nucleiTask.outputDir)

                if (project.nuclei.apt) {
                    def javaCompile = variant.javaCompile

                    if (javaCompile != null) {
                        def outputDir = project.file("$project.buildDir/generated/nuclei/apt")

                        variant.addJavaSourceFoldersToModel(outputDir);

                        def defaultPackage = project.nuclei.defaultPackage;
                        if (defaultPackage == null || defaultPackage.length() == 0)
                            defaultPackage = "nuclei.persistence.apt";

                        javaCompile.options.compilerArgs += [
                                '-processorpath', (project.rootProject.buildscript.configurations.classpath + javaCompile.classpath).asPath,
                                '-s', outputDir,
                                '-AdefaultPackage=' + defaultPackage
                        ]

                        if (project.nuclei.ui)
                            javaCompile.options.compilerArgs += [ '-Anuclei.ui=true' ]

                        if (project.nuclei.modelTypes.size() > 0) {
                            StringBuilder types = new StringBuilder();
                            for (String type : project.nuclei.modelTypes) {
                                if (types.length() > 0)
                                    types.append(",");
                                types.append(type);
                            }
                            javaCompile.options.compilerArgs += [ '-AmodelTypes=' + types ]
                        }

                        javaCompile.doFirst {
                            outputDir.mkdirs()
                        }
                    }
                }
            }
        }
    }

}
