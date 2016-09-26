package nuclei.task;

import android.content.Context;

public class RunnableTask<T> extends Task<T> {

    private TaskRunnable<T> mRunnable;

    public RunnableTask(TaskRunnable<T> runnable) {
        mRunnable = runnable;
    }

    @Override
    public String getId() {
        return "runnable-" + System.currentTimeMillis();
    }

    @Override
    public void run(Context context) {
        T result = mRunnable.run(context);
        onComplete(result);
    }

}
