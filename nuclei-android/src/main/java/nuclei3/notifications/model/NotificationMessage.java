package nuclei3.notifications.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nuclei3.notifications.NotificationManager;

@Entity(indices = @Index("groupKey"))
public class NotificationMessage {

    @PrimaryKey(autoGenerate = true)
    public Long _id;

    public String url;
    public String groupKey;
    public int sortOrder;
    public int id;
    public String tag;

    @Ignore
    private List<NotificationData> _data;

    @Ignore
    private Map<String, NotificationData> _dataMap;

    @Ignore
    @WorkerThread
    public NotificationData getData(String key) {
        if (_dataMap != null)
            return _dataMap.get(key);
        _dataMap = new ArrayMap<>();
        List<NotificationData> data = getData();
        for (int i = 0, len = data.size(); i < len; i++) {
            NotificationData d = data.get(i);
            _dataMap.put(d.dataKey, d);
        }
        return _dataMap.get(key);
    }

    @Ignore
    @WorkerThread
    public List<NotificationData> getData() {
        if (_data != null)
            return _data;
        List<NotificationData> data = NotificationManager.getInstance().getData(this);
        _data = new ArrayList<>(data);
        return _data;
    }

    @Ignore
    @WorkerThread
    public void setData(List<NotificationData> data) {
        try {
            _data = data;
            _dataMap = null;
            if (_id != null && _id > 0) {
                NotificationManager.getInstance().setData(this, data);
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

}

