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

import android.content.Context;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

import nuclei.ui.Destroyable;

/**
 * A list adapter for putting items at certain offsets.  This adapter will wrap another adapter and manage
 * the placement of it's items within the items of the wrapped adapter.
 *
 *
 * @param <T>
 * @param <VH>
 */
public abstract class OffsetListAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> implements Destroyable {

    private RecyclerView.Adapter<VH> mOriginalAdapter;
    private SparseArrayCompat<T> mItems;
    private RangeOffset mOffset;
    private LayoutInflater mInflater;

    /**
     * If the wrapped adapter has stable IDs, the offset list adapter must have stable IDs too.
     *
     * @param context
     * @param adapter The adapter which we will be wrapping
     * @param items The items where the key is the position the value will be placed.
     */
    public OffsetListAdapter(Context context, RecyclerView.Adapter<VH> adapter, SparseArrayCompat<T> items) {
        mInflater = LayoutInflater.from(context);
        super.setHasStableIds(adapter.hasStableIds());
        mOriginalAdapter = adapter;
        mItems = items;
        buildOffsets();
    }

    /**
     * Rebuild the item offsets
     */
    private void buildOffsets() {
        mOffset = new RangeOffset();
        RangeOffset offset = mOffset;
        int[] positions = new int[mItems.size()];
        for (int i = 0, len = mItems.size(); i < len; i++) {
            positions[i] = mItems.keyAt(i);
        }
        Arrays.sort(positions); // positions must be sorted
        for (int i = 0, len = positions.length; i < len; i++) {
            offset = offset.expand(positions[i]);
        }
        offset.end = Integer.MAX_VALUE;
    }

    public RecyclerView.Adapter<VH> getOriginalAdapter() {
        return mOriginalAdapter;
    }

    public SparseArrayCompat<T> getItems() {
        return mItems;
    }

    /**
     * Update the current items and offset positions.  This will rebuild the offsets (so, call this sparingly).
     * @param items
     */
    public void setItems(SparseArrayCompat<T> items) {
        mItems = items;
        buildOffsets();
        notifyDataSetChanged();
    }

    /**
     * Get the original position that can be passed to the wrapped adapter.  Or -1 if the position is an offset position.
     *
     * @param position
     * @return
     */
    protected int getOriginalPosition(int position) {
        if (!mOffset.contains(position)) {
            mOffset = mOffset.find(position);
        }
        return mOffset.actual(position);
    }

    @Override
    public final int getItemViewType(int position) {
        int originalPos = getOriginalPosition(position);
        if (originalPos != -1 && mOriginalAdapter.getItemCount() > originalPos)
            return mOriginalAdapter.getItemViewType(originalPos);
        return getOffsetItemViewType(position);
    }

