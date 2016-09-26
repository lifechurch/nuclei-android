package nuclei.ui.view;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public abstract class FragmentButtonAdapter extends ButtonAdapter implements ButtonBarView.OnItemSelectedListener {

    private FragmentManager mFragmentManager;
    private int mContainerViewId;

    public FragmentButtonAdapter(FragmentManager fragmentManager, int containerViewId) {
        mFragmentManager = fragmentManager;
        mContainerViewId = containerViewId;
    }

    public abstract Fragment getFragment(int position);

    @Override
    public final void onSelected(int position) {
        mFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(mContainerViewId, getFragment(position))
                .commitNow();
    }

}
