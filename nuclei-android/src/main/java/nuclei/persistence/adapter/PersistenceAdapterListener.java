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
package nuclei.persistence.adapter;

import java.lang.ref.WeakReference;

import nuclei.persistence.PersistenceList;

/**
 * A listener to bind PersistenceLists to adapters after they are ready.
 *
 * @param <T>
 */
public class PersistenceAdapterListener<T> implements PersistenceList.Listener<T> {

    final WeakReference<PersistenceListAdapter<T>> mAdapter;

    public PersistenceAdapterListener(PersistenceListAdapter<T> adapter) {
        mAdapter = new WeakReference<>(adapter);
    }

    @Override
    public void onAvailable(PersistenceList<T> list, boolean sizeChanged) {
        PersistenceListAdapter<T> adapter = mAdapter.get();
        if (adapter != null) {
            if (sizeChanged)
                adapter.notifyListSizeChanged();
            adapter.setList(list.isClosed() ? null : list);
        }
    }

}
