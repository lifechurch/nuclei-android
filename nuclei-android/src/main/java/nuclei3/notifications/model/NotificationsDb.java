package nuclei3.notifications.model;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 1, entities = {NotificationMessage.class, NotificationData.class}, exportSchema = false)
public abstract class NotificationsDb extends RoomDatabase {

    public abstract NotificationsDao notificationsDao();

}
