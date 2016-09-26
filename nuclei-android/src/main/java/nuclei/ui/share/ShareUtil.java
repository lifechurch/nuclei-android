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
package nuclei.ui.share;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import nuclei.task.ContextHandle;
import nuclei.task.Result;
import nuclei.task.http.Http;
import nuclei.task.http.HttpException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class ShareUtil {

    protected static final String BUILDER = "builder";
    private static final String SHARE_INTENT = "share_intent";

    public static ShareIntent.Builder getBuilder(Intent intent) {
        return intent.getParcelableExtra(BUILDER);
    }

    public static ShareIntent.Builder getBuilder(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return null;
        return savedInstanceState.getParcelable(BUILDER);
    }

    public static void setBuilder(Bundle savedInstanceState, ShareIntent intent) {
        savedInstanceState.putParcelable(BUILDER, intent.builder());
    }

    public static Intent getShareIntent(Intent intent) {
        return intent.getParcelableExtra(SHARE_INTENT);
    }

    public static void setShareIntent(Intent intent, Intent shareIntent) {
        intent.putExtra(SHARE_INTENT, shareIntent);
    }

    public static File newShareFile(Context context, String name) {
        return newShareFile(context.getCacheDir(), name);
    }

    public static File newShareFile(File parent, String name) {
        File directory = new File(parent, "cyto_share_provider");
        if (!directory.exists())
            directory.mkdirs();
        else {
            for (File child : directory.listFiles()) {
                child.delete();
            }
        }
        return new File(directory, System.currentTimeMillis() + "_" + name);
    }

    public static Result<File> downloadImageToShare(final ContextHandle handle, String url) {
        Uri uri = Uri.parse(url);
        return downloadImageToShare(handle, url, uri.getLastPathSegment());
    }

    public static Result<File> downloadImageToShare(final ContextHandle handle, String url, final String name) {
        final Result<File> pending = new Result<>();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Http.getClient()
                .newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, final IOException e) {
                        Context context = handle.get();
                        onException(pending, context, e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Context context = handle.get();
                        if (context == null) {
                            response.body().close();
                            pending.onException(new Exception("Handle Detached"));
                            return;
                        }
                        if (!response.isSuccessful()) {
                            onException(pending, context, new HttpException(response.message(), response.code()));
                            return;
                        }
                        final File shareFile = ShareUtil.newShareFile(context, name == null ? "share_image" : name);
                        try {
                            InputStream in = response.body().byteStream();
                            long len = response.body().contentLength();
                            if (len < 0)
                                len = 1024 * 1024 * 1024;
                            byte[] buf = new byte[8096];
                            int r;
                            FileOutputStream out = new FileOutputStream(shareFile);
                            try {
                                while (len > 0) {
                                    r = in.read(buf);
                                    if (r == -1)
                                        break;
                                    out.write(buf, 0, r);
                                    len -= r;
                                }
                            } finally {
                                out.flush();
                                out.close();
                            }
                            if (context instanceof Activity)
                                ((Activity) context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pending.onResult(shareFile);
                                    }
                                });
                            else
                                pending.onResult(shareFile);
                        } catch (Exception err) {
                            onException(pending, context, err);
                        } finally {
                            response.body().close();
                        }
                    }
                });
        return pending;
    }

    static void onException(final Result<File> pending, Context context, final Exception err) {
        if (context instanceof Activity)
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pending.onException(err);
                }
            });
        else
            pending.onException(err);
    }

}
