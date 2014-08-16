package com.znck.android.nosql;

import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.znck.android.nosql.util.JSON;
import com.znck.android.nosql.util.JSONable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class Document implements JSONable, Comparable<Document> {
    private final static String TAG = Document.class.getName();
    public final static String CREATED_AT = "odml_created_at";
    public final static String SYNCED_AT = "odml_synced_at";
    public final static String UPDATED_AT = "odml_updated_at";
    public final static String REMOTE_ID = "odml_remote_id";
    public final static String USER_ID = "odml_user_id";
    public final static String DOC_TYPE = "odml_doc_type";
    public final static String DOC_TEXT = "odml_doc_text";
    public final static String TIMESTAMP_FORMAT = "E, d MMM yyyy HH:mm:ss Z";
    private static DatabaseHelper databaseHelper;
    protected boolean modified = false;

    public com.couchbase.lite.Document getDocument() {
        return document;
    }

    private com.couchbase.lite.Document document;
    private Map<String, Object> properties = new HashMap<String, Object>();
    private SimpleDateFormat dateFormat;

    public Document() throws IllegalAccessException {
        build();
    }

    public Document(com.couchbase.lite.Document document) {
        this.document = document;
        try {
            build();
        } catch (IllegalAccessException e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    public Document(Map<String, Object> properties){
        try {
            build();
        } catch (IllegalAccessException e) {
            Log.d(TAG, e.getMessage(), e);
        }
        this.properties.putAll(properties);
    }

    public static boolean init(DatabaseHelper db) {
        if (null == databaseHelper) {
            databaseHelper = db;
            return true;
        }
        return false;
    }

    protected void build() throws IllegalAccessException {
        dateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);
        if (null == databaseHelper)
            throw new IllegalAccessException("DatabaseHelper should be initialized before creating NoSQL store objects");

        properties = document != null ? document.getProperties() : new HashMap<String, Object>();

        if (null == this.document) this.document = databaseHelper.getDatabase().createDocument();

        set(REMOTE_ID, 0, false);
        set(CREATED_AT, dateFormat.format(new Date()), false);
        set(UPDATED_AT, dateFormat.format(new Date()), false);
        set(SYNCED_AT, dateFormat.format(new Date(0)), false);
        set(USER_ID, DatabaseHelper.getUser());
        set(DOC_TYPE, "__none");
    }

    public void set(String key, Object value) {
        set(key, value, true);
    }

    public boolean set(String key, Object value, boolean overwrite) {
        if (!overwrite && properties.containsKey(key))
            return false;
        modified = true;
        properties.put(key, value);
        return true;
    }

    public Object get(String key) {
        return properties.get(key);
    }

    public Date created() {
        try {
            return dateFormat.parse((String) get(CREATED_AT));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    public Date updated() {
        try {
            return dateFormat.parse((String) get(UPDATED_AT));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    public Date synced() {
        try {
            return dateFormat.parse((String) get(SYNCED_AT));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    public void touch() {
        set("updated_at", new Date());
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String commit() {
        try {
            document.putProperties(properties);
            return document.getId();
        } catch (CouchbaseLiteException e) {
            Log.d(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String toJSON() {
        return JSON.export(properties);
    }

    public void set(Document remote, Boolean overwrite) {
        for (String key : remote.getProperties().keySet()) {
            set(key, remote.get(key), overwrite);
        }
    }

    public Map<String, String> syncableData() {
        Map<String, Object> properties = getProperties();
        Map<String, String> data = new HashMap<String, String>();
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            data.put(property.getKey(), (String) property.getValue());
        }
        return data;
    }

    public void setRemoteId(String remoteId, Date syncedAt) {
        set(REMOTE_ID, remoteId, true);
        set(SYNCED_AT, dateFormat.format(syncedAt));
        commit();
    }

    @Override
    public int compareTo(Document document) {
        return this.document.getId().compareTo(document.document.getId());
    }

    public void updateSyncTimestamp(Date date) {
        properties.put(SYNCED_AT, dateFormat.format(date));
        commit();
    }
}
