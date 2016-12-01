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
package nuclei.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.support.AnimationUtils;
import android.support.ViewOffsetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;

import java.lang.ref.WeakReference;

public class VisibilityAnimator {

    ValueAnimator mAnimator;
    final Callback mCallback;
    boolean mShowing;

    public VisibilityAnimator(Callback callback) {
        mCallback = callback;
    }

    public void show() {
        if (mAnimator == null) {
            mAnimator = new ValueAnimator();
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (mCallback != null)
                        mCallback.onAnimating(animation);
                }
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {
                private boolean mAnimating;
                @Override
                public void onAnimationStart(Animator animation) {
                    if (mCallback != null && !mAnimating && mCallback.getVisibility() != View.VISIBLE) {
                        mCallback.setVisibility(View.VISIBLE);
                    }
                    mAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mCallback != null && mAnimating && !mShowing)
                        mCallback.setVisibility(View.GONE);
                    mAnimating = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mAnimating = false;
                }
            });
            mAnimator.setDuration(300);
            mAnimator.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
            mCallback.onPrepare();
        }
        mAnimator.cancel();
        mShowing = true;
        mCallback.onShow(mAnimator);
        if (mAnimator.getValues() != null)
            mAnimator.start();
        else if (mCallback != null)
            mCallback.setVisibility(View.VISIBLE);
    }

    public void hide() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mShowing = false;
            mCallback.onHide(mAnimator);
            if (mAnimator.getValues() != null)
                mAnimator.start();
            else if (mCallback != null)
                mCallback.setVisibility(View.GONE);
        }
    }

    public interface Callback {

        int getVisibility();
        void setVisibility(int state);
        void onPrepare();
        void onAnimating(ValueAnimator animator);
        void onShow(ValueAnimator animator);
        void onHide(ValueAnimator animator);

    }

    public static class ViewOffsetCallback implements Callback {

        private final WeakReference<View> mView;
        private final WeakReference<ViewOffsetBehavior> mBehavior;

        public ViewOffsetCallback(View view) {
            mView = new WeakReference<>(view);
            mBehavior = new WeakReference<>((ViewOffsetBehavior)
                    ((CoordinatorLayout.LayoutParams) view.getLayoutParams()).getBehavior());
        }

        @Override
        public int getVisibility() {
            View view = mView.get();
            if (view != null)
                return view.getVisibility();
            return 0;
        }

        @Override
        public void setVisibility(int state) {
            View view = mView.get();
            if (view != null)
                view.setVisibility(state);
        }

        @Override
        public void onPrepare() {
            ViewOffsetBehavior behavior = mBehavior.get();
            View view = mView.get();
            if (behavior != null && view != null) {
                behavior.setTopAndBottomOffset(view.getHeight());
            }
        }

        @Override
        public void onAnimating(ValueAnimator animator) {
            ViewOffsetBehavior behavior = mBehavior.get();
            if (behavior != null)
                behavior.setTopAndBottomOffset((int) animator.getAnimatedValue());
        }

        @Override
        public void onShow(ValueAnimator animator) {
            ViewOffsetBehavior behavior = mBehavior.get();
            if (behavior != null)
                animator.setIntValues(behavior.getTopAndBottomOffset(), 0);
        }

        @Override
        public void onHide(ValueAnimator animator) {
            ViewOffsetBehavior behavior = mBehavior.get();
            if (behavior != null)
                animator.setIntValues(behavior.getTopAndBottomOffset(), getHeight());
        }

        protected int getHeight() {
            View view = mView.get();
            if (view != null)
                return view.getHeight();
            return 0;
        }

    }

}