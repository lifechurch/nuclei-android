package nuclei3.notifications.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(indices = @Index("messageClientId"),
        foreignKeys = @ForeignKey(
                entity = NotificationMessage.class,
                parentColumns = "_id",
                childColumns = "messageClientId",
                onDelete = ForeignKey.CASCADE,
                deferred = true))
public class NotificationData {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    public long _id;

    public long messageClientId;
    public String dataKey;
    public Long valLong;
    public Boolean valBoolean;
    public Integer valInt;
    public Double valDouble;
    public String valString;

}
