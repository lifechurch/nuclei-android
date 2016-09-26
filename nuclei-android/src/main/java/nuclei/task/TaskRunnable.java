package nuclei.task;

import android.content.Context;

public interface TaskRunnable<T> {

    T run(Context context);

}
