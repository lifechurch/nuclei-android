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
package nuclei.ui.view;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class LoadingView {

    private ViewGroup mParent;
    private ProgressBar mView;
    private int mDismissed;

    private LoadingView(ViewGroup parent) {
        mParent = parent;
        ProgressBar progressBar = new ProgressBar(parent.getContext());
        progressBar.setIndeterminate(true);
        mView = progressBar;
    }

    public static LoadingView make(Activity activity, View view, boolean immediate) {
        ViewGroup parent = findSuitableParent(view);
        if (parent == null)
            parent = findSuitableParent(activity.findViewById(android.R.id.content));

        if (parent == null)
            throw new NullPointerException("Parent not found");

        LoadingView loadingView = new LoadingView(parent);
        if (immediate)
            loadingView.showNow();
        else
            loadingView.show();
        return loadingView;
    }

    @Nullable
    private static ViewGroup findSuitableParent(View view) {
        ViewGroup fallback = null;

        do {
            if (view instanceof CoordinatorLayout) {
                return (ViewGroup) view;
            }

            if (view instanceof FrameLayout) {
                fallback = (ViewGroup) view;
            }

            if (view != null) {
                ViewParent parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
        } while (view != null);

        return fallback;
    }

    public void dismiss() {
        mDismissed++;
        if (mView != null) {
            mParent.post(new Runnable() {
                @Override
                public void run() {
                    if (mDismissed >= 0) {
                        if (mView != null)
                            mParent.removeView(mView);
                        mView = null;
                        mParent = null;
                    }
                }
            });
        }
    }

    public void show() {
        mDismissed--;
        if (mParent != null) {
            mParent.postDelayed(new Runnable() {
                @Override
                public void run() {
                    immediateShow();
                }
            }, 1000);
        }
    }

    public void showNow() {
        mDismissed--;
        immediateShow();
    }

    private void immediateShow() {
        if (mDismissed >= 0)
            return;
        if (mView != null && mParent != null) {
            mParent.post(new Runnable() {
                @Override
                public void run() {
                    if (mDismissed < 0 && mParent != null && mView != null) {
                        if (mParent instanceof FrameLayout) {
                            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                            p.gravity = Gravity.CENTER;
                            mView.setLayoutParams(p);
                        } else if (mParent instanceof CoordinatorLayout) {
                            CoordinatorLayout.LayoutParams p = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                            p.gravity = Gravity.CENTER;
                            mView.setLayoutParams(p);
                        }
                        mParent.addView(mView);
                    }
                }
            });
        }
    }

}
