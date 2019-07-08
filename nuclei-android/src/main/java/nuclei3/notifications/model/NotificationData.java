package nuclei3.notifications.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(indices = @Index("messageClientId"),
        foreignKeys = @ForeignKey(
                entity = NotificationMessage.class,
                parentColumns = "_id",
                childColumns = "messageClientId",
                onDelete = ForeignKey.CASCADE,
                deferred = true))
public class NotificationData {

    @PrimaryKey(autoGenerate = true)
    public Long _id;

    public long messageClientId;
    public String dataKey;
    public Long valLong;
    public Boolean valBoolean;
    public Integer valInt;
    public Double valDouble;
    public String valString;

}
