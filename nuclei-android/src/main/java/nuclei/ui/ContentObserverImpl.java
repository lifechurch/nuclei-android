package nuclei.ui;

import android.app.Activity;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import nuclei.persistence.PersistenceObserver;

public class ContentObserverImpl extends ContentObserver implements Destroyable {

    private Activity mActivity;
    private PersistenceObserver mObserver;

    public ContentObserverImpl(Handler handler, Activity activity, PersistenceObserver observer) {
        super(handler);
        mActivity = activity;
        mObserver = observer;
    }

    @Override
    public void onChange(boolean selfChange) {
        mObserver.onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        mObserver.onChange(selfChange, uri);
    }

    @Override
    public void onDestroy() {
        mActivity.getContentResolver().unregisterContentObserver(this);
        mActivity = null;
        mObserver = null;
    }

}
