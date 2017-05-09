/**
 * Copyright 2016 YouVersion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.task;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * An asynchronous pending result
 *
 * @param <T>
 */
public class Result<T> {

    final List<SimpleCallback<T>> mCallbacks = new ArrayList<>(1);

    T mData;
    Exception mException;
    boolean mDataSet;
    boolean mFromCache;
    Object mObjectHandle;
    ContextHandle mContextHandle;
    Result<T> mForwardTo;

    public Result() {
    }

    public Result(T data) {
        mData = data;
        mDataSet = true;
    }

    public Result<T> forward(Result<T> forwardTo) {
        synchronized (this) {
            mForwardTo = forwardTo;
            if (mDataSet)
                mForwardTo.onResult(mData, mFromCache);
        }
        return this;
    }

    /**
     * Returns true if the result is from a cache
     *
     * @return
     */
    public boolean isFromCache() {
        return mFromCache;
    }

    /**
     * Get the result, but ignore errors
     *
     * @return The result
     */
    public T uncheckedGet() {
        return mData;
    }

    /**
     * Get the result
     *
     * @return The result
     */
    public T get() {
        synchronized (this) {
            if (!mDataSet)
                throw new IllegalStateException("Result Not Ready");
            if (mException != null)
                throw new RuntimeException(mException);
            return mData;
        }
    }

    /**
     * Sync wait and return the result
     *
     * @param millis The amount of time to wait before timing out
     * @return The result
     * @throws TimeoutException
     */
    public T get(long millis) throws TimeoutException {
        syncWait(millis);
        return get();
    }

    public Exception getException() {
        return mException;
    }

    /**
     * Return true if the result is complete
     *
     * @return True if this result has been delivered
     */
    public boolean isComplete() {
        return mDataSet;
    }

    public boolean isSuccessful() {
        return mDataSet && mException == null;
    }

    public <C> Result<C> continueWith(final ChainedTask<C, T> task) {
        return continueWith(task, task, Tasks.get());
    }

    public <C> Result<C> continueWith(final ChainedTask<C, T> task, final ChainedTask<C, T> errorTask) {
        return continueWith(task, errorTask, Tasks.get());
    }

    public <C> Result<C> continueWith(final ChainedTask<C, T> task, final TaskPool taskPool) {
        return continueWith(task, task, taskPool);
    }

    public <C> Result<C> continueWith(final ChainedTask<C, T> task, final ChainedTask<C, T> errorTask, final TaskPool taskPool) {
        final Result<C> nextResult = new Result<>();
        addCallback(new CallbackAdapter<T>() {
            @Override
            public void onResult(T result) {
                if (task != null) {
                    task.chainedResult = Result.this;
                    taskPool.execute(task).forward(nextResult);
                } else {
                    try {
                        nextResult.onResult((C) result);
                    } catch (ClassCastException err) {
                        nextResult.onException(err);
                    }
                }
            }

            @Override
            public void onException(Exception err) {
                if (errorTask != null) {
                    errorTask.chainedResult = Result.this;
                    taskPool.execute(errorTask).forward(nextResult);
                } else {
                    nextResult.onException(err);
                }
            }
        });
        return nextResult;
    }

    public <C> Result<C> continueWith(final ContextHandle handle, final ChainedTask<C, T> task) {
        return continueWith(handle, task, task, Tasks.get());
    }

    public <C> Result<C> continueWith(final ContextHandle handle, final ChainedTask<C, T> task, final ChainedTask<C, T> errorTask) {
        return continueWith(handle, task, errorTask, Tasks.get());
    }

    public <C> Result<C> continueWith(final ContextHandle handle, final ChainedTask<C, T> task, final TaskPool taskPool) {
        return continueWith(handle, task, task, taskPool);
    }

    public <C> Result<C> continueWith(final ContextHandle handle, final ChainedTask<C, T> task, final ChainedTask<C, T> errorTask, final TaskPool taskPool) {
        final Result<C> nextResult = new Result<>();
        addCallback(new CallbackAdapter<T>() {
            @Override
            public void onResult(T result) {
                if (task != null) {
                    task.chainedResult = Result.this;
                    taskPool.execute(handle, task).forward(nextResult);
                } else {
                    try {
                        nextResult.onResult((C) result);
                    } catch (ClassCastException err) {
                        nextResult.onException(err);
                    }
                }
            }

            @Override
            public void onException(Exception err) {
                if (errorTask != null) {
                    errorTask.chainedResult = Result.this;
                    taskPool.execute(handle, errorTask).forward(nextResult);
                } else {
                    nextResult.onException(err);
                }
            }
        });
        return nextResult;
    }

    /**
     * Add a callback to listen for the result
     *
     * @param callback The callback
     * @return The Result
     */
    public Result<T> addCallback(Callback<T> callback) {
        return addCallback(callback, null);
    }

    /**
     * Add a callback to listen for the result
     *
     * @param callback The callback
     * @param handle The handle to be delivered to the context
     * @return The Result
     */
    public Result<T> addCallback(Callback<T> callback, Object handle) {
        synchronized (this) {
            if (mDataSet) {
                callback.setResult(this);
                if (mException != null)
                    callback.onException(mException, handle);
                else
                    callback.onResult(mData, handle);
                notifyAll();
            } else {
                mObjectHandle = handle;
                mCallbacks.add(callback);
            }
        }
        return this;
    }

