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

class NucleiPluginExtension {

    Map<String, String> nucleiModels = new HashMap<>();
    List<String> modelTypes = new ArrayList<>();
    boolean apt = true;
    String defaultPackage = "";
    boolean ui = true;

    public List<String> getModelTypes() {
        return modelTypes;
    }

    public void setModelTypes(List<String> modelTypes) {
        this.modelTypes = modelTypes;
    }

    public void modelTypes(List<String> modelTypes) {
        this.modelTypes = modelTypes;
    }

    public void addModelType(String modelType) {
        modelTypes.add(modelType);
    }

    public void modelType(String modelType) {
        modelTypes.add(modelType);
    }

    public String getDefaultPackage() {
        return defaultPackage;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    public void defaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    public boolean isUi() {
        return ui;
    }

    public boolean getUi() {
        return ui;
    }

    public void setUi(boolean ui) {
        this.ui = ui;
    }

    public void ui(boolean ui) {
        this.ui = ui;
    }

    public boolean isApt() {
        return apt;
    }

    public boolean getApt() {
        return apt;
    }

    public void setApt(boolean enabled) {
        apt = enabled;
    }

    public void apt(boolean enabled) {
        apt = enabled;
    }

    public Map<String, String> getNucleiModels() {
        return nucleiModels;
    }

    public void setNucleiModels(Map<String, String> nucleiModels) {
        this.nucleiModels = nucleiModels;
    }

    public void addNucleiModel(String fileName, String authority) {
        nucleiModels.put(fileName, authority);
    }

    public void addNucleiModel(String fileName) {
        addNucleiModel(fileName, null);
    }

    public void nucleiModel(String fileName, String authority) {
        addNucleiModel(fileName, authority);
    }

    public void nucleiModel(String fileName) {
        addNucleiModel(fileName, null);
    }

}
