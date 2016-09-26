package nuclei.intent;

import android.content.Context;
import android.content.Intent;

public interface IntentFilter {

    boolean filter(Context context, Intent intent);

}
