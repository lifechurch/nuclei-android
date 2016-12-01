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
package nuclei.ui;

import android.app.Activity;
import android.support.v4.view.ViewCompat;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

import nuclei.ui.view.LoadingView;

public class LoadingManager implements Destroyable {

    Activity mActivity;
    LoadingView mLoadingView;
    final Set<Integer> mLoadingTokens = new HashSet<>();
    int mLoadingId;

    public LoadingManager(Activity activity) {
        mActivity = activity;
    }

    public int showLoading(View view) {
        return showLoading(view, false);
    }

    public synchronized int showLoading(View view, final boolean immediate) {
        if (view == null)
            view = mActivity.findViewById(android.R.id.content);
        if (view == null)
            return 0;
        final Integer id = ++mLoadingId;
        mLoadingTokens.add(id);
        showLoading(id, view, immediate);
        return id;
    }

    void showLoading(final Integer id, final View view, final boolean immediate) {
        if (mActivity != null)
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mLoadingTokens.contains(id)) {
                        if (mLoadingView == null && ViewCompat.isAttachedToWindow(view)) {
                            mLoadingView = LoadingView.make(mActivity, view, immediate);
                        } else if (mLoadingView == null) {
                            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                                @Override
                                public void onViewAttachedToWindow(View v) {
                                    showLoading(id, view, immediate);
                                    view.removeOnAttachStateChangeListener(this);
                                }

                                @Override
                                public void onViewDetachedFromWindow(View v) {
                                    view.removeOnAttachStateChangeListener(this);
                                }
                            });
                        }
                    }
                }
            });
    }

    public void hideLoading(final int tokenId) {
        if (mActivity != null)
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLoadingTokens.remove(tokenId);
                    if (mLoadingTokens.size() == 0) {
                        if (mLoadingView != null) {
                            mLoadingView.dismiss();
                            mLoadingView = null;
                        }
                    }
                }
            });
    }

    @Override
    public void onDestroy() {
        mActivity = null;
        if (mLoadingView != null)
            mLoadingView.dismiss();
        mLoadingView = null;
    }

}
