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

    final List<Callback<T>> mCallbacks = new ArrayList<>(1);

    T mData;
    Exception mException;
    boolean mDataSet;
    Object mObjectHandle;
    ContextHandle mContextHandle;

    public Result() {
    }

    public Result(T data) {
        mData = data;
        mDataSet = true;
    }

    /**
     * Get the result
     *
     * @return The result
     */
    public T get() {
        if (!mDataSet)
            throw new IllegalStateException("Result Not Ready");
        if (mException != null)
            throw new RuntimeException(mException);
        return mData;
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

    /**
     * Return true if the result is complete
     *
     * @return True if this result has been delivered
     */
    public boolean isComplete() {
        return mDataSet;
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
            boolean released = mContextHandle != null && mContextHandle.get() == null;
            for (Callback<T> callback : mCallbacks) {
                if (released) {
                    callback.onContextReleased(data, mObjectHandle);
                } else {
                    if (fromCache)
                        callback.onCacheResult(data, mObjectHandle);
                    else
                        callback.onResult(data, mObjectHandle);
                }
            }
            mObjectHandle = null;
            mCallbacks.clear();
            notifyAll();
        }
    }

    /**
     * Deliver an exception as the result
     *
     * @param err
     */
    public void onException(Exception err) {
        synchronized (this) {
            if (mDataSet)
                throw new IllegalStateException("Data already set");
            mDataSet = true;
            mException = err;
            boolean released = mContextHandle != null && mContextHandle.get() == null;
            for (Callback<T> callback : mCallbacks) {
                if (released)
                    callback.onContextReleased(mException, mObjectHandle);
                else
                    callback.onException(mException, mObjectHandle);
            }
            mObjectHandle = null;
            mCallbacks.clear();
            notifyAll();
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

    /**
     * A callback interface
     *
     * @param <T>
     */
    public interface Callback<T> {

        void onResult(T type);

        void onResult(T type, Object handle);

        void onCacheResult(T type);

        void onCacheResult(T type, Object handle);

        void onException(Exception err);

        void onException(Exception err, Object handle);

        void onContextReleased(T type, Object handle);

        void onContextReleased(Exception err, Object handle);

    }

    /**
     * A callback adapter that allows for only overriding certain methods
     *
     * @param <T>
     */
    public static class CallbackAdapter<T> implements Callback<T> {

        static final String TAG = CallbackAdapter.class.getSimpleName();

        @Override
        public void onCacheResult(T type) {
            onResult(type);
        }

        @Override
        public void onCacheResult(T type, Object handle) {
            onCacheResult(type);
        }

        @Override
        public void onResult(T type) {
        }

        @Override
        public void onResult(T type, Object handle) {
            onResult(type);
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
        public void onContextReleased(T type, Object handle) {
        }

        @Override
        public void onContextReleased(Exception err, Object handle) {
        }

    }

}