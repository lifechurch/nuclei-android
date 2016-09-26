/**
 * Copyright 2016 YouVersion
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import java.io.IOException;
import java.io.InputStream;

import io.nuclei.R;

public class GlideImageView extends AppCompatImageView {

    public static final float ASPECT_RATIO_16x9 = 1.77777777778f;
    public static final float ASPECT_RATIO_1x1 = 1;

    public static final int DOWNLOAD_STATE_ALL = 1;
    public static final int DOWNLOAD_STATE_CACHE_ONLY = 2;
    public static final int DOWNLOAD_STATE_NONE = 3;

    private static final String TAG = GlideImageView.class.getSimpleName();

    private static int sDefaultDownloadState = DOWNLOAD_STATE_ALL;

    public static int getDefaultDownloadState() {
        return sDefaultDownloadState;
    }

    public static void setDefaultDownloadState(int downloadState) {
        sDefaultDownloadState = downloadState;
    }

    private boolean mCircle = false;
    private int mDownloadState;
    private float mRadius;
    private float mRatio;
    private int mPlaceholderId;
    private Uri mUri;
    private GlideDrawableListener mListener;
    private boolean mSet;
    private RectF mBorderRect;
    private Paint mBorderPaint;

    public GlideImageView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public GlideImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public GlideImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    protected DiskCacheStrategy getStrategy() {
        return DiskCacheStrategy.ALL;
    }

    void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.GlideImageView, defStyleAttr, defStyleRes);

        mDownloadState = sDefaultDownloadState;

        mCircle = a.getBoolean(R.styleable.GlideImageView_circle, false);
        mRatio = a.getFloat(R.styleable.GlideImageView_ratio, 0);
        mRadius = a.getDimensionPixelSize(R.styleable.GlideImageView_radius, 0);
        mPlaceholderId = a.getResourceId(R.styleable.GlideImageView_placeholder, 0);

        if (a.hasValue(R.styleable.GlideImageView_border_color)) {
            int borderColor = a.getColor(R.styleable.GlideImageView_border_color, 0);
            float borderWidth = a.getDimensionPixelSize(R.styleable.GlideImageView_border_width, 0);
            setBorder(borderColor, borderWidth);
        }

        String uri = a.getString(R.styleable.GlideImageView_url);
        boolean preload = a.getBoolean(R.styleable.GlideImageView_preload, true);
        a.recycle();

        if (preload) {
            if (!isInEditMode()) {
                if (uri == null) {
                    setPlaceHolder();
                } else
                    setImageURI(uri);
            } else
                setPlaceHolder();
        }
    }

    public int getDownloadState() {
        return mDownloadState;
    }

    public void setDownloadState(int downloadState) {
        mDownloadState = downloadState;
    }

    private void setPlaceHolder() {
        if (mPlaceholderId > 0) {
            if (mCircle || mRadius > 0) {
                Glide.with(getContext()).load(mPlaceholderId)
                        .asBitmap()
                        .diskCacheStrategy(getStrategy())
                        .into(newTarget());
            } else
                Glide.with(getContext()).load(mPlaceholderId).into(this);
        }
    }

    private BitmapImageViewTarget newTarget() {
        return new BitmapImageViewTarget(this) {

            Drawable newDrawable(Drawable drawable) {
                if (drawable instanceof BitmapDrawable)
                    return newDrawable(((BitmapDrawable) drawable).getBitmap());
                return drawable;
            }

            Drawable newDrawable(Bitmap bitmap) {
                if (bitmap == null)
                    return null;
                RoundedBitmapDrawable d = RoundedBitmapDrawableFactory.create(view.getResources(), bitmap);
                if (mCircle)
                    d.setCircular(true);
                else
                    d.setCornerRadius(mRadius);
                return d;
            }

            @Override
            public void onLoadStarted(Drawable placeholder) {
                view.setImageDrawable(newDrawable(placeholder));
            }

            @Override
            public void onLoadCleared(Drawable placeholder) {
                view.setImageDrawable(newDrawable(placeholder));
            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                view.setImageDrawable(newDrawable(errorDrawable));
            }

            @Override
            protected void setResource(Bitmap resource) {
                view.setImageDrawable(newDrawable(resource));
            }
        };
    }

    public void clearBorder() {
        mBorderPaint = null;
        mBorderRect = null;
        invalidate();
    }

    public void setBorder(@ColorInt int color, float width) {
        if (mBorderPaint == null) {
            mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBorderPaint.setStyle(Paint.Style.STROKE);
        }
        if (mBorderRect == null)
            mBorderRect = new RectF();
        mBorderPaint.setColor(color);
        mBorderPaint.setStrokeWidth(width);
        invalidate();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (mListener != null)
            mListener.onDrawable(this, drawable);
    }

    public void setListener(GlideDrawableListener listener) {
        mListener = listener;
    }

    public void setAspectRatio(float ratio) {
        mRatio = ratio;
        requestLayout();
    }

    public void setUrl(String url) {
        setImageURI(url);
    }

    public void setPlaceholderId(int placeholderId) {
        mPlaceholderId = placeholderId;
        setImageURI(mUri);
    }

    public Uri getImageURI() {
        return mUri;
    }

    public void setImageURI(String uri) {
        setImageURI(uri == null ? null : Uri.parse(uri));
    }

    @Override
    public void setImageURI(Uri uri) {
        if (mDownloadState == DOWNLOAD_STATE_NONE) {
            if (mPlaceholderId == 0) {
                Glide.clear(this);
                setImageDrawable(null);
            } else {
                setPlaceHolder();
            }
            return;
        }
        if (isInEditMode())
            return;
        mSet = true;
        try {
            mUri = uri;
            RequestManager mgr = Glide.with(getContext());
            if (uri == null) {
                if (mPlaceholderId == 0) {
                    Glide.clear(this);
                    setImageDrawable(null);
                } else {
                    setPlaceHolder();
                }
            } else {
                if (mCircle || mRadius > 0) {
                    if (mPlaceholderId != 0) {
                        if (mDownloadState == DOWNLOAD_STATE_CACHE_ONLY)
                            mgr.using(CACHE_MODEL_LOADER)
                                    .load(uri)
                                    .asBitmap()
                                    .diskCacheStrategy(getStrategy())
                                    .placeholder(mPlaceholderId)
                                    .error(mPlaceholderId)
                                    .fallback(mPlaceholderId)
                                    .into(newTarget());
                        else
                            mgr.load(uri)
                                    .asBitmap()
                                    .diskCacheStrategy(getStrategy())
                                    .placeholder(mPlaceholderId)
                                    .error(mPlaceholderId)
                                    .fallback(mPlaceholderId)
                                    .into(newTarget());
                    } else {
                        if (mDownloadState == DOWNLOAD_STATE_CACHE_ONLY)
                            mgr.using(CACHE_MODEL_LOADER)
                                    .load(uri)
                                    .asBitmap()
                                    .diskCacheStrategy(getStrategy())
                                    .into(newTarget());
                        else
                            mgr.load(uri)
                                    .asBitmap()
                                    .diskCacheStrategy(getStrategy())
                                    .into(newTarget());
                    }
                } else {
                    if (mPlaceholderId != 0) {
                        if (mDownloadState == DOWNLOAD_STATE_CACHE_ONLY)
                            mgr.using(CACHE_MODEL_LOADER)
                                    .load(uri)
                                    .asBitmap()
                                    .diskCacheStrategy(getStrategy())
                                    .placeholder(mPlaceholderId)
                                    .error(mPlaceholderId)
                                    .fallback(mPlaceholderId)
                                    .into(this);
                        else
                            mgr.load(uri)
                                    .asBitmap()
                                    .diskCacheStrategy(getStrategy())
                                    .placeholder(mPlaceholderId)
                                    .error(mPlaceholderId)
                                    .fallback(mPlaceholderId)
                                    .into(this);
                    } else {
                        if (mDownloadState == DOWNLOAD_STATE_CACHE_ONLY)
                            mgr.using(CACHE_MODEL_LOADER)
                                    .load(uri)
                                    .asBitmap()
                                    .diskCacheStrategy(getStrategy())
                                    .into(this);
                        else
                            mgr.load(uri)
                                    .asBitmap()
                                    .diskCacheStrategy(getStrategy())
                                    .into(this);
                    }
                }
            }
        } catch (IllegalArgumentException err) {
            Log.e(TAG, "Error setImageURI", err);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mRatio == 0)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        else {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width / mRatio);
            setMeasuredDimension(width, height);
        }
        if (mBorderRect != null) {
            mBorderRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mSet && (mUri != null || mPlaceholderId != 0))
            setImageURI(mUri);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSet = false;
        try {
            Glide.clear(this);
        } catch (IllegalArgumentException err) {
            Log.e(TAG, "Error onDetachedFromWindow", err);
        }
        setImageDrawable(null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBorderRect != null && mBorderPaint != null) {
            if (mRadius > 0)
                canvas.drawRoundRect(mBorderRect, mRadius, mRadius, mBorderPaint);
            else if (mCircle)
                canvas.drawCircle(mBorderRect.centerX(), mBorderRect.centerY(), mBorderRect.width() / 2, mBorderPaint);
            else
                canvas.drawRect(mBorderRect, mBorderPaint);
        }
    }

    public interface GlideDrawableListener {

        void onDrawable(GlideImageView imageView, Drawable drawable);

    }

    private static final StreamModelLoader<Uri> CACHE_MODEL_LOADER = new StreamModelLoader<Uri>() {
        @Override
        public DataFetcher<InputStream> getResourceFetcher(final Uri model, int width, int height) {
            return new DataFetcher<InputStream>() {
                @Override
                public InputStream loadData(Priority priority) throws Exception {
                    throw new IOException();
                }

                @Override
                public void cleanup() {
                }

                @Override
                public String getId() {
                    return model.toString();
                }

                @Override
                public void cancel() {
                }
            };
        }
    };

}
