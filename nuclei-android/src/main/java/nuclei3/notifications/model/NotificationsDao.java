package nuclei3.notifications.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public abstract class NotificationsDao {

    @Query("select * from NotificationMessage")
    public abstract List<NotificationMessage> getMessages();

    @Query("select * from NotificationMessage where _id = :id")
    public abstract NotificationMessage getMessage(long id);

    @Query("select * from NotificationMessage where groupKey = :groupKey")
    public abstract List<NotificationMessage> getMessages(String groupKey);

    @Query("select count(*) from NotificationMessage where groupKey = :groupKey")
    public abstract int getCount(String groupKey);

    @Query("delete from NotificationMessage where groupKey = :groupKey")
    public abstract void deleteMessages(String groupKey);

    @Insert
    public abstract long addMessage(NotificationMessage message);

    @Delete
    public abstract void deleteMessage(NotificationMessage message);

    @Update
    public abstract void updateMessage(NotificationMessage message);

    @Query("delete from NotificationData where messageClientId = :id")
    public abstract void deleteData(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addData(List<NotificationData> data);

    @Query("select * from NotificationData where messageClientId = :id")
    public abstract List<NotificationData> getData(long id);

}
