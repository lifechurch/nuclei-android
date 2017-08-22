package nuclei3.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;

import junit.framework.Assert;

import io.nuclei.test.R;
import nuclei3.ui.share.ShareIntent;
import nuclei3.ui.share.ShareUtil;

public class TestShareActivity extends Activity {

    static final int REQUEST_CODE = 200;

    public String packageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nuclei3_activity_share_test);

        findViewById(R.id.click_me)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        share();
                    }
                });
    }

    void share() {
        ShareIntent shareIntent = ShareIntent.newBuilder()
                .text("Hello World")
                .build();
        Intent intent = shareIntent.createDefaultShareIntent(this);
        for (ResolveInfo info : shareIntent.queryIntentActivities(this, intent)) {
            if (info.activityInfo.name.equals("nuclei.ui.TestSendActivity")) {
                shareIntent.startActivityForResult(this, info, REQUEST_CODE, 1);
                return;
            }
        }
        Assert.fail();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            Intent intent = ShareUtil.getShareIntent(data);
            Assert.assertNotNull(intent);
            Assert.assertNotNull(intent.getPackage());
            Assert.assertEquals("Hello World", intent.getStringExtra(Intent.EXTRA_TEXT));
            packageName = intent.getPackage();
        } else {
            Assert.fail();
        }
    }
}
