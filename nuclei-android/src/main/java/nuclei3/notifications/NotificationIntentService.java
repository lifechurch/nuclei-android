package nuclei3.notifications;

import android.app.IntentService;
import android.content.Intent;

import nuclei3.notifications.model.NotificationMessage;

public class NotificationIntentService extends IntentService {

    public static final String EXTRA_CLEAR_ID = "clear_id";
    public static final String EXTRA_CLEAR_ALL = "clear_all";
    public static final String EXTRA_GROUP_KEY = "group_key";

    public NotificationIntentService() {
        super("NotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;
        String groupKey = intent.hasExtra(EXTRA_GROUP_KEY)
                ? intent.getStringExtra(EXTRA_GROUP_KEY)
                : null;
        if (intent.hasExtra(EXTRA_CLEAR_ALL)) {
            NotificationManager.getInstance().removeMessages(groupKey);
        } else {
            long clientId = intent.getLongExtra(EXTRA_CLEAR_ID, -1);
            NotificationMessage message = NotificationManager.getInstance().getMessage(clientId);
            if (message != null)
                NotificationManager.getInstance().removeMessage(message);
        }
    }

}
