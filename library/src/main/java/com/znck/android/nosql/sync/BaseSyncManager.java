package com.znck.android.nosql.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.couchbase.lite.QueryEnumerator;
import com.znck.android.nosql.DatabaseHelper;
import com.znck.android.nosql.Document;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

abstract class BaseSyncManager {
    public static final String LAST_SYNC_TIMESTAMP = "last_sync_timestamp";
    private static final String TAG = BaseSyncManager.class.getName();
    protected final SimpleDateFormat dateFormat;
    protected SharedPreferences preferences;
    protected Context context;
    protected Date lastSyncTimestamp;
    protected MergeScheme mergeScheme;

    public BaseSyncManager(Context context, MergeScheme mergeScheme) {
        this.context = context;
        this.mergeScheme = mergeScheme;
        preferences = context.getSharedPreferences("com.zunk.nosql.prefs", Context.MODE_PRIVATE);
        dateFormat = new SimpleDateFormat(Document.TIMESTAMP_FORMAT, Locale.US);
    }

    public void startSync() {
        Date start = new Date();
        startUpload(start);
        // TODO store remote
        startDownload(start);
        // TODO notify dataset changed
        Date end = new Date();
    }

    protected final void addUpdateSyncTimestamp() {
        addUpdateSyncTimestamp(null);
    }

    protected final void addUpdateSyncTimestamp(Date date) {
        if (null == date) date = new Date();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LAST_SYNC_TIMESTAMP, dateFormat.format(date));
        editor.commit();
    }

    public final Date getLastSyncTimestamp() {
        Date date = null;
        try {
            date = dateFormat.parse(preferences.getString(LAST_SYNC_TIMESTAMP, dateFormat.format(new Date())));
        } catch (ParseException e) {
            Log.d(TAG, "Date parse exception", e);
            date = new Date(0);
        }
        return lastSyncTimestamp = date;
    }

    protected final QueryEnumerator changedLocally() {
        return changedLocallyAfter(getLastSyncTimestamp());
    }

    protected final QueryEnumerator changedLocallyAfter(Date date) {
        if (null == date) return null;
        return DatabaseHelper.getInstance().between(Document.UPDATED_AT, dateFormat.format(date), null, true);
    }

    protected final Document detectConflict(Document remote) {
        QueryEnumerator docs = DatabaseHelper.getInstance().find(Document.REMOTE_ID, "" + remote.get(Document.REMOTE_ID), true);
        int count = docs.getCount();
        if (1 < count) Log.d(TAG, "Injection conflict: Keeping only first entry for sync");
        if (0 == count) return null;
        Document local = new Document(docs.next().getDocumentProperties());
        switch (mergeScheme) {
            case MERGE_OVER_WRITE_LOCAL:
                local.set(remote, true);
                return local;
            case MERGE_OVER_WRITE_REMOTE:
                remote.set(local, true);
                return remote;
            case MERGE_CREATE_NEW:
                local.set(Document.REMOTE_ID, 0, true);
                local.commit();
                return remote;
        }
        return null;
    }

    protected final Document finalize(Document doc) {
        doc.commit();
        return doc;
    }

    protected final ArrayList<Document> uploadList(Date lastSync) {
        ArrayList<Document> docs = new ArrayList<Document>();
        QueryEnumerator query = DatabaseHelper.getInstance().between(Document.SYNCED_AT, dateFormat.format(lastSync), null, true);
        while (query.hasNext()) {
            Document doc = new Document(query.next().getDocumentProperties());
            docs.add(doc);
        }
        query = changedLocally();
        while (query.hasNext()) {
            Document doc = new Document(query.next().getDocumentProperties());
            if (!docs.contains(doc))
                docs.add(doc);
        }
        return docs;
    }

    public enum MergeScheme {
        MERGE_OVER_WRITE_LOCAL,
        MERGE_OVER_WRITE_REMOTE,
        MERGE_CREATE_NEW
    }

    protected abstract void startUpload(Date start);

    protected abstract void startDownload(Date start);

    protected abstract JSONObject download(String url, String meta, boolean is);

    protected abstract JSONObject upload(Document doc);

    protected abstract ArrayList<String> downloadList(Date lastSync);
}