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
package nuclei3.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import io.nuclei3.R;

public class HeaderImageView extends NucleiImageView {

    public HeaderImageView(Context context) {
        super(context);
    }

    public HeaderImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HeaderImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.HeaderImageView, defStyleAttr, defStyleRes);

        String uri = a.getString(R.styleable.HeaderImageView_url);
        if (uri != null)
            setImageURI(uri);

        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec,
                             int heightMeasureSpec) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = width * drawable.getIntrinsicHeight() / drawable.getIntrinsicWidth();
            setMeasuredDimension(width, height);
        } else
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
