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
package nuclei.ui.share;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nuclei.logs.Log;
import nuclei.logs.Logs;

/**
 * While most apps support sharing correctly, there are a few caveats when trying to support certain apps.
 * <br />
 * This aims to simplify sharing to some of the more popular apps and provide
 * a place to provide additional customization to what gets shared so that
 * you can focus on passing standard sets of information (text, url, images, etc) and
 * not worry about which app it is sent to.
 */
public abstract class PackageTargetManager implements Parcelable {

    private static final Log LOG = Logs.newLog(PackageTargetManager.class);

    public static final String FACEBOOK = "com.facebook.katana";
    public static final String TWITTER = "com.twitter.android";
    public static final String WECHAT = "com.tencent.mm";
    public static final String INSTAGRAM = "com.instagram.android";
    public static final String LINE = "jp.naver.line.android";
    public static final String TELEGRAM = "org.telegram.messenger";
    public static final String WHATSAP = "com.whatsapp";

    private static final int TWITTER_MAX_LEN = 140;
    private static final String DEFAULT_SHARE_WEIGHTS = "nuclei_share_weights";

    protected String mFacebookId;
    protected String mText;
    protected String mUrl;
    protected String mEmail;
    protected String mSms;
    protected String mSubject;
    protected File mFile;
    protected Uri mUri;

    SharedPreferences mWeights;

    public PackageTargetManager() {
    }

    protected PackageTargetManager(Parcel in) {
        mFacebookId = in.readString();
        mText = in.readString();
        mUrl = in.readString();
        mSms = in.readString();
        mEmail = in.readString();
        mSubject = in.readString();
        mFile = (File) in.readSerializable();
        mUri = in.readParcelable(getClass().getClassLoader());
    }

    /**
     * Initialize the target manager from the builder
     */
    protected void initialize(String text, Uri uri, String url, String sms, String email, String subject, File file) {
        mText = text;
        mUri = uri;
        mUrl = url;
        mSms = sms;
        mEmail = email;
        mSubject = subject;
        mFile = file;
    }

    /**
     * For targets like twitter, determine the max length of the text
     */
    protected int getMaxLen(String packageName) {
        if (TWITTER.equals(packageName))
            return TWITTER_MAX_LEN;
        return Integer.MAX_VALUE;
    }

    /**
     * Trim the text of targets like twitter, ensuring the URL
     * is included in the text.
     */
    protected String trim(String content, String url, int maxLen) {
        if (content != null)
            content = content.trim();
        if (url != null)
            url = url.trim();
        String text = content;
        if (content != null) {
            int len = content.length()
                    + (url == null ? 0 : url.length() + 2); // 2 = new line + ellipse
            if (len > maxLen) {
                len = maxLen;
                if (url != null)
                    len -= url.length() + 2;
                if (len > 0 && len < content.length()) {
                    text = text.substring(0, len) + "\u2026";
                }
            }
        } else
            text = "";
        if (url != null && text.length() > 0)
            text += "\n" + url;
        else if (url != null)
            text = url;
        return text;
    }

