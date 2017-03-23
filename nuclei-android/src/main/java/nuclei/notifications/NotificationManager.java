package nuclei.notifications;

import android.app.Notification;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nuclei.notifications.model.NotificationData;
import nuclei.notifications.model.NotificationMessage;
import nuclei.notifications.persistence.Persistence;
import nuclei.persistence.PersistenceList;
import nuclei.persistence.Query;
import nuclei.task.TaskScheduler;
import nuclei.task.Tasks;

public abstract class NotificationManager {

    private static Context CONTEXT;
    private static NotificationManager INSTANCE;

    static final String AUTO_DISMISS_TAG = "_nuclei_auto_dismiss_tag_";
    static final String AUTO_DISMISS_ID = "_nuclei_auto_dismiss_id_";

    public static void initialize(Context context, NotificationManager instance) {
        CONTEXT = context.getApplicationContext();
        INSTANCE = instance;
    }

    public static NotificationManager getInstance() {
        return INSTANCE;
    }

    public abstract int getDefaultSmallIcon();

    public abstract Bitmap getDefaultLargeIcon();

    public abstract int getTaskId();

    public abstract NotificationBuilder getBuilder();

    protected void onPrepareTaskScheduler(TaskScheduler.Builder builder) {
        builder.setWindowDelay(1, 60);
    }

    protected abstract boolean prepareNotificationMessage(NotificationMessage message, List<NotificationData> data);

    public NotificationMessage getMessage(Intent intent) {
        return getMessage(intent.getExtras());
    }

    public NotificationMessage getMessage(Bundle bundle) {
        List<NotificationData> notificationData = getData(bundle);
        NotificationMessage message = new NotificationMessage();
        message.setData(notificationData);
        if (prepareNotificationMessage(message, notificationData))
            return message;
        return null;
    }

    public NotificationMessage getMessage(Map<String, String> data) {
        List<NotificationData> notificationData = getData(data);
        NotificationMessage message = new NotificationMessage();
        message.setData(notificationData);
        if (prepareNotificationMessage(message, notificationData))
            return message;
        return null;
    }

    public List<NotificationData> getData(Intent intent) {
        return getData(intent.getExtras());
    }

    public List<NotificationData> getData(Bundle bundle) {
        List<NotificationData> d = new ArrayList<>();
        for (String key : bundle.keySet()) {
            NotificationData nd = new NotificationData();
            nd.dataKey = key;
            Object v = bundle.get(key);
            if (v instanceof Long)
                nd.valLong = (Long) v;
            else if (v instanceof Boolean)
                nd.valBoolean = (Boolean) v;
            else if (v instanceof Integer)
                nd.valInt = (Integer) v;
            else if (v instanceof Double)
                nd.valDouble = (Double) v;
            else if (v instanceof String)
                nd.valString = (String) v;
            else
                throw new IllegalArgumentException("Invalid type");
            d.add(nd);
        }
        return d;
    }

    public List<NotificationData> getData(Map<String, String> data) {
        List<NotificationData> d = new ArrayList<>();
        for (Map.Entry<String, String> e : data.entrySet()) {
            NotificationData nd = new NotificationData();
            nd.dataKey = e.getKey();
            nd.valString = e.getValue();
            d.add(nd);
        }
        return d;
    }

    public void addMessage(Intent intent) {
        NotificationMessage message = getMessage(intent);
        addMessage(message);
    }

    public void addMessage(Bundle bundle) {
        NotificationMessage message = getMessage(bundle);
        addMessage(message);
    }

    public void addMessage(Map<String, String> data) {
        NotificationMessage message = getMessage(data);
        addMessage(message);
    }

    public void addMessage(NotificationMessage message) {
        if (message == null)
            return;
        if (message.tag == null)
            message.tag = "default";
        if (message.groupKey == null)
            message.groupKey = "default";
        message.sortOrder = NotificationMessage.SELECT_BYGROUP.newSelect().count(Query.args().arg(message.groupKey)) + 1;
        Uri uri = NotificationMessage.INSERT.insert(message);
        message._id = Long.parseLong(uri.getLastPathSegment());
        message.setData(message.getData());
        rebuild();
    }

