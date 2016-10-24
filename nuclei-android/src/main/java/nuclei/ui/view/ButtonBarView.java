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
package nuclei.ui.view;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.nuclei.R;

public class ButtonBarView extends FrameLayout {

    private ButtonAdapter mAdapter;
    private Item[] mItems;
    private List<OnItemSelectedListener> mListeners = new ArrayList<>();
    private int mSelectedTint;
    private int mUnselectedTint;
    private int mOrientation;
    private int mSelectedItem = -1;
    private LinearLayout mButtons;
    private AdapterObserver mObserver;
    private ArrayMap<Item, ValueAnimator> mLabelAnimators = new ArrayMap<>();
    private boolean mStateRestored;

    public ButtonBarView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ButtonBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ButtonBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public ButtonBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ButtonBarView, defStyleAttr, defStyleRes);

        final boolean bottom = a.getBoolean(R.styleable.ButtonBarView_bottom, false);

        final int layout = a.getResourceId(R.styleable.ButtonBarView_control_layout,
                bottom
                ? R.layout.cyto_view_button_bar_bottom
                : R.layout.cyto_view_button_bar);

        final View view = LayoutInflater.from(context).inflate(layout, this, false);
        addView(view);

        mButtons = (LinearLayout) view.findViewById(R.id.buttons);

        mOrientation = a.getInt(R.styleable.ButtonBarView_control_orientation, 1);
        switch (mOrientation) {
            case 1:
                mButtons.setOrientation(LinearLayout.HORIZONTAL);
                break;
            case 2:
                mButtons.setOrientation(LinearLayout.VERTICAL);
                break;
        }

        mSelectedTint = a.getColor(R.styleable.ButtonBarView_selected_color, ResourcesCompat.getColor(getResources(), R.color.black, context.getTheme()));
        mUnselectedTint = a.getColor(R.styleable.ButtonBarView_unselected_color, ResourcesCompat.getColor(getResources(), R.color.grey, context.getTheme()));

        if (a.hasValue(R.styleable.ButtonBarView_buttons_background)) {
            final int color = a.getColor(R.styleable.ButtonBarView_buttons_background, Color.WHITE);
            View v = view.findViewById(R.id.buttons_container);
            if (v != null)
                v.setBackgroundColor(color);
            else
                mButtons.setBackgroundColor(color);
        }

        a.recycle();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.selected = mSelectedItem;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        mStateRestored = true;
        final SavedState savedState = (SavedState) state;
        setSelectedItemState(savedState.selected);
        onEnsurePosition();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mStateRestored) {
            mStateRestored = true;
            onEnsurePosition();
        }
    }

    public void setAdapter(ButtonAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.setViewObserver(null);

            if (mAdapter instanceof OnItemSelectedListener)
                removeOnItemSelectedListener((OnItemSelectedListener) mAdapter);
        }

        mAdapter = adapter;

        if (mAdapter != null) {
            if (mObserver == null) {
                mObserver = new AdapterObserver();
            }
            mAdapter.setViewObserver(mObserver);
            if (mAdapter instanceof OnItemSelectedListener)
                addOnItemSelectedListener((OnItemSelectedListener) mAdapter);

            dataSetChanged();

            if (mStateRestored) {
                onEnsurePosition();
            }
        }
    }

    public int getSelectedItem() {
        return mSelectedItem;
    }

    private void onEnsurePosition() {
        if (mAdapter != null) {
            if (mSelectedItem == -1)
                setSelectedItem(0);
            else if (mSelectedItem > mAdapter.getCount())
                setSelectedItem(mAdapter.getCount() - 1);
        }
    }

    public void addOnItemSelectedListener(OnItemSelectedListener listener) {
        mListeners.add(listener);
    }

    public void removeOnItemSelectedListener(OnItemSelectedListener listener) {
        mListeners.remove(listener);
    }

    private void setSelectedItemState(int position) {
        int pos = 0;
        for (Item item : mItems) {
            final boolean selected = pos == position;
            item.imageView.setSelected(selected);
            if (selected) {
                mSelectedItem = pos;
                item.imageView.setColorFilter(mSelectedTint, PorterDuff.Mode.SRC_ATOP);
                if (item.textView != null)
                    item.textView.setTextColor(mSelectedTint);
                setSelected(item, true);
            } else {
                item.imageView.setColorFilter(mUnselectedTint, PorterDuff.Mode.SRC_ATOP);
                if (item.textView != null)
                    item.textView.setTextColor(mUnselectedTint);
                setSelected(item, false);
            }
            pos++;
        }
    }

    public void setSelectedItem(int position) {
        if (position > -1 && position != mSelectedItem && mItems != null) {
            setSelectedItemState(position);
            final int size = mListeners.size();
            for (int i = 0; i < size; i++)
                mListeners.get(i).onSelected(mSelectedItem);
        }
    }

    private void setSelected(final Item item, boolean selected) {
        if (mItems.length < 5)
            return;
        if (selected) {
            item.imageView.setColorFilter(mSelectedTint, PorterDuff.Mode.SRC_ATOP);
            if (item.textView != null) {
                item.textView.setTextColor(mSelectedTint);
                item.textView.setVisibility(View.VISIBLE);
                item.textView.setTextSize(0);
                ValueAnimator animator = mLabelAnimators.get(item);
                if (animator != null)
                    animator.cancel();
                animator = new ValueAnimator();
                mLabelAnimators.put(item, animator);
                animator.setDuration(200);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        int animatedValue = (Integer) valueAnimator.getAnimatedValue();
                        item.textView.setTextSize(animatedValue);
                    }
                });
                animator.setIntValues(0, 14);
                animator.start();
            }
        } else {
            item.imageView.setColorFilter(mUnselectedTint, PorterDuff.Mode.SRC_ATOP);
            if (item.textView != null) {
                item.textView.setTextColor(mUnselectedTint);
                ValueAnimator animator = mLabelAnimators.get(item);
                if (animator != null)
                    animator.cancel();
                animator = new ValueAnimator();
                mLabelAnimators.put(item, animator);
                animator.setDuration(200);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        int animatedValue = (Integer) valueAnimator.getAnimatedValue();
                        item.textView.setTextSize(animatedValue);
                        if (animatedValue == 0)
                            item.textView.setVisibility(View.GONE);
                    }
                });
                animator.setIntValues(14, 0);
                animator.start();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLabelAnimators.clear();
    }

    void dataSetChanged() {
        mLabelAnimators.clear();
        mButtons.removeAllViews();

        if (mAdapter == null) {
            mItems = null;
            return;
        }

        final int count = mAdapter.getCount();
        final View.OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = 0;
                for (Item item : mItems) {
                    if (item.view == v) {
                        setSelectedItem(position);
                        break;
                    }
                    position++;
                }
            }
        };

        final LayoutInflater inflater = LayoutInflater.from(getContext());

        if (mItems == null || mItems.length != count)
            mItems = new Item[count];

        for (int i = 0; i < count; i++) {
            final int textId = mAdapter.getTitle(i);
            final int imageId = mAdapter.getDrawable(i);
            Item item = new Item(textId, imageId);

            item.view = (ViewGroup) inflater.inflate(
                    mOrientation == 1
                    ? R.layout.cyto_view_button_horizontal_bar_item
                    : R.layout.cyto_view_button_vertical_bar_item, this, false);
            item.view.setOnClickListener(listener);

            item.imageView = (ImageView) item.view.findViewById(R.id.image);
            item.imageView.setImageResource(item.imageId);
            item.imageView.setColorFilter(mUnselectedTint, PorterDuff.Mode.SRC_ATOP);

            item.textView = (TextView) item.view.findViewById(R.id.text);
            if (item.textView != null) {
                item.textView.setText(item.textId);

                if (count > 4)
                    item.textView.setVisibility(GONE);
            }

            mItems[i] = item;

            LinearLayout.LayoutParams params = mOrientation == 1
                                               ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1)
                                               : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = mOrientation == 1 ? Gravity.CENTER_VERTICAL : Gravity.CENTER_HORIZONTAL;
            mButtons.addView(item.view, params);
        }
    }

    private class AdapterObserver extends DataSetObserver {
        AdapterObserver() {
        }

        @Override
        public void onChanged() {
            dataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataSetChanged();
        }
    }

    private static class Item {

        final int textId;
        final int imageId;
        ViewGroup view;
        TextView textView;
        ImageView imageView;

        public Item(int textId, int imageId) {
            this.textId = textId;
            this.imageId = imageId;
        }

    }

    public interface OnItemSelectedListener {

        void onSelected(int position);

    }

    public static class SavedState extends BaseSavedState {

        int selected;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(selected);
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<ButtonBarView.SavedState> CREATOR = new Parcelable.Creator<ButtonBarView.SavedState>() {
            public ButtonBarView.SavedState createFromParcel(Parcel in) {
                return new ButtonBarView.SavedState(in);
            }

            public ButtonBarView.SavedState[] newArray(int size) {
                return new ButtonBarView.SavedState[size];
            }
        };

        private SavedState(Parcel in) {
            super(in);
            selected = in.readInt();
        }

    }

}
