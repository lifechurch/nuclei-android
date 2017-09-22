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
package nuclei.persistence.model;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

public class CustomSerializer<T> {

    public T deserialize(Context context, JsonReader reader) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void serialize(Context context, JsonWriter writer, T object) throws IOException {
        throw new UnsupportedOperationException();
    }

}