    public void removeMessages(String group) {
        List<NotificationMessage> messages = getMessages(group);
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(CONTEXT);
        for (int i = 0, len = messages.size(); i < len; i++) {
            NotificationMessage message = messages.get(i);
            ops.add(NotificationMessage.DELETE_BYCLIENTID.toDeleteOperation(Query.args().arg(message._id)));
            ops.add(NotificationData.DELETE_BYMESSAGEID.toDeleteOperation(Query.args().arg(message._id)));
            managerCompat.cancel(getTag(message), message.id);
        }
        try {
            Persistence.applyBatch(ops);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public void removeMessage(NotificationMessage message) {
        NotificationMessage.DELETE_BYCLIENTID.newUpdate().delete(Query.args().arg(message._id));
        NotificationData.DELETE_BYMESSAGEID.newUpdate().delete(Query.args().arg(message._id));
        NotificationManagerCompat.from(CONTEXT).cancel(getTag(message), message.id);

        if (getMessageCount(message.groupKey) == 1) {
            show();
        }
    }

    public NotificationMessage getMessage(long clientId) {
        return NotificationMessage.SELECT_BYCLIENTID.newSelect().executeOne(Query.args().arg(clientId));
    }

    public int getMessageCount(String groupKey) {
        return NotificationMessage.SELECT_BYGROUP.newSelect().count(Query.args().arg(groupKey));
    }

    public List<NotificationMessage> getMessages(String group) {
        PersistenceList<NotificationMessage> messages
                = NotificationMessage.SELECT_BYGROUP.newSelect().executeList(Query.args().arg(group));
        try {
            return new ArrayList<>(messages);
        } finally {
            messages.close();
        }
    }

    public void updateMessage(NotificationMessage message, boolean rebuild) {
        NotificationMessage.UPDATE_BYCLIENTID.newUpdate().update(Query.args().arg(message._id), message);
        if (rebuild)
            rebuild();
    }

    protected ArrayMap<String, List<NotificationMessage>> getMessagesByGroup() {
        ArrayMap<String, List<NotificationMessage>> messagesByGroup = new ArrayMap<>();
        PersistenceList<NotificationMessage> messages = NotificationMessage.SELECT_ALL.newSelect().executeList();
        try {
            for (int i = 0, size = messages.size(); i < size; i++) {
                NotificationMessage message = messages.get(i);
                List<NotificationMessage> m = messagesByGroup.get(message.groupKey);
                if (m == null) {
                    m = new ArrayList<>();
                    messagesByGroup.put(message.groupKey, m);
                }
                m.add(message);
            }
        } finally {
            messages.close();
        }
        return messagesByGroup;
    }

    private void rebuild() {
        if (TaskScheduler.supportsScheduling()) {
            TaskScheduler.Builder builder = TaskScheduler.newBuilder(new NotificationTask(), TaskScheduler.TASK_ONE_OFF);
            onPrepareTaskScheduler(builder);
            builder.setUpdateCurrent(true)
                    .setPersisted(true)
                    .build()
                    .schedule(CONTEXT);
        } else {
            Tasks.executeNow(new NotificationTask());
        }
    }

    public void show() {
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(CONTEXT);
        NotificationBuilder builder = getBuilder();
        ArrayMap<String, List<NotificationMessage>> messagesByGroup = getMessagesByGroup();
        for (int i = 0, size = messagesByGroup.size(); i < size; i++) {
            List<NotificationMessage> messages = messagesByGroup.valueAt(i);
            if (messages.size() > 1) {
                String group = messagesByGroup.keyAt(i);
                Notification notification = builder.buildSummary(CONTEXT, this, group, messages);
                onShow(managerCompat, group, null, notification);
                for (int m = 0, mSize = messages.size(); m < mSize; m++) {
                    NotificationMessage message = messages.get(m);
                    notification = builder.buildNotification(CONTEXT, this, message);
                    onShow(managerCompat, group, message, notification);
                }
            } else {
                NotificationMessage message = messages.get(0);
                Notification notification = builder.buildNotification(CONTEXT, this, message);
                onShow(managerCompat, message.groupKey, message, notification);
            }
        }
    }

    protected void onShow(NotificationManagerCompat managerCompat, String group, NotificationMessage message, Notification notification) {
        if (message == null)
            managerCompat.notify(getTag(group), getId(group), notification);
        else {
            managerCompat.notify(getTag(message), message.id, notification);
        }
    }

    protected int getId(String group) {
        return 1;
    }

    protected String getTag(String group) {
        return group;
    }

    protected String getTag(NotificationMessage message) {
        String tag = message.tag;
        if (message.id == 0)
            tag += "_" + message._id;
        return tag;
    }

    public void setAutoDismiss(Bundle args, String group, NotificationMessage message) {
        if (message == null) {
            args.putString(AUTO_DISMISS_TAG, getTag(group));
            args.putInt(AUTO_DISMISS_ID, getId(group));
        } else {
            args.putString(AUTO_DISMISS_TAG, getTag(message));
            args.putInt(AUTO_DISMISS_ID, message.id);
        }
    }

    public void autoDismiss(Intent intent) {
        if (intent != null && intent.hasExtra(AUTO_DISMISS_TAG) && intent.hasExtra(AUTO_DISMISS_ID)) {
            final String tag = intent.getStringExtra(AUTO_DISMISS_TAG);
            final int id = intent.getIntExtra(AUTO_DISMISS_ID, 0);
            NotificationManagerCompat.from(CONTEXT).cancel(tag, id);
        }
    }

}