    /**
     * Return the view type.  This type must be unique from the wrapped adapter types.
     */
    protected abstract int getOffsetItemViewType(int position);

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
        mOriginalAdapter.setHasStableIds(hasStableIds);
    }

    @Override
    public long getItemId(int position) {
        int originalPos = getOriginalPosition(position);
        if (originalPos != -1 && mOriginalAdapter.getItemCount() > originalPos)
            return mOriginalAdapter.getItemId(originalPos);
        return super.getItemId(position);
    }

    @Override
    public void onViewRecycled(VH holder) {
        mOriginalAdapter.onViewRecycled(holder);
    }

    @Override
    public boolean onFailedToRecycleView(VH holder) {
        return mOriginalAdapter.onFailedToRecycleView(holder);
    }

    @Override
    public void onViewAttachedToWindow(VH holder) {
        mOriginalAdapter.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(VH holder) {
        mOriginalAdapter.onViewDetachedFromWindow(holder);
    }

    @Override
    public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.registerAdapterDataObserver(observer);
        mOriginalAdapter.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.unregisterAdapterDataObserver(observer);
        mOriginalAdapter.unregisterAdapterDataObserver(observer);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mOriginalAdapter.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mOriginalAdapter.onDetachedFromRecyclerView(recyclerView);
    }

    /**
     * Return true if the view type is to be managed by the offset list adapter and NOT the wrapped adapter
     */
    protected abstract boolean isOffsetViewType(int viewType);

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (!isOffsetViewType(viewType))
            return mOriginalAdapter.onCreateViewHolder(parent, viewType);
        return onOffsetCreateViewHolder(mInflater, parent, viewType);
    }

    protected abstract VH onOffsetCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(VH holder, int position) {
        int originalPos = getOriginalPosition(position);
        if (originalPos != -1 && mOriginalAdapter.getItemCount() > originalPos)
            mOriginalAdapter.onBindViewHolder(holder, originalPos);
        else
            onOffsetBindViewHolder(holder, position);
    }

    protected abstract void onOffsetBindViewHolder(VH holder, int position);

    @Override
    public void onBindViewHolder(VH holder, int position, List<Object> payloads) {
        int originalPos = getOriginalPosition(position);
        if (originalPos != -1 && mOriginalAdapter.getItemCount() > originalPos)
            mOriginalAdapter.onBindViewHolder(holder, originalPos, payloads);
        else
            onOffsetBindViewHolder(holder, position, payloads);
    }

    protected void onOffsetBindViewHolder(VH holder, int position, List<Object> payloads) {
        onOffsetBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return mOriginalAdapter.getItemCount() + mItems.size();
    }

    @Override
    public void onDestroy() {
        mInflater = null;
        mOriginalAdapter = null;
    }

    /**
     * Keep track of the range offsets.
     */
    private static class RangeOffset {

        public int start = 0; // the start of the range
        public int end = 0; // the end of the range
        private int offset = 0; // the offset to apply to the position that will be passed to the wrapped adapter

        private RangeOffset lastOffset;
        private RangeOffset nextOffset;

        /**
         * Find a range offset for a position.  This isn't great for random access.  But, my
         * current thought process is that the list will be traversed upwards or downards (or left/right, etc ... NOT randomly).
         * Which should yield decent performance considering I'm not anticipating there being more than a few
         * offset items.  Which means this data structure shouldn't have that many nodes and finding the correct node should be trivial AND
         * most of the time we should be within the correct range... meaning we'll mostly just return this immediately.  And, in the few
         * situations where we actually have to find the correct one, it'll likely be directly to left or right of the current node.
         * These are assumptions ... eventually I'll either prove myself correct or incorrect.  Arguments/suggestions are welcome.
         *
         * @param position The OffsetListAdapter position
         * @return The range offset for the position
         */
        public RangeOffset find(int position) {
            if (contains(position))
                return this;
            if (position < start) {
                RangeOffset o = lastOffset;
                while (o != null) {
                    if (o.contains(position))
                        return o;
                    o = o.lastOffset;
                }
            } else if (position > end) {
                RangeOffset o = nextOffset;
                while (o != null) {
                    if (o.contains(position))
                        return o;
                    o = o.nextOffset;
                }
            }
            throw new IllegalArgumentException("Invalid Position");
        }

        /**
         * Expand the range or return a new range if necessary.  This should be called in a linear
         * progression of the offset positions.
         *
         * @param position The next position.
         * @return The next range offset (or the currently expanded one)
         */
        public RangeOffset expand(int position) {
            if (position == 0 && start == 0) {
                return nextOffset(position);
            } else if (position == end) {
                end++;
                return this;
            } else if (position > end) {
                end = position;
                return nextOffset(position);
            }
            return this;
        }

        private RangeOffset nextOffset(int position) {
            RangeOffset o = new RangeOffset();
            o.start = position + 1;
            o.end = o.start;
            o.offset = offset + 1;
            o.lastOffset = this;
            nextOffset = o;
            return o;
        }

        /**
         * Find the actual position after having it adjusted for the current range offset.
         *
         * @param position
         * @return -1 if the position is one of the offsets or the wrapped adapters actual position relative to all other offsets
         */
        public int actual(int position) {
            if (start <= position && position < end)
                return position - offset;
            return -1;
        }

        /**
         * Check to see if the position is contained within this range
         *
         * @param position
         * @return true if the position can be calculated by this range offset
         */
        public boolean contains(int position) {
            return start <= position && position <= end;
        }

        @Override
        public String toString() {
            return "s" + start + "e" + end + "o" + offset;
        }

    }

}
