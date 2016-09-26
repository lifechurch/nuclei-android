package nuclei.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.ListPopupWindow;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import io.nuclei.R;

public class PopupMenu extends ListPopupWindow {

    CharSequence mHeaderText;
    int mPopupMaxWidth;
    FrameLayout mMeasureParent;
    ListAdapter mAdapter;
    boolean mContentMeasured;
    boolean mAdapterSet;

    public PopupMenu(Context context, View anchor, CharSequence headerText) {
        super(context);
        setAnchorView(anchor);
        mHeaderText = headerText;

        final Resources res = context.getResources();
        mPopupMaxWidth = Math.max(res.getDisplayMetrics().widthPixels / 2,
                res.getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_config_prefDialogWidth));
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public void show() {
        if (!mContentMeasured) {
            mContentMeasured = true;
            measureContent();
        }

        super.show();

        if (!mAdapterSet) {
            mAdapterSet = true;
            if (getListView().getHeaderViewsCount() == 0) {
                View view = LayoutInflater.from(getAnchorView().getContext())
                        .inflate(R.layout.cyto_view_dropdown_header, null);
                TextView v = (TextView) view.findViewById(android.R.id.text1);
                v.setText(mHeaderText);
                getListView().addHeaderView(view);
            }
            super.setAdapter(mAdapter);
        }
    }

    private void measureContent() {
        // Menus don't tend to be long, so this is more sane than it looks.
        int maxWidth = 0;
        int height = 0;
        View itemView = null;
        int itemType = 0;

        final ListAdapter adapter = mAdapter;
        final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (mMeasureParent == null) {
                mMeasureParent = new FrameLayout(getAnchorView().getContext());
            }

            itemView = adapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();
            final int itemHeight = itemView.getMeasuredHeight();
            if (itemWidth >= mPopupMaxWidth) {
                maxWidth = mPopupMaxWidth;
            } else if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
            if (itemHeight > height)
                height = itemHeight;
        }

        setContentWidth(maxWidth);
        if (height > 0)
            setHeight(height * (adapter.getCount() + 1));
    }

}