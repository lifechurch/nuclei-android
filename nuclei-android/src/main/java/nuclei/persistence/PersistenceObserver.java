package nuclei.persistence;

import android.net.Uri;
import androidx.annotation.Nullable;

public interface PersistenceObserver {

    void onChange(boolean selfChange, @Nullable Uri uri);

}
