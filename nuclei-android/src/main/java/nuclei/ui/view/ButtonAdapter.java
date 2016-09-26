package nuclei.ui.view;

import android.database.DataSetObserver;

public abstract class ButtonAdapter {

    private DataSetObserver mViewObserver;

    public abstract int getTitle(int position);
    public abstract int getDrawable(int position);
    public abstract int getCount();

    public void notifyDataSetChanged() {
        synchronized (this) {
            if (mViewObserver != null)
                mViewObserver.onChanged();
        }
    }

    public void setViewObserver(DataSetObserver viewObserver) {
        synchronized (this) {
            mViewObserver = viewObserver;
        }
    }

}
