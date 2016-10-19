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
package nuclei.task.http;

import android.app.Application;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;

import nuclei.logs.Logs;
import nuclei.task.ContextHandle;
import nuclei.task.Result;
import nuclei.task.cache.SimpleCache;
import nuclei.task.Task;
import nuclei.task.TaskPool;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;

/**
 * Utility Class to handle setup and management of a single OkHttpClient instance and
 * an HTTP TaskPool.
 */
public final class Http {

    private static final nuclei.logs.Log LOG = Logs.newLog(Http.class);

    private static final long MAX_CACHE_SIZE = 10 * 1024 * 1024;
    private static OkHttpClient sClient;
    static SimpleCache sCache;
    private static OkUrlFactory sUrlFactory;
    private static TaskPool sHttpPool;

    private Http() {
    }

    /**
     * Let the default OkHttpClient instance be generated here.
     */
    public static void initialize(Application application) {
        initialize(application, null);
    }

    /**
     * Let the default OkHttpClient instance be generated here.
     */
    public static void initialize(Application application, HttpPoolBuilder builder) {
        if (sHttpPool != null)
            throw new IllegalStateException("Already initialized");
        OkHttpClient client = new OkHttpClient.Builder()
                .cache(new okhttp3.Cache(new File(application.getCacheDir(), "neuron-http"), MAX_CACHE_SIZE))
                .build();
        OkUrlFactory factory = new OkUrlFactory(client);
        try {
            URL.setURLStreamHandlerFactory(factory);
        } catch (Throwable err) {
            if (!"factory already defined".equals(err.getMessage()))
                LOG.i("Error setting stream handler", err);
        }
        SimpleCache cache = new SimpleCache(new File(application.getCacheDir(), "neuron-object"), MAX_CACHE_SIZE);
        initialize(application, client, cache, factory, builder);
    }

    /**
     * Supply your own instance of OkHttpClient
     */
    public static void initialize(Application application,
                                  OkHttpClient client,
                                  SimpleCache cache,
                                  OkUrlFactory factory,
                                  HttpPoolBuilder builder) {
        if (sHttpPool != null)
            throw new IllegalStateException("Already initialized");
        sClient = client;
        sUrlFactory = factory;
        sHttpPool = builder == null ? TaskPool.newBuilder(TaskPool.HTTP_POOL).build() : builder.build();
        sCache = cache;
    }

    @VisibleForTesting
    public static void destroy() {
        if (sHttpPool != null)
            sHttpPool.shutdown();
        sHttpPool = null;
        sClient = null;
        sUrlFactory = null;
        if (sCache != null)
            try {
                sCache.flush();
                sCache.close();
                sCache = null;
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
    }

    /**
     * Get the default OkHttpClient instance
     *
     * @return The OkHttpClient
     */
    public static OkHttpClient getClient() {
        return sClient;
    }

    @Deprecated
    public static OkUrlFactory getUrlFactory() {
        return sUrlFactory;
    }

    /**
     * The Cache associated with the OkHttpClient
     *
     * @return The Cache
     */
    public static okhttp3.Cache getHttpCache() {
        return sClient.cache();
    }

    /**
     * The Cache associated with the HttpTask
     *
     * @return The Cache
     */
    public static SimpleCache getObjectCache() {
        return sCache;
    }

    /**
     * Evict this task from Cache
     *
     * @param task The task to generate the URL from
     * @return True if evicted, false otherwise
     *
     * @throws IOException
     */
    public static boolean evict(HttpTask<?> task) throws IOException {
        final String taskUrl = task.getUrl();
        Iterator<String> urls = getHttpCache().urls();
        try {
            while (urls.hasNext()) {
                String url = urls.next();
                if (url.equals(taskUrl)) {
                    urls.remove();
                    return true;
                }
            }
        } catch (NoSuchElementException ignore) {}
        Iterator<String> keys = getObjectCache().keys();
        try {
            while (keys.hasNext()) {
                String key = urls.next();
                if (key.equals(task.getUrl())) {
                    keys.remove();
                    return true;
                }
            }
        } catch (NoSuchElementException ignore) {}
        return false;
    }

    /**
     * Execute an HTTP Task on the default Http Pool
     *
     * @param task
     * @return The Result of the Task
     */
    public static <T> Result<T> execute(HttpTask<T> task) {
        if (sHttpPool == null)
            throw new IllegalStateException("Http Pool NOT initialized");
        return sHttpPool.execute(task);
    }

    /**
     * Execute an HTTP Task on the default Http Pool
     *
     * @param handle The ContextHandle to attach to the task and Result
     * @param task
     * @return The Result of the Task
     */
    public static <T> Result<T> execute(ContextHandle handle, HttpTask<T> task) {
        if (sHttpPool == null)
            throw new IllegalStateException("Http Pool NOT initialized");
        return sHttpPool.execute(handle, task);
    }

    /**
     * @see TaskPool#executeNow(Task)
     */
    public static <T> T executeNow(HttpTask<T> task) {
        if (sHttpPool == null)
            throw new IllegalStateException("Http Pool NOT initialized");
        return sHttpPool.executeNow(task);
    }

    /**
     * @see TaskPool#executeNowResult(Task)
     */
    public static <T> Result<T> executeNowResult(HttpTask<T> task) {
        if (sHttpPool == null)
            throw new IllegalStateException("Http Pool NOT initialized");
        return sHttpPool.executeNowResult(task);
    }

    public interface HttpPoolBuilder {
        TaskPool build();
    }

}
