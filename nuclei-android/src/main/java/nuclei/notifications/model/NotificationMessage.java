package nuclei.notifications.model;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.util.JsonReader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import nuclei.notifications.persistence.Persistence;
import nuclei.notifications.providers.Schemas;
import nuclei.persistence.ModelObject;
import nuclei.persistence.PersistenceList;
import nuclei.persistence.Query;

public class NotificationMessage implements ModelObject {

    public static final Query.MapperEntity<NotificationMessage> INSERT = Schemas.NotificationMessage.INSERT;
    public static final Query<NotificationMessage> SELECT_BYGROUP = Schemas.NotificationMessage.SELECT_BYGROUP;
    public static final Query<NotificationMessage> SELECT_ALL = Schemas.NotificationMessage.SELECT_ALL;
    public static final Query<NotificationMessage> SELECT_BYCLIENTID = Schemas.NotificationMessage.SELECT_BYCLIENTID;
    public static final Query<NotificationMessage> UPDATE_BYCLIENTID = Schemas.NotificationMessage.UPDATE_BYCLIENTID;
    public static final Query<NotificationMessage> DELETE_ALL = Schemas.NotificationMessage.DELETE_ALL;
    public static final Query<NotificationMessage> DELETE_BYCLIENTID = Schemas.NotificationMessage.DELETE_BYCLIENTID;

    public String url;
    public String groupKey;
    public long _id;
    public int sortOrder;
    public int id;
    public String tag;

    private List<NotificationData> _data;
    private Map<String, NotificationData> _dataMap;

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

    public List<NotificationData> getData() {
        if (_data != null)
            return _data;
        PersistenceList<NotificationData> data
                = NotificationData.SELECT_BYMESSAGEID.newSelect().executeList(Query.args().arg(_id));
        try {
            _data = new ArrayList<>(data);
        } finally {
            data.close();
        }
        return _data;
    }

    public void setData(List<NotificationData> data) {
        try {
            _data = data;
            _dataMap = null;
            if (_id > 0) {
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                ops.add(NotificationData.DELETE_BYMESSAGEID.toDeleteOperation(Query.args().arg(_id)));
                for (int i = 0, len = data.size(); i < len; i++) {
                    NotificationData d = data.get(i);
                    d.messageClientId = _id;
                    ops.add(NotificationData.INSERT.toInsertOperation(d));
                }
                Persistence.applyBatch(ops);
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

}