    /**
     * Get a list of activities that can be shared to
     */
    public void sortActivities(Context context, Intent shareIntent, List<ResolveInfo> resolveInfos) {
        if (mWeights == null)
            mWeights = context.getSharedPreferences(DEFAULT_SHARE_WEIGHTS, Context.MODE_PRIVATE);
        Collections.sort(resolveInfos, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo lhs, ResolveInfo rhs) {
                ComponentName name1 = new ComponentName(lhs.activityInfo.packageName, lhs.activityInfo.name);
                ComponentName name2 = new ComponentName(rhs.activityInfo.packageName, rhs.activityInfo.name);
                int weight1 = mWeights.getInt(name1.getClassName(), 0);
                int weight2 = mWeights.getInt(name2.getClassName(), 0);
                if (weight1 > weight2)
                    return -1;
                if (weight1 < weight2)
                    return 1;
                return 0;
            }
        });
    }

    /**
     * Get a list of activities that can be shared to
     */
    public List<ResolveInfo> queryIntentActivities(Context context, Intent shareIntent) {
        List<ResolveInfo> resolveInfos = context.getApplicationContext().getPackageManager()
                .queryIntentActivities(shareIntent, 0);
        for (int i = 0, len = resolveInfos.size(); i < len; i++) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            if ("com.android.fallback.Fallback".equals(resolveInfo.activityInfo.name)) {
                resolveInfos.remove(i);
                break;
            }
        }
        return resolveInfos;
    }

    /**
     * Create an intent without knowing which package has been chosen
     */
    public Intent onCreateIntent(Context context, String authority) {
        Intent intent = new Intent(TextUtils.isEmpty(mEmail) && TextUtils.isEmpty(mSms) ? Intent.ACTION_SEND : Intent.ACTION_SENDTO);
        onSetDefault(context, null, authority, intent, mText);
        return intent;
    }

    /**
     * Create an intent with package specific modifications
     *
     * @param authority The file authority provider to be used for sharing files
     * @param permissionRequestCode Some packages may require we put files on external storage,
     *                              this is the permission request code that will be used to request that permission
     */
    public Intent onCreateIntent(Activity activity, String authority, ResolveInfo info, int permissionRequestCode) {
        int maxLen = getMaxLen(info.activityInfo.packageName);
        String text = mText;

        if (maxLen != Integer.MAX_VALUE)
            text = trim(text, mUrl, maxLen);
        else if (mText != null && mUrl != null)
            text += '\n' + mUrl;

        Intent intent = new Intent(TextUtils.isEmpty(mEmail) && TextUtils.isEmpty(mSms) ? Intent.ACTION_SEND : Intent.ACTION_SENDTO);
        intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
        intent.setPackage(info.activityInfo.packageName);

        onSetDefault(activity, info.activityInfo.packageName, authority, intent, text);

        switch (info.activityInfo.packageName) {
            case FACEBOOK:
                intent = onFacebook(activity, intent);
                break;
            case WECHAT:
                intent = onExternalStorage(activity, info.activityInfo.packageName, authority, intent, permissionRequestCode, true);
                break;
            case LINE:
            case TELEGRAM:
            case WHATSAP:
                intent = onExternalStorage(activity, info.activityInfo.packageName, authority, intent, permissionRequestCode, false);
                break;
            case INSTAGRAM:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    intent = onExternalStorage(activity, info.activityInfo.packageName, authority, intent, permissionRequestCode, false);
                break;
        }
        return intent;
    }

    protected Intent onFacebook(Activity activity, Intent intent) {
        if (mFacebookId != null)
            intent.putExtra("com.facebook.platform.extra.APPLICATION_ID", mFacebookId);
        return intent;
    }

    /**
     * WeChat and on some android versions Instagram doesn't seem to handle file providers very well, so instead of those we move the
     * file to external storage and startActivityForResult with the actual file.
     */
    protected Intent onExternalStorage(Activity activity, String packageName, String authority, Intent intent, int permissionRequestCode, boolean stripText) {
        if (mFile != null) {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File file = ShareUtil.newShareFile(new File(Environment.getExternalStorageDirectory(), ".cyto"), mFile.getName());
                    try {
                        onCopyFile(mFile, file);
                        mFile.delete();
                        mFile = file;
                    } catch (IOException err) {
                        LOG.e("Error copying file for sharing", err);
                    }
                    onSetFileProvider(activity, packageName, authority, intent);
                } else {
                    File file = ShareUtil.newShareFile(new File(Environment.getExternalStorageDirectory(), ".cyto"), mFile.getName());
                    try {
                        onCopyFile(mFile, file);
                        mFile.delete();
                        mFile = file;
                    } catch (IOException err) {
                        LOG.e("Error copying file for sharing", err);
                    }
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFile));
                }
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, permissionRequestCode);
                return null;
            }
            if (stripText && intent.hasExtra(Intent.EXTRA_STREAM) && intent.hasExtra(Intent.EXTRA_TEXT))
                intent.removeExtra(Intent.EXTRA_TEXT);
        }
        return intent;
    }

    protected void onCopyFile(File inFile, File outFile) throws IOException {
        InputStream in = new FileInputStream(inFile);
        try {
            OutputStream out = new FileOutputStream(outFile);
            try {
                byte[] buf = new byte[8096];
                while (in.available() > 0) {
                    int r = in.read(buf);
                    out.write(buf, 0, r);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /**
     * Set default intent data
     */
    protected void onSetDefault(Context context, String packageName, String authority, Intent intent, String text) {
        intent.putExtra(Intent.EXTRA_TEXT, text);
        if (!TextUtils.isEmpty(mSubject))
            intent.putExtra(Intent.EXTRA_SUBJECT, mSubject);
        onSetFileProvider(context, packageName, authority, intent);
        if (!TextUtils.isEmpty(mEmail)) {
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mEmail});
            intent.setData(Uri.parse("mailto:"));
        } else if (!TextUtils.isEmpty(mSms)) {
            //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                intent.putExtra("sms_body", text);
                intent.putExtra("address", mSms);
                intent.putExtra(Intent.EXTRA_PHONE_NUMBER, new String[]{mSms});
            //}
            intent.setData(Uri.parse("smsto:" + mSms));
        }
    }

    protected void onSetFileProvider(Context context, String packageName, String authority, Intent intent) {
        if (mUri != null || mFile != null) {
            Uri uri = mUri;
            String type = "*/*";
            if (mFile != null) {
                uri = FileProvider.getUriForFile(context, authority, mFile);
                final int lastDot = mFile.getName().lastIndexOf('.');
                if (lastDot >= 0) {
                    String extension = mFile.getName().substring(lastDot + 1);
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    if (mimeType != null)
                        type = mimeType;
                }
            }
            intent.setDataAndType(intent.getData(), type);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            if (packageName != null) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        } else {
            intent.setType("text/plain");
        }
    }

    /**
     * Send the startActivityForResult intent to an activity
     *
     * @param requestCode The request code to listen to in the onActivityResult
     */
    public void onShare(Activity activity, Intent intent, int requestCode) {
        if (mWeights == null)
            mWeights = activity.getSharedPreferences(DEFAULT_SHARE_WEIGHTS, Context.MODE_PRIVATE);
        int weight = mWeights.getInt(intent.getComponent().getClassName(), 0) + 1;
        mWeights.edit().putInt(intent.getComponent().getClassName(), weight).apply();
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Send the startActivityForResult intent to an activity
     *
     * @param requestCode The request code to listen to in the onActivityResult
     */
    public void onShare(Fragment fragment, Intent intent, int requestCode) {
        if (mWeights == null)
            mWeights = fragment.getActivity().getSharedPreferences(DEFAULT_SHARE_WEIGHTS, Context.MODE_PRIVATE);
        int weight = mWeights.getInt(intent.getComponent().getClassName(), 0) + 1;
        mWeights.edit().putInt(intent.getComponent().getClassName(), weight).apply();
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFacebookId);
        dest.writeString(mText);
        dest.writeString(mUrl);
        dest.writeString(mSms);
        dest.writeString(mEmail);
        dest.writeString(mSubject);
        dest.writeSerializable(mFile);
        dest.writeParcelable(mUri, 0);
    }

}
