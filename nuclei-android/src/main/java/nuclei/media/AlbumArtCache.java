/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.media;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import nuclei.logs.Log;
import nuclei.logs.Logs;

/**
 * Implements a basic cache of album arts, with async loading support.
 */
public final class AlbumArtCache {

    static final Log LOG = Logs.newLog(AlbumArtCache.class);

    private static final int MAX_ART_WIDTH = 800;  // pixels
    private static final int MAX_ART_HEIGHT = 480;  // pixels

    private static final int MAX_ART_WIDTH_ICON = 128;  // pixels
    private static final int MAX_ART_HEIGHT_ICON = 128;  // pixels

    private static final int MEMORY_CLASS = 32;

    private static final AlbumArtCache INSTANCE = new AlbumArtCache();

    public static AlbumArtCache getInstance() {
        return INSTANCE;
    }

    private AlbumArtCache() {
    }

    public void fetch(final Context context, final String artUrl, final FetchListener listener) {
        if (context == null || TextUtils.isEmpty(artUrl) || listener == null)
            return;
        ActivityManager mgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mgr.isLowRamDevice())
            return;
        int memoryClass = mgr.getMemoryClass();
        int width = MAX_ART_WIDTH;
        int height = MAX_ART_HEIGHT;
        int widthIcon = MAX_ART_WIDTH_ICON;
        int heightIcon = MAX_ART_HEIGHT_ICON;
        if (memoryClass <= MEMORY_CLASS) {
            width /= 2;
            height /= 2;
            widthIcon /= 2;
            heightIcon /= 2;
        }

        Glide.with(context)
                .load(artUrl)
                .asBitmap()
                .dontAnimate()
                .dontTransform()
                .override(width, height)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .format(DecodeFormat.PREFER_RGB_565)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(final Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
                        try {
                            listener.onFetchedImage(artUrl, bitmap);
                        } catch (Throwable err) {
                            listener.onError(artUrl, new Exception(err));
                        }
                    }
                });
        Glide.with(context)
                .load(artUrl)
                .asBitmap()
                .dontAnimate()
                .dontTransform()
                .override(widthIcon, heightIcon)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .format(DecodeFormat.PREFER_RGB_565)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(final Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
                        try {
                            listener.onFetchedIcon(artUrl, bitmap);
                        } catch (Throwable err) {
                            listener.onError(artUrl, new Exception(err));
                        }
                    }
                });
    }

    public Bitmap fetchIconSync(Context context, String artUrl) {
        ActivityManager mgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mgr.isLowRamDevice())
            return null;
        int memoryClass = mgr.getMemoryClass();
        int widthIcon = MAX_ART_WIDTH_ICON;
        int heightIcon = MAX_ART_HEIGHT_ICON;
        if (memoryClass <= MEMORY_CLASS) {
            widthIcon /= 2;
            heightIcon /= 2;
        }
        try {
            return Glide.with(context)
                    .load(artUrl)
                    .asBitmap()
                    .dontAnimate()
                    .dontTransform()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .into(widthIcon, heightIcon)
                    .get();
        } catch (Exception err) {
            return null;
        }
    }

    public abstract static class FetchListener {
        public abstract void onFetchedImage(String artUrl, Bitmap image);
        public abstract void onFetchedIcon(String artUrl, Bitmap icon);

        public void onError(String artUrl, Exception e) {
            LOG.e("AlbumArtFetchListener: error while downloading " + artUrl, e);
        }
    }
}
