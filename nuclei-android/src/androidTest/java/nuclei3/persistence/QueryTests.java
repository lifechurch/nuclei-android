package nuclei3.persistence;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class QueryTests {

    @Test
    public void testArgs() {
        Date now = new Date(123);
        String[] args = Query.args()
                .arg(true)
                .arg(now)
                .arg(1)
                .arg(3.3F)
                .arg(3.3D)
                .arg(3L)
                .arg("a")
                .arg(Boolean.valueOf(true))
                .arg(Long.valueOf(1))
                .arg(Double.valueOf(2))
                .arg(Integer.valueOf(3))
                .arg(Float.valueOf(4))
                .args();

        assertEquals(12, args.length);
        assertEquals("true", args[0]);
        assertEquals("123", args[1]);
        assertEquals("1", args[2]);
        assertEquals("3.3", args[3]);
        assertEquals("3.3", args[4]);
        assertEquals("3", args[5]);
        assertEquals("a", args[6]);
        assertEquals("true", args[7]);
        assertEquals("1", args[8]);
        assertEquals("2.0", args[9]);
        assertEquals("3", args[10]);
        assertEquals("4.0", args[11]);
    }

}
