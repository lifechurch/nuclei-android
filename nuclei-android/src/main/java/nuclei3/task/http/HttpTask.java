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
package nuclei3.task.http;

import android.content.Context;

import java.io.IOException;

import nuclei3.task.cache.SimpleCache;
import nuclei3.logs.Log;
import nuclei3.logs.Logs;
import nuclei3.task.Task;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.http.HttpMethod;

/**
 * An HttpTask to be used with a TaskPool.
 * <br />
 * This utilizes the default OkHttpClient instance
 * from the NeuronHttp class.
 */
public abstract class HttpTask<T> extends Task<T> {

    private static final Log LOG = Logs.newLog(HttpTask.class);

    private String url;
    private boolean cache;

    @Override
    public String getId() {
        return getUrl();
    }

    protected String getLogKey() {
        return getUrl();
    }

    protected int getCacheSeconds() {
        return Integer.MAX_VALUE;
    }

    protected boolean shouldReCache(Context context, SimpleCache.Entry entry) {
        return isCacheExpired(context, entry);
    }

    protected boolean isCacheExpired(Context context, SimpleCache.Entry entry) {
        return entry == null || entry.isExpired();
    }

    protected boolean shouldCache() {
        return cache;
    }

    @Override
    public final void run(Context context) {
        boolean cached = false;
        try {
            if (shouldCache() && Http.sCache != null) {
                SimpleCache.Entry entry = Http.sCache.get(getUrl());
                T object = onLoadCache(context, entry);
                if (object != null) {
                    cached = true;
                    onComplete(object);
                    if (!shouldReCache(context, entry))
                        return;
                }
            }
            Request request = toRequest(context);
            Response response = execute(request);
            try {
                if (onResponse(context, response)) {
                    if (response.code() != 404 && !response.isSuccessful()) {
                        if (!cached)
                            onException(onHttpError(context, response));
                        else
                            throw onHttpError(context, response);
                    } else {
                        switch (response.code()) {
                            case 204: // no content
                                if (!cached)
                                    onComplete();
                                break;
                            case 404: // not found
                                if (!cached)
                                    onComplete(null);
                                break;
                            default:
                                T object = onDeserialize(context, response);
                                if (object != null && shouldCache() && Http.sCache != null)
                                    onCache(context, object);
                                if (!cached)
                                    onComplete(object, response.cacheResponse() != null);
                                break;
                        }
                    }
                }
            } finally {
                response.body().close();
            }
        } catch (Exception err) {
            if (!cached)
                onException(err);
            else
                LOG.e("Failed to re-cache a response (" + getLogKey() + ")", err);
        }
    }

    protected Response execute(Request request) throws IOException {
        if (Http.sCache != null) {
            if (HttpMethod.invalidatesCache(request.method())) {
                try {
                    Http.sCache.remove(getUrl());
                } catch (IOException ignore) {
                }
            }
            if (!"GET".equals(request.method())) {
                setShouldCache(false);
            }
        }
        OkHttpClient client = getClient();
        Call call = client.newCall(request);
        long start = System.currentTimeMillis();
        Response response = call.execute();
        if (LOG.isLoggable(Log.INFO))
            LOG.i("Took " + (System.currentTimeMillis() - start) + "ms to execute request (" + getLogKey() + ")");
        if (Http.sCache != null && HttpHeaders.hasVaryAll(response)) {
            setShouldCache(false);
        }
        return response;
    }

    protected void setShouldCache(boolean cache) {
        this.cache = cache;
    }

    protected T onLoadCache(Context context, SimpleCache.Entry entry) throws IOException {
        try {
            if (entry != null) {
                if (!isCacheExpired(context, entry)) {
                    T object = onLoadCacheEntry(context, entry);
                    if (object != null) {
                        if (LOG.isLoggable(Log.INFO))
                            LOG.i("Cache Hit (" + getLogKey() + ")");
                    }
                    return object;
                } else {
                    if (LOG.isLoggable(Log.INFO))
                        LOG.i("Cache Expired (" + getLogKey() + ")");
                    Http.sCache.remove(getUrl());
                }
            } else {
                if (LOG.isLoggable(Log.INFO))
                    LOG.i("Cache Miss (" + getLogKey() + ")");
            }
        } catch (Throwable err) {
            LOG.e("Error loading cache (" + getLogKey() + ")", err);
        } finally {
            Util.closeQuietly(entry);
        }
        return null;
    }

    protected void onSaveCache(Context context, SimpleCache.Entry entry, T object) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected T onLoadCacheEntry(Context context, SimpleCache.Entry entry) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected void onCache(Context context, T object) throws IOException {
        try {
            SimpleCache.Entry entry = Http.sCache.put(getUrl(), getCacheSeconds());
            if (entry != null) {
                try {
                    onSaveCache(context, entry, object);
                } catch (IOException err) {
                    LOG.e("Error saving cache (" + getUrl() + ")", err);
                    entry.abort();
                } finally {
                    Util.closeQuietly(entry);
                }
            }
        } catch (IOException err) {
            LOG.e("Error caching response (" + getUrl() + ")", err);
        }
    }

    /**
     * An override point to utilize a different OkHttpClient instance other than the default.
     *
     * @return
     */
    protected OkHttpClient getClient() {
        return Http.getClient();
    }

    /**
     * Generate an HttpException based on the Response
     *
     * @param context
     * @param response
     * @return The new HttpException based on the Response
     */
    protected HttpException onHttpError(Context context, Response response) throws Exception {
        return new HttpException(response.message(), response.code());
    }

    /**
     * An override point of where you can handle the entire Response process.
     *
     * @param context
     * @param response
     * @return True if you want to continue with the default Response handling,
     *         false if you wish to fully handle the Response yourself.
     */
    protected boolean onResponse(Context context, Response response) {
        return true;
    }

    /**
     * Deserialize the Response and return the deserialized model.
     *
     * @param context
     * @param response The Response
     * @return The deserialized model
     * @throws Exception
     */
    protected abstract T onDeserialize(Context context, Response response) throws Exception;

    /**
     * Append to the builder things like RequestBody and Request method
     *
     * @param context
     * @param builder
     * @throws Exception
     */
    protected void onBuildRequest(Context context, Request.Builder builder) throws Exception {
    }

    /**
     * Generate the URL for the Request
     *
     * @return The URL
     */
    protected abstract String toUrl();

    /**
     * Generate a Request.  Calls build(context, builder) passing a Builder with the generated URL.
     *
     * @param context
     * @return The built Request
     * @throws Exception
     */
    protected Request toRequest(Context context) throws Exception {
        Request.Builder builder = new Request.Builder().url(getUrl());
        onBuildRequest(context, builder);
        return builder.build();
    }

    /**
     * Get a cached copy of the URL
     *
     * @return The URL
     */
    public String getUrl() {
        if (url == null)
            url = toUrl();
        return url;
    }

}
