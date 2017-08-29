package nuclei3.notifications;

import android.content.Context;

import nuclei3.task.Task;

public class NotificationTask extends Task<Void> {

    @Override
    public int getTaskId() {
        return NotificationManager.getInstance().getTaskId();
    }

    @Override
    public String getId() {
        return "notification-task";
    }

    @Override
    public void run(Context context) throws Exception {
        NotificationManager.getInstance().show();
        onComplete();
    }

}
