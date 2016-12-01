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
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.nuclei.R;
import nuclei.task.ContextHandle;
import nuclei.logs.Log;
import nuclei.logs.Logs;
import nuclei.ui.ContextViewOnClickListener;

public class ShareFragment extends BottomSheetDialogFragment {

    static final Log LOG = Logs.newLog(ShareFragment.class);
    static final int REQUEST_CODE = 101;
    static final int PERMISSION_REQUEST_CODE = 102;

    private ContextHandle mHandle;

    RecyclerView mTargetsView;
    TargetsAdapter mAdapter;
    Intent mShareIntent;
    ShareIntent mManager;
    ResolveInfo mInfo;
    boolean mShareSuccess;

    /**
     * Get a managed context handle.
     *
     * When the context is destroyed, the handle will be released.
     *
     * @return The Context Handle
     */
    public ContextHandle getContextHandle() {
        if (mHandle == null)
            mHandle = ContextHandle.obtain(getActivity());
        return mHandle;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShareIntent.Builder builder = savedInstanceState != null
                                      ? ShareUtil.getBuilder(savedInstanceState)
                                      : ShareUtil.getBuilder(getArguments());
        if (builder != null) {
            mManager = builder.build();
        } else {
            mManager = ShareIntent.newBuilder().build();
        }
        mShareIntent = mManager.createDefaultShareIntent(getActivity());
        mAdapter = new TargetsAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cyto_fragment_share, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = view.getContext();
        // TODO: Adjust span count?
        mTargetsView = (RecyclerView) view.findViewById(R.id.targets);
        mTargetsView.setLayoutManager(new GridLayoutManager(context, 4));
        mTargetsView.setAdapter(mAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ShareUtil.setBuilder(outState, mManager);
        outState.putParcelable("info", mInfo);
        outState.putParcelable("shareIntent", mShareIntent);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mInfo = savedInstanceState.getParcelable("info");
            mShareIntent = savedInstanceState.getParcelable("shareIntent");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PERMISSION_REQUEST_CODE == requestCode && grantResults.length > 0) {
            if (getParentFragment() instanceof OnShareListener) {
                mShareIntent = mManager.startActivityForResult(this, mInfo, REQUEST_CODE, PERMISSION_REQUEST_CODE);
                if (mShareIntent != null)
                    ((OnShareListener) getParentFragment()).onShareStart(mShareIntent);
            } else if (getActivity() instanceof OnShareListener) {
                mShareIntent = mManager.startActivityForResult(this, mInfo, REQUEST_CODE, PERMISSION_REQUEST_CODE);
                if (mShareIntent != null)
                    ((OnShareListener) getActivity()).onShareStart(mShareIntent);
            } else
                mShareIntent = mManager.startActivityForResult(getActivity(), mInfo, REQUEST_CODE, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            mShareSuccess = resultCode == Activity.RESULT_OK;
            if (getActivity() instanceof OnShareListener)
                ((OnShareListener) getActivity()).onShareFinish(mShareIntent, mShareSuccess);
            if (mShareIntent != null && mShareIntent.hasExtra(Intent.EXTRA_STREAM)) {
                try {
                    Uri uri = mShareIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                    getActivity().revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception err) {
                    LOG.e("Error revoking permission", err);
                }
            }
            if (getDialog() != null) {
                View view = getDialog().findViewById(R.id.design_bottom_sheet);
                if (view != null) {
                    BottomSheetBehavior behavior = BottomSheetBehavior.from(view);
                    behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                        @Override
                        public void onStateChanged(@NonNull View bottomSheet, int newState) {
                            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                                try {
                                    dismiss();
                                } catch (Exception err) {
                                    LOG.e("Share dialog may have already been dismissed", err);
                                }
                            }
                        }

                        @Override
                        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                        }
                    });
                    try {
                        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    } catch (NullPointerException err) { // happens sometimes, fairly certain this is because of the view being destroyed
                        LOG.e("Error changing behavior state, bottom sheet failed.", err);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTargetsView = null;
        mAdapter.mActivities = null;
        mAdapter.mInflater = null;
        mAdapter = null;
        mManager = null;
        mShareIntent = null;
        if (mHandle != null)
            mHandle.release();
        mHandle = null;
    }

    class TargetViewHolder extends RecyclerView.ViewHolder {

        final ImageView icon;
        final TextView label;
        ResolveInfo info;

        public TargetViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            label = (TextView) itemView.findViewById(R.id.label);

            itemView.setOnClickListener(new ContextViewOnClickListener(getContextHandle()) {
                @Override
                public void onClick(Context context, View v) {
                    mInfo = info;
                    if (getParentFragment() instanceof OnShareListener) {
                        mShareIntent = mManager.startActivityForResult(ShareFragment.this, info, REQUEST_CODE, PERMISSION_REQUEST_CODE);
                        if (mShareIntent != null)
                            ((OnShareListener) getParentFragment()).onShareStart(mShareIntent);
                    } else if (getActivity() instanceof OnShareListener) {
                        mShareIntent = mManager.startActivityForResult(ShareFragment.this, info, REQUEST_CODE, PERMISSION_REQUEST_CODE);
                        if (mShareIntent != null)
                            ((OnShareListener) getActivity()).onShareStart(mShareIntent);
                    } else
                        mShareIntent = mManager.startActivityForResult((Activity) context, info, REQUEST_CODE, PERMISSION_REQUEST_CODE);
                }
            });
        }
    }

    class TargetsAdapter extends RecyclerView.Adapter<TargetViewHolder> {

        List<ResolveInfo> mActivities;
        LayoutInflater mInflater;

        public TargetsAdapter() {
            mInflater = LayoutInflater.from(getActivity());
            mActivities = new ArrayList<>();
            List<ResolveInfo> resolveInfos = mManager.queryIntentActivities(getActivity(), mShareIntent);
            mManager.sortActivities(getActivity(), mShareIntent, resolveInfos);
            mActivities = resolveInfos;
        }

        @Override
        public TargetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TargetViewHolder(mInflater.inflate(R.layout.cyto_view_share, parent, false));
        }

        @Override
        public void onBindViewHolder(TargetViewHolder holder, int position) {
            ResolveInfo i = mActivities.get(position);
            holder.info = i;
            holder.label.setText(i.loadLabel(getActivity().getApplicationContext().getPackageManager()));
            holder.icon.setImageDrawable(i.loadIcon(getActivity().getApplicationContext().getPackageManager()));
        }

        @Override
        public int getItemCount() {
            return mActivities.size();
        }

    }

    public interface OnShareListener {

        void onShareStart(Intent intent);

        void onShareFinish(Intent intent, boolean success);

    }

}
