package nuclei3.task;

import android.content.Context;

public class RunnableTask<T> extends Task<T> {

    private final String mId;
    private final TaskRunnable<T> mRunnable;

    public RunnableTask(String id, TaskRunnable<T> runnable) {
        mId = id;
        mRunnable = runnable;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public void run(Context context) {
        T result = mRunnable.run(context);
        onComplete(result);
    }

}
