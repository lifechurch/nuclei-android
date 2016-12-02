package nuclei.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.nuclei.test.R;
import nuclei.persistence.adapter.ListAdapter;
import nuclei.persistence.adapter.OffsetListAdapter;
import nuclei.persistence.adapter.PagedList;
import nuclei.persistence.adapter.PagedListAdapter;
import nuclei.task.Result;
import nuclei.task.TaskRunnable;
import nuclei.task.Tasks;

public class OffsetTestActivity2 extends NucleiActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.offset_activity_test);

        ItemsAdapter adapter = new ItemsAdapter(this);
        adapter.setList(new ItemPagedList(this));
        manage(adapter);

        SparseArrayCompat<Integer> offsetItems = new SparseArrayCompat<>();
        offsetItems.put(0, Integer.MAX_VALUE - 1);
        offsetItems.put(1, Integer.MAX_VALUE - 2);
        offsetItems.put(2, Integer.MAX_VALUE - 3);

        OffsetTestAdapter offsetAdapter = new OffsetTestAdapter(this, adapter, offsetItems);
        manage(offsetAdapter);

        RecyclerView items = (RecyclerView) findViewById(R.id.items);
        items.setLayoutManager(new LinearLayoutManager(this));
        items.setAdapter(offsetAdapter);
    }

    private static class OffsetViewHolder extends ListAdapter.ViewHolder<Integer> {
        TextView textView;

        public OffsetViewHolder(View view) {
            super(view);
            if (view instanceof TextView)
                textView = (TextView) view;
        }

        @Override
        public void onBind() {
            if (textView != null && item != null)
                textView.setText(item.toString());
        }
    }

    private static class OffsetTestAdapter extends OffsetListAdapter<Integer, OffsetViewHolder> {

        public OffsetTestAdapter(Context context, RecyclerView.Adapter<OffsetViewHolder> adapter, SparseArrayCompat<Integer> items) {
            super(context, adapter, items);
        }

        @Override
        protected int getOffsetItemViewType(int position) {
            return 3;
        }

        @Override
        protected boolean isOffsetViewType(int viewType) {
            return viewType == 3;
        }

        @Override
        protected OffsetViewHolder onOffsetCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
            return new OffsetViewHolder(new TextView(parent.getContext()));
        }

    }

    private static class ItemsAdapter extends PagedListAdapter<Integer, OffsetViewHolder> {

        public ItemsAdapter(Context context) {
            super(context);
        }

        @Override
        protected OffsetViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
            return new OffsetViewHolder(new TextView(parent.getContext()));
        }

        @Override
        protected OffsetViewHolder onCreateMoreViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
            return new OffsetViewHolder(new ProgressBar(parent.getContext()));
        }

    }

    private static class ItemPagedList extends PagedList<Integer> {

        public ItemPagedList(Context context) {
            super(context, 32, 10);
        }

        @Override
        protected void onLoadPage(Context context, final int pageIndex) {
            Tasks.execute(new TaskRunnable<List<Integer>>() {
                @Override
                public List<Integer> run(Context context) {
                    return null;
                }
            }).addCallback(new Result.CallbackAdapter<List<Integer>>() {
                @Override
                public void onResult(List<Integer> result) {
                    List<Integer> data = new ArrayList<Integer>();
                    int start = pageIndex * 10;
                    for (int i = start; i < start + 10; i++) {
                        data.add(i);
                    }
                    onPageLoaded(pageIndex, data, data.size() + size(), pageIndex < 10);
                }
            });
        }

    }

}
