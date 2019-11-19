/**
 * Copyright 2016 YouVersion
 * <p>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei3.ui.view

import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.BitmapImageViewTarget
import io.nuclei3.R.styleable

open class NucleiImageView : AppCompatImageView {
    internal var mCircle = false
    var downloadState = -1
    internal var mRadius = 0f
    private var mRatio = 0f
    private var mPlaceholderId = 0
    var imageURI: Uri? = null
        private set
    private var mListener: GlideDrawableListener? = null
    private var mSet = false
    private var mBorderRect: RectF? = null
    private var mBorderPaint: Paint? = null

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    protected val strategy: DiskCacheStrategy?
        get() = DiskCacheStrategy.ALL

    protected open fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a: TypedArray = context.obtainStyledAttributes(attrs,
                styleable.NucleiImageView, defStyleAttr, defStyleRes)
        mCircle = a.getBoolean(styleable.NucleiImageView_circle, false)
        mRatio = a.getFloat(styleable.NucleiImageView_ratio, 0f)
        mRadius = a.getDimensionPixelSize(styleable.NucleiImageView_radius, 0).toFloat()
        mPlaceholderId = a.getResourceId(styleable.NucleiImageView_placeholder, 0)
        if (a.hasValue(styleable.NucleiImageView_border_color)) {
            val borderColor = a.getColor(styleable.NucleiImageView_border_color, 0)
            val borderWidth = a.getDimensionPixelSize(styleable.NucleiImageView_border_width, 0).toFloat()
            setBorder(borderColor, borderWidth)
        }
        val uri: String? = a.getString(styleable.NucleiImageView_url)
        val preload = a.getBoolean(styleable.NucleiImageView_preload, true)
        a.recycle()
        if (preload && !isInEditMode) {
            if (uri == null) {
                setPlaceHolder()
            } else setImageURI(uri)
        }
    }

    private fun setPlaceHolder() {
        if (mPlaceholderId > 0) {
            if (mCircle || mRadius > 0) {
                Glide.with(context)
                        .asBitmap()
                        .load(mPlaceholderId)
                        .diskCacheStrategy(strategy!!)
                        .into(newTarget())
            } else Glide.with(context).load(mPlaceholderId).into(this)
        }
    }

    private fun newTarget(): BitmapImageViewTarget {
        return object : BitmapImageViewTarget(this) {
            fun newDrawable(drawable: Drawable?): Drawable? {
                (drawable as? BitmapDrawable)?.let {
                    return newDrawable(it.bitmap)
                }
                return drawable
            }

            fun newDrawable(bitmap: Bitmap?): Drawable? {
                if (bitmap == null) return null
                val d = RoundedBitmapDrawableFactory.create(view.resources, bitmap)
                if (mCircle) d.isCircular = true else d.cornerRadius = mRadius
                return d
            }

            override fun onLoadStarted(placeholder: Drawable?) {
                super.onLoadStarted(placeholder)
                view.setImageDrawable(newDrawable(placeholder))
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                super.onLoadCleared(placeholder)
                view.setImageDrawable(newDrawable(placeholder))
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                view.setImageDrawable(newDrawable(errorDrawable))
            }

            override fun setResource(resource: Bitmap?) {
                view.setImageDrawable(newDrawable(resource))
            }
        }
    }

    fun clearBorder() {
        mBorderPaint = null
        mBorderRect = null
        invalidate()
    }

    fun setBorder(@ColorInt color: Int, width: Float) {
        if (mBorderPaint == null) {
            mBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mBorderPaint!!.style = Style.STROKE
        }
        if (mBorderRect == null) mBorderRect = RectF()
        mBorderPaint!!.color = color
        mBorderPaint!!.strokeWidth = width
        invalidate()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (mListener != null) mListener!!.onDrawable(this, drawable)
    }

    fun setListener(onDrawable: (NucleiImageView, Drawable?) -> Unit) {
        mListener = object : GlideDrawableListener {
            override fun onDrawable(imageView: NucleiImageView, drawable: Drawable?) {
                onDrawable.invoke(imageView, drawable)
            }
        }
    }

    fun setAspectRatio(ratio: Float) {
        mRatio = ratio
        requestLayout()
    }

    fun setUrl(url: String?) {
        setImageURI(url)
    }

    fun setPlaceholderId(placeholderId: Int) {
        mPlaceholderId = placeholderId
        setImageURI(imageURI)
    }

    fun setImageURI(uri: String?) {
        setImageURI(if (uri == null) null else Uri.parse(uri))
    }

    override fun setImageURI(uri: Uri?) {
        if (isInEditMode) return
        val downloadStatus = if (downloadState == -1) defaultDownloadState else downloadState
        if (downloadStatus == DOWNLOAD_STATE_NONE) {
            if (mPlaceholderId == 0) {
                Glide.with(this).clear(this)
                setImageDrawable(null)
            } else {
                setPlaceHolder()
            }
            return
        }
        mSet = true
        try {
            imageURI = uri
            val mgr = Glide.with(context)
            if (uri == null) {
                if (mPlaceholderId == 0) {
                    Glide.with(this).clear(this)
                    setImageDrawable(null)
                } else {
                    setPlaceHolder()
                }
            } else {
                if (mCircle || mRadius > 0) {
                    if (mPlaceholderId != 0) {
                        mgr.asBitmap()
                                .onlyRetrieveFromCache(downloadStatus == DOWNLOAD_STATE_CACHE_ONLY)
                                .load(uri)
                                .diskCacheStrategy(strategy!!)
                                .placeholder(mPlaceholderId)
                                .error(mPlaceholderId)
                                .fallback(mPlaceholderId)
                                .into(newTarget())
                    } else {
                        mgr.asBitmap()
                                .onlyRetrieveFromCache(downloadStatus == DOWNLOAD_STATE_CACHE_ONLY)
                                .load(uri)
                                .diskCacheStrategy(strategy!!)
                                .into(newTarget())
                    }
                } else {
                    if (mPlaceholderId != 0) {
                        mgr.asBitmap()
                                .onlyRetrieveFromCache(downloadStatus == DOWNLOAD_STATE_CACHE_ONLY)
                                .load(uri)
                                .diskCacheStrategy(strategy!!)
                                .placeholder(mPlaceholderId)
                                .error(mPlaceholderId)
                                .fallback(mPlaceholderId)
                                .into(this)
                    } else {
                        mgr.asBitmap()
                                .onlyRetrieveFromCache(downloadStatus == DOWNLOAD_STATE_CACHE_ONLY)
                                .load(uri)
                                .diskCacheStrategy(strategy!!)
                                .into(this)
                    }
                }
            }
        } catch (err: IllegalArgumentException) {
            Log.e(TAG, "Error setImageURI", err)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mRatio == 0f) super.onMeasure(widthMeasureSpec, heightMeasureSpec) else {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = (width / mRatio).toInt()
            setMeasuredDimension(width, height)
        }
        if (mBorderRect != null) {
            val borderWidth = (mBorderPaint?.strokeWidth ?: 0f) / 2f
            mBorderRect!!.set(
                    borderWidth,
                    borderWidth,
                    measuredWidth.toFloat() - borderWidth,
                    measuredHeight.toFloat() - borderWidth
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!mSet && (imageURI != null || mPlaceholderId != 0)) setImageURI(imageURI)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mSet = false
        var clearCtx = true
        try {
            val ctx: Context? = context
            if (ctx == null) clearCtx = false
            if (context is Activity) {
                val act = context as Activity
                if (act.isFinishing || act.isDestroyed) {
                    clearCtx = false
                }
            }
            if (clearCtx) Glide.with(ctx!!).clear(this)
        } catch (err: IllegalArgumentException) {
            Log.e(TAG, "Error onDetachedFromWindow", err)
        }
        setImageDrawable(null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mBorderRect != null && mBorderPaint != null) {
            when {
                mRadius > 0 -> canvas.drawRoundRect(mBorderRect!!, mRadius, mRadius, mBorderPaint!!)
                mCircle -> canvas.drawCircle(mBorderRect!!.centerX(), mBorderRect!!.centerY(), mBorderRect!!.width() / 2, mBorderPaint!!)
                else -> canvas.drawRect(mBorderRect!!, mBorderPaint!!)
            }
        }
    }

    interface GlideDrawableListener {
        fun onDrawable(imageView: NucleiImageView, drawable: Drawable?)
    }

    companion object {
        const val ASPECT_RATIO_16x9 = 1.77777777778f
        const val ASPECT_RATIO_1x1 = 1f
        const val DOWNLOAD_STATE_ALL = 1
        const val DOWNLOAD_STATE_CACHE_ONLY = 2
        const val DOWNLOAD_STATE_NONE = 3
        private val TAG: String = NucleiImageView::class.java.simpleName
        var defaultDownloadState = DOWNLOAD_STATE_ALL

    }
}