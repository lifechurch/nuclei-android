package nuclei.persistence;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import nuclei.persistence.adapter.PagedList;

public class PagedListTests extends ApplicationTestCase<Application> {
    public PagedListTests() {
        super(Application.class);
    }

    public void testPagedList() {
        final AtomicInteger index = new AtomicInteger(-1);
        PagedList<Integer> numbers = new PagedList<Integer>(getContext(), 3, 25) {
            @Override
            protected void onLoadPage(Context context, final int pageIndex) {
                List<Integer> numbers = new ArrayList<>();
                for (int i = pageIndex * 25; i < pageIndex * 25 + 25; i++) {
                    numbers.add(i);
                }
                index.set(pageIndex);
                onPageLoaded(pageIndex, numbers, 100, pageIndex < 5);
            }
        };

        assertNull(numbers.get(5)); // warm the list
        assertEquals(5, numbers.get(5).intValue());
        assertEquals(index.get(), 0);

        for (int i = 0; i <= 200; i += 25) {
            numbers.get(i); // warm list
            assertEquals(i, numbers.get(i).intValue());
        }

        assertEquals(7, numbers.getMinPageIndex());
        assertEquals(9, numbers.getMaxPageIndex());

        for (int i = 0; i < numbers.size() / 25; i++) {
            if (i >= 6 && i <= 8) {
                assertTrue(numbers.isPageLoaded(i));
            } else {
                assertFalse(numbers.isPageLoaded(i));
            }
        }

        assertFalse(numbers.isPaging());
    }

}