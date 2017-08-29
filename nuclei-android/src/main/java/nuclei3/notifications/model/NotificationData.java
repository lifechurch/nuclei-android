package nuclei3.notifications.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

@Entity(indices = @Index("messageClientId"),
        foreignKeys = @ForeignKey(entity = NotificationMessage.class, parentColumns = "_id", childColumns = "messageClientId", onDelete = ForeignKey.CASCADE))
public class NotificationData {

    @PrimaryKey(autoGenerate = true)
    public long _id;

    public long messageClientId;
    public String dataKey;
    public Long valLong;
    public Boolean valBoolean;
    public Integer valInt;
    public Double valDouble;
    public String valString;

}
