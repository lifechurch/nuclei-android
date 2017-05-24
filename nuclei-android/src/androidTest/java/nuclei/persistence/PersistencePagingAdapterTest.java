package nuclei.persistence;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.atomic.AtomicInteger;

import nuclei.adapter.ListAdapter;

public class PersistencePagingAdapterTest extends ApplicationTestCase<Application> {
    public PersistencePagingAdapterTest() {
        super(Application.class);
    }

    private PersistencePagingAdapter<Object, ListAdapter.ViewHolder<Object>> newAdapter(final AtomicInteger lastPageLoad) {
        PersistencePagingAdapter<Object, ListAdapter.ViewHolder<Object>> adapter = new PersistencePagingAdapter<Object, ListAdapter.ViewHolder<Object>>(getContext()) {
            @Override
            protected ViewHolder<Object> onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
                return null;
            }

            @Override
            protected ViewHolder<Object> onCreateMoreViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
                return null;
            }

            @Override
            public void reset() {
                super.reset();
                lastPageLoad.set(-1);
            }

            @Override
            protected void onLoadPage(int pageIndex, int pageSize) {
                assertTrue(lastPageLoad.get() + " >= " + pageIndex, lastPageLoad.get() < pageIndex);
                lastPageLoad.set(pageIndex);
            }
        };
        adapter.setPageSize(25);
        adapter.setList(new MultiPersistenceListImpl<Object>(new Query[0]) {
            @Override
            public Query<Object> getQuery() {
                return null;
            }
        });
        return adapter;
    }

    public void testPaging() {
        final AtomicInteger lastPageLoad = new AtomicInteger(-1);
        PersistencePagingAdapter<Object, ListAdapter.ViewHolder<Object>> adapter = newAdapter(lastPageLoad);

        adapter.onBindViewHolder(new ListAdapter.ViewHolder<Object>(new View(getContext())), 0);
        assertEquals(0, lastPageLoad.get());
        adapter.onBindViewHolder(new ListAdapter.ViewHolder<Object>(new View(getContext())), 5);
        assertEquals(0, lastPageLoad.get());

        adapter.onBindViewHolder(new ListAdapter.ViewHolder<Object>(new View(getContext())), 25);
        assertEquals(1, lastPageLoad.get());

        adapter.reset();
        adapter.onBindViewHolder(new ListAdapter.ViewHolder<Object>(new View(getContext())), 100);
        assertEquals(4, lastPageLoad.get());
        adapter.onBindViewHolder(new ListAdapter.ViewHolder<Object>(new View(getContext())), 0);
        assertEquals(4, lastPageLoad.get());

        adapter.reset();
        for (int i = 0; i <= 10 * 25; i += 25) {
            adapter.onBindViewHolder(new ListAdapter.ViewHolder<Object>(new View(getContext())), i);
            assertEquals(i / 25, lastPageLoad.get());
        }
    }

}
