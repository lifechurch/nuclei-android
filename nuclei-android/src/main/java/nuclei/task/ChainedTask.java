package nuclei.task;

import android.content.Context;

public abstract class ChainedTask<T, C> extends Task<T> {

    Result<C> chainedResult;

    public boolean isRoot() {
        return chainedResult == null;
    }

    @Override
    public void run(Context context) throws Exception {
        run(context, chainedResult);
    }

    protected abstract void run(Context context, Result<C> previousResult) throws Exception;

}