    /**
     * Deliver a result
     *
     * @param data The data result
     */
    public void onResult(T data) {
        onResult(data, false);
    }

    /**
     * Deliver a result
     *
     * @param data The data result
     * @param fromCache Whether the result is from a cache
     */
    public void onResult(T data, boolean fromCache) {
        synchronized (this) {
            if (mDataSet)
                throw new IllegalStateException("Data already set");
            mData = data;
            mDataSet = true;
            mFromCache = fromCache;
            boolean released = mContextHandle != null && mContextHandle.get() == null;
            for (SimpleCallback<T> cb : mCallbacks) {
                if (cb instanceof Callback) {
                    Callback<T> callback = (Callback<T>) cb;
                    callback.setResult(this);
                    if (released) {
                        callback.onContextReleased(data, mObjectHandle);
                    } else {
                        if (fromCache)
                            callback.onCacheResult(data, mObjectHandle);
                        else
                            callback.onResult(data, mObjectHandle);
                    }
                } else {
                    cb.onResult(data, null, mObjectHandle);
                }
            }
            mObjectHandle = null;
            mCallbacks.clear();
            notifyAll();
        }
        synchronized (this) {
            if (mForwardTo != null)
                mForwardTo.onResult(data, fromCache);
        }
    }

    /**
     * Deliver an exception as the result
     *
     * @param err
     */
    public void onException(Exception err) {
        onExceptionWithResult(err, null, false);
    }

    public void onExceptionWithResult(Exception err, T result, boolean fromCache) {
        synchronized (this) {
            if (mDataSet)
                throw new IllegalStateException("Data already set");
            mData = result;
            mFromCache = fromCache;
            mDataSet = true;
            mException = err;
            boolean released = mContextHandle != null && mContextHandle.get() == null;
            for (SimpleCallback<T> cb : mCallbacks) {
                if (cb instanceof Callback) {
                    Callback<T> callback = (Callback<T>) cb;
                    callback.setResult(this);
                    if (released)
                        callback.onContextReleased(mException, mObjectHandle);
                    else
                        callback.onException(mException, mObjectHandle);
                } else {
                    cb.onResult(result, err, mObjectHandle);
                }
            }
            mObjectHandle = null;
            mCallbacks.clear();
            notifyAll();
        }
        synchronized (this) {
            if (mForwardTo != null)
                mForwardTo.onExceptionWithResult(err, result, fromCache);
        }
    }

    /**
     * Synchronously wait for the result to be delivered.
     *
     * @param millis The amount of time to wait before timing out
     * @throws TimeoutException
     */
    public void syncWait(long millis) throws TimeoutException {
        synchronized (this) {
            if (!mDataSet) {
                try {
                    long now = System.currentTimeMillis();
                    wait(millis);
                    if (!mDataSet && (now + millis) < System.currentTimeMillis()) {
                        throw new TimeoutException();
                    }
                } catch (InterruptedException err) {
                }
            }
        }
    }

    /**
     * Leverage a ContextHandle to ensure that callbacks are only executed while the
     * ContextHandle is not released.
     *
     * @param handle The ContextHandle
     * @return The result
     */
    public Result<T> withHandle(ContextHandle handle) {
        mContextHandle = handle;
        return this;
    }

    public interface SimpleCallback<T> {

        void onResult(T result, Exception err, Object handle);

    }

    /**
     * A callback interface
     *
     * @param <T>
     */
    public interface Callback<T> extends SimpleCallback<T> {

        Result<T> getResult();

        void setResult(Result<T> result);

        void onResult(T result);

        void onResult(T result, Object handle);

        void onCacheResult(T result);

        void onCacheResult(T result, Object handle);

        void onException(Exception err);

        void onException(Exception err, Object handle);

        void onContextReleased(T result, Object handle);

        void onContextReleased(Exception err, Object handle);

    }

    /**
     * A callback adapter that allows for only overriding certain methods
     *
     * @param <T>
     */
    public static class CallbackAdapter<T> implements Callback<T> {

        static final String TAG = CallbackAdapter.class.getSimpleName();

        private Result<T> mResult;

        @Override
        public Result<T> getResult() {
            return mResult;
        }

        @Override
        public void setResult(Result<T> result) {
            mResult = result;
        }

        @Override
        public void onCacheResult(T result) {
            onResult(result);
        }

        @Override
        public void onCacheResult(T result, Object handle) {
            onCacheResult(result);
        }

        @Override
        public void onResult(T result) {
        }

        @Override
        public void onResult(T result, Object handle) {
            onResult(result);
        }

        @Override
        public void onResult(T result, Exception err, Object handle) {
            if (err != null)
                onException(err);
            if (result != null)
                onResult(result, handle);
        }

        @Override
        public void onException(Exception err) {
            Log.e(TAG, "onException", err);
        }

        @Override
        public void onException(Exception err, Object handle) {
            onException(err);
        }

        @Override
        public void onContextReleased(T result, Object handle) {
        }

        @Override
        public void onContextReleased(Exception err, Object handle) {
        }

    }

}