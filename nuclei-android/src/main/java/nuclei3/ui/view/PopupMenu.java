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
package nuclei3.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.ListPopupWindow;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import io.nuclei3.R;

public class PopupMenu extends ListPopupWindow {

    private final CharSequence mHeaderText;
    private final int mPopupMaxWidth;
    private FrameLayout mMeasureParent;
    private ListAdapter mAdapter;
    private boolean mContentMeasured;
    private boolean mAdapterSet;

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
                        .inflate(R.layout.nuclei3_view_dropdown_header, null);
                TextView v = (TextView) view.findViewById(android.R.id.text1);
                v.setText(mHeaderText);
                getListView().addHeaderView(view);
            }
            super.setAdapter(mAdapter);
        }
    }

    private void measureContent() {
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