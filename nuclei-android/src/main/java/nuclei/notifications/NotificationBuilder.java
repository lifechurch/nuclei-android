package nuclei.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import java.util.List;

import nuclei.notifications.model.NotificationMessage;

public abstract class NotificationBuilder {

    protected int getDeleteIntentRequestId() {
        return 1;
    }

    public Notification buildSummary(Context context, NotificationManager manager, String group, List<NotificationMessage> messages) {
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();

        Intent intent = new Intent(context, NotificationIntentService.class);
        manager.setCancelAll(intent, group);
        intent.setData(Uri.parse("nuclei://notifications?_g=" + group));
        PendingIntent pendingIntent = PendingIntent.getService(context, getDeleteIntentRequestId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Builder builder = new Builder(context)
                .setSmallIcon(manager.getDefaultSmallIcon())
                .setLargeIcon(manager.getDefaultLargeIcon())
                .setAutoCancel(true)
                .setGroup(group)
                .setGroupSummary(true);

        onBuildSummary(context, manager, builder, extender, group, messages);

        builder.setDeleteIntent(pendingIntent);

        extender.extend(builder);

        return builder.build();
    }

    protected abstract void onBuildSummary(Context context, NotificationManager manager, Builder builder, NotificationCompat.WearableExtender extender, String group, List<NotificationMessage> messages);

    public Notification buildNotification(Context context, NotificationManager manager, NotificationMessage message, List<NotificationMessage> messages) {
        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();

        Intent intent = new Intent(context, NotificationIntentService.class);
        manager.setCancelMessage(intent, message);
        intent.setData(Uri.parse("nuclei://notifications?_id=" + message._id + "&_g=" + message.groupKey));
        PendingIntent pendingIntent = PendingIntent.getService(context, getDeleteIntentRequestId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Builder builder = new Builder(context)
                .setSmallIcon(manager.getDefaultSmallIcon())
                .setLargeIcon(manager.getDefaultLargeIcon())
                .setOngoing(false)
                .setAutoCancel(true);

        if (messages.size() > 1) {
            builder.setGroup(message.groupKey).setGroupSummary(false);
        }

        onBuildNotification(context, manager, builder, extender, message);

        builder.setDeleteIntent(pendingIntent);

        extender.extend(builder);

        return builder.build();
    }

    protected abstract void onBuildNotification(Context context, NotificationManager manager, Builder builder, NotificationCompat.WearableExtender extender, NotificationMessage message);

}
