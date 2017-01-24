package nuclei.persistence;

import android.net.Uri;
import android.support.annotation.Nullable;

public interface PersistenceObserver {

    void onChange(boolean selfChange, @Nullable Uri uri);

}
