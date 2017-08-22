package nuclei3.ui;

import android.test.ActivityInstrumentationTestCase2;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class ShareActivityTest extends ActivityInstrumentationTestCase2<TestShareActivity> {

    public ShareActivityTest() {
        super(TestShareActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testShareManager() throws Exception {
        onView(withId(io.nuclei.test.R.id.click_me))
                .perform(click());

        assertNotNull(getActivity().packageName);
    }

}
