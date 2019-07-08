/**
 * Copyright 2016 YouVersion
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei3.ui.share;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import android.webkit.MimeTypeMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base FileProvider that helps with sharing content with several applications that don't
 * behave well with the default FileProvider
 */
public class NucleiFileProvider extends FileProvider {

    // a set of column names that other apps expect to exist and that may not
    final static String[] COLUMN_NAMES = new String[]{
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE
    };

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor source = super.query(uri, projection, selection, selectionArgs, sortOrder);
        if (source == null)
            return null;
        try {
            final String[] columnNames = source.getColumnNames();

            String[] names = columnNames;
            List<String> missingNames = getMissingColumns(columnNames);
            int mimeTypeIndex = -1;

            if (missingNames.size() > 0) {
                String[] newColumnNames = Arrays.copyOf(columnNames, columnNames.length + missingNames.size());
                int ix = columnNames.length;
                for (String missingName : missingNames) {
                    newColumnNames[ix] = missingName;
                    if (MediaStore.MediaColumns.MIME_TYPE.equals(missingName))
                        mimeTypeIndex = ix;
                    ix++;
                }
                names = newColumnNames;
            }

            MatrixCursor cursor = new MatrixCursor(names, source.getCount());
            source.moveToPosition(-1);
            int columnLength = columnNames.length;
            while (source.moveToNext()) {
                MatrixCursor.RowBuilder row = cursor.newRow();
                for (int i = 0; i < names.length; i++) {
                    if (i < columnLength)
                        row.add(source.getString(i));
                    else if (i == mimeTypeIndex) {
                        // populate the MIME_TYPE column with a guess as to the mime type of the file
                        final int lastDot = uri.getPath().lastIndexOf('.');
                        if (lastDot >= 0) {
                            String extension = uri.getPath().substring(lastDot + 1);
                            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                            if (mimeType != null)
                                row.add(mimeType);
                            else
                                row.add("application/octet");
                        } else
                            row.add("application/octet");
                    } else
                        row.add(null);
                }
            }

            return cursor;
        } finally {
            source.close();
        }
    }

    /**
     * Build a list of columns that many apps expect to exist that aren't currently defined
     *
     * @param columnNames
     * @return
     */
    private List<String> getMissingColumns(String[] columnNames) {
        Set<String> names = new HashSet<>();
        for (String name : columnNames)
            names.add(name);

        List<String> missing = new ArrayList<>();

        for (String name : COLUMN_NAMES) {
            if (!names.contains(name))
                missing.add(name);
        }

        return missing;
    }


}
