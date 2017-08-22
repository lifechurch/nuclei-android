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
package nuclei3.ui.share;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Map;

public final class ShareIntent {

    public static final String SHARING_AUTHORITY = "nuclei3.sharing.authority";

    static final String TAG = "ShareIntent";
    static final String MISSING_CONFIG = "Missing meta data key " + SHARING_AUTHORITY + " in AndroidManifest.xml, file sharing won't work properly";

    final String mText;
    final String mUrl;
    final String mSms;
    final String mEmail;
    final String mSubject;
    final File mFile;
    final Uri mUri;
    final ArrayMap<String, PackageTargetManager> mTargetListeners;
    final PackageTargetManager mDefaultTargetManager;

    ShareIntent(Builder builder) {
        mText = builder.mText;
        mUri = builder.mUri;
        mSms = builder.mSms;
        mEmail = builder.mEmail;
        mSubject = builder.mSubject;
        mUrl = builder.mUrl;
        mFile = builder.mFile;
        mTargetListeners = builder.mTargetListeners;
        mDefaultTargetManager = builder.mDefaultTargetManager;
    }

    public Builder builder() {
        Builder builder = new Builder();
        builder.mText = mText;
        builder.mUri = mUri;
        builder.mUrl = mUrl;
        builder.mSms = mSms;
        builder.mEmail = mEmail;
        builder.mSubject = mSubject;
        builder.mFile = mFile;
        builder.mTargetListeners = mTargetListeners;
        builder.mDefaultTargetManager = mDefaultTargetManager;
        return builder;
    }

