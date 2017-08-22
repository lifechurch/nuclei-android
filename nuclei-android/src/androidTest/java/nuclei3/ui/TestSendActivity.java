package nuclei3.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import nuclei3.ui.share.ShareUtil;

public class TestSendActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent();
        ShareUtil.setShareIntent(intent, getIntent());
        setResult(RESULT_OK, intent);
        finish();
    }
}