    public void show(FragmentManager manager) {
        ShareFragment fragment = new ShareFragment();
        Bundle args = new Bundle();
        ShareUtil.setBuilder(args, this);
        fragment.setArguments(args);
        fragment.show(manager, null);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Sort the list of activities
     */
    public void sortActivities(Context context, Intent shareIntent, List<ResolveInfo> resolveInfos) {
        PackageTargetManager manager = mDefaultTargetManager;
        if (manager == null)
            manager = new DefaultPackageTargetManager();
        manager.sortActivities(context, shareIntent, resolveInfos);
    }

    /**
     * Get a list of activities that can be shared to
     */
    public List<ResolveInfo> queryIntentActivities(Context context, Intent shareIntent) {
        PackageTargetManager manager = mDefaultTargetManager;
        if (manager == null)
            manager = new DefaultPackageTargetManager();
        return manager.queryIntentActivities(context, shareIntent);
    }

    /**
     * Start the sharing activity
     *
     * @param info The activity info
     * @param requestCode The request code to receive back from the started activity
     * @param permissionRequestCode The permission request code in case we need access to external storage
     */
    public Intent startActivityForResult(Activity activity, ResolveInfo info, int requestCode, int permissionRequestCode) {
        String authority;
        String facebookId = null;
        try {
            ApplicationInfo applicationInfo = activity.getPackageManager()
                    .getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
            authority = applicationInfo.metaData.getString(SHARING_AUTHORITY);
            if (PackageTargetManager.FACEBOOK.equals(info.activityInfo.packageName))
                facebookId = applicationInfo.metaData.getString("com.facebook.sdk.ApplicationId");
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        if (TextUtils.isEmpty(authority))
            Log.w(TAG, MISSING_CONFIG);
        PackageTargetManager manager = mTargetListeners == null ? null : mTargetListeners.get(info.activityInfo.packageName);
        if (manager == null) {
            if (mDefaultTargetManager == null)
                manager = new DefaultPackageTargetManager();
            else
                manager = mDefaultTargetManager;
        }
        manager.initialize(mText, mUri, mUrl, mSms, mEmail, mSubject, mFile);
        manager.mFacebookId = facebookId;
        Intent intent = manager.onCreateIntent(activity, authority, info, permissionRequestCode);
        if (intent != null)
            manager.onShare(activity, intent, requestCode);
        return intent;
    }

    /**
     * Start the sharing activity
     *
     * @param info The activity info
     * @param requestCode The request code to receive back from the started activity
     * @param permissionRequestCode The permission request code in case we need access to external storage
     */
    public Intent startActivityForResult(Fragment fragment, ResolveInfo info, int requestCode, int permissionRequestCode) {
        String authority;
        String facebookId = null;
        try {
            ApplicationInfo applicationInfo = fragment.getActivity().getPackageManager()
                    .getApplicationInfo(fragment.getActivity().getPackageName(), PackageManager.GET_META_DATA);
            authority = applicationInfo.metaData.getString(SHARING_AUTHORITY);
            if (PackageTargetManager.FACEBOOK.equals(info.activityInfo.packageName))
                facebookId = applicationInfo.metaData.getString("com.facebook.sdk.ApplicationId");
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        if (TextUtils.isEmpty(authority))
            Log.w(TAG, MISSING_CONFIG);
        PackageTargetManager manager = mTargetListeners == null ? null : mTargetListeners.get(info.activityInfo.packageName);
        if (manager == null) {
            if (mDefaultTargetManager == null)
                manager = new DefaultPackageTargetManager();
            else
                manager = mDefaultTargetManager;
        }
        manager.initialize(mText, mUri, mUrl, mSms, mEmail, mSubject, mFile);
        manager.mFacebookId = facebookId;
        Intent intent = manager.onCreateIntent(fragment.getActivity(), authority, info, permissionRequestCode);
        if (intent != null)
            manager.onShare(fragment, intent, requestCode);
        return intent;
    }

    /**
     * Create a default share intent for the purposes of requesting activities that can
     * handle this intent.
     */
    public Intent createDefaultShareIntent(Context context) {
        String authority;
        try {
            ApplicationInfo applicationInfo = context.getApplicationContext().getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            authority = applicationInfo.metaData.getString(SHARING_AUTHORITY);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        if (TextUtils.isEmpty(authority))
            Log.w(TAG, MISSING_CONFIG);
        PackageTargetManager manager = mDefaultTargetManager == null ? new DefaultPackageTargetManager() : mDefaultTargetManager;
        manager.initialize(mText, mUri, mUrl, mSms, mEmail, mSubject, mFile);
        return manager.onCreateIntent(context, authority);
    }

    public static final class Builder implements Parcelable {

        String mText;
        String mEmail;
        String mSms;
        String mSubject;
        String mUrl;
        File mFile;
        Uri mUri;
        ArrayMap<String, PackageTargetManager> mTargetListeners;
        PackageTargetManager mDefaultTargetManager;

        Builder() {
        }

        Builder(Parcel in) {
            mText = in.readString();
            mUrl = in.readString();
            mEmail = in.readString();
            mSms = in.readString();
            mSubject = in.readString();
            mFile = (File) in.readSerializable();
            mUri = in.readParcelable(getClass().getClassLoader());

            int size = in.readInt();
            if (size > 0) {
                mTargetListeners = new ArrayMap<>(size);
                for (int i = 0; i < size; i++) {
                    String key = in.readString();
                    PackageTargetManager mgr = in.readParcelable(getClass().getClassLoader());
                    mTargetListeners.put(key, mgr);
                }
            }

            mDefaultTargetManager = in.readParcelable(getClass().getClassLoader());
        }

        public Builder text(String text) {
            mText = text;
            return this;
        }

        public Builder url(String url) {
            mUrl = url;
            return this;
        }

        public Builder defaultTargetListener(PackageTargetManager defaultTargetManager) {
            mDefaultTargetManager = defaultTargetManager;
            return this;
        }

        public Builder targetListeners(ArrayMap<String, PackageTargetManager> targetListeners) {
            for (Map.Entry<String, PackageTargetManager> entry : targetListeners.entrySet()) {
                targetListener(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder targetListener(String packageName, PackageTargetManager manager) {
            if (mTargetListeners == null)
                mTargetListeners = new ArrayMap<>();
            mTargetListeners.put(packageName, manager);
            return this;
        }

        public Builder uri(Uri uri) {
            mUri = uri;
            return this;
        }

        public Builder email(String email) {
            mEmail = email;
            return this;
        }

        public Builder sms(String sms) {
            mSms = sms;
            return this;
        }

        public Builder subject(String subject) {
            mSubject = subject;
            return this;
        }

        public Builder file(File file) {
            mFile = file;
            return this;
        }

        public ShareIntent build() {
            return new ShareIntent(this);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mText);
            dest.writeString(mUrl);
            dest.writeString(mEmail);
            dest.writeString(mSms);
            dest.writeString(mSubject);
            dest.writeSerializable(mFile);
            dest.writeParcelable(mUri, 0);

            int size = mTargetListeners == null ? 0 : mTargetListeners.size();

            dest.writeInt(size);

            if (size > 0) {
                for (Map.Entry<String, PackageTargetManager> entry : mTargetListeners.entrySet()) {
                    dest.writeString(entry.getKey());
                    dest.writeParcelable(entry.getValue(), 0);
                }
            }

            dest.writeParcelable(mDefaultTargetManager, 0);
        }

        public static final Creator<Builder> CREATOR = new Creator<Builder>() {
            public Builder createFromParcel(Parcel in) {
                return new Builder(in);
            }

            public Builder[] newArray(int size) {
                return new Builder[size];
            }
        };

    }

}
