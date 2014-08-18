package com.znck.android.nosql;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.couchbase.lite.Manager.DEFAULT_OPTIONS;

/**
 * A helper class for NoSQL database
 * <p>
 * This class provides generic functions to store data objects in Couchbase lite database
 * </p>
 *
 * @author Rahul Kadyan, <mail@rahulkadyan.com>
 * @version 1.0.0
 */
public class DatabaseHelper {
    /**
     * Debug tag
     */
    public static final String TAG = DatabaseHelper.class.getName();

    public static String getUser() {
        return user;
    }

    public static void setUser(String user) {
        if (null != user)
            DatabaseHelper.user = user;
    }

    private static String user = "__default";
    /**
     * property name for kind of document storing
     */
    private static final String DATABASE_NAME = "database";
    private static DatabaseHelper instance;
    private static Manager manager = null;
    private static Database database = null;

    /**
     * Instantiates a new Database helper.
     * <p>
     * Creates a Database manager and opens a connection to default database ${DatabaseHelper::DATABASE_NAME}
     * </p>
     *
     * @param context Android application context
     */
    private DatabaseHelper(Context context) {
        if (null == manager)
            try {
                manager = new Manager(new AndroidContext(context), DEFAULT_OPTIONS);
            } catch (IOException e) {
                Log.e(TAG, "Cannot create NoSQL manager object", e);
                return;
            }

        if (null == database)
            try {
                database = manager.getDatabase(DATABASE_NAME);
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "Cannot get database", e);
            }
    }

    public static DatabaseHelper init(Context context) {
        if (null == instance) instance = new DatabaseHelper(context);
        Document.init(instance);
        return instance;
    }

    public static DatabaseHelper getInstance() {
        return instance;
    }

    /**
     * Close all database connections
     */
    public static void release() {
        if (null != database) database.close();
        if (null != manager) manager.close();
    }

    /**
     * Returns Database instance
     *
     * @return Database database
     */
    public Database getDatabase() {
        return database;
    }

    public Query createQuery() {
        return createQuery(false);
    }

    public Query createQuery(boolean override) {
        return createQuery("_id", override);
    }

    public Query createQuery(final String key) {
        return createQuery(key, false);
    }

    public Query createQuery(final String key, final boolean override) {
        View view = database.getView("list-by-" + key);
        if (view.getMap() == null) {
            Mapper mapper = new Mapper() {
                @Override
                public void map(Map<String, Object> properties, Emitter emitter) {
                    String u = (String) properties.get(Document.USER_ID);
//                    Log.d("SYNC", "from: " + u);
                    if (override || user.equals(u) || u.equals("__default")) {
//                        Log.d("SYNC", "emitting: " + key + "=" + properties.get(key));
                        emitter.emit(properties.get(key), properties);
                    }
                }
            };
            view.setMap(mapper, null);
        }
        Query query = view.createQuery();
        query.setDescending(true);
        return query;
    }

    public Query createQuery(final Set<String> where) {
        return createQuery(where, false);
    }

    public Query createQuery(final Set<String> where, final boolean override) {
        View view = database.getView("list-by-" + where.toString());
        if (view.getMap() == null) {
            Mapper mapper = new Mapper() {
                @Override
                public void map(Map<String, Object> properties, Emitter emitter) {
                    String u = (String) properties.get(Document.USER_ID);
                    if (override || user.equals(u) || u.equals("__default")) {
                        List<Object> compoundKey = new ArrayList<Object>();

                        for (String field : where) compoundKey.add(properties.get(field));
                        emitter.emit(compoundKey, properties);
                    }
                }
            };
            view.setMap(mapper, null);
        }
        Query query = view.createQuery();
        query.setDescending(true);
        return query;
    }

    public QueryEnumerator find(String key, String value) {
        return find(key, value, false);
    }

    public QueryEnumerator find(String key, String value, boolean override) {
//        Log.d("SYNC", "find: " + key + "=" + value);
        Query query = createQuery(key, override);

        List<Object> keys = new ArrayList<Object>();
        keys.add(value);

        query.setKeys(keys);

        try {
            return query.run();
        } catch (CouchbaseLiteException e) {
            Log.i(TAG, "Cannot run query", e);
        }
        return null;
    }

    public QueryEnumerator find(Map<String, String> where) {
        return find(where, false);
    }

    public QueryEnumerator find(Map<String, String> where, boolean override) {
        Query query = createQuery(where.keySet(), override);

        List<Object> keys = new ArrayList<Object>();
        keys.addAll(where.values());

        query.setKeys(keys);

        try {
            return query.run();
        } catch (CouchbaseLiteException e) {
            Log.i(TAG, "Cannot run query", e);
        }
        return null;
    }

    public int count() {
        return count(false);
    }

    public int count(boolean override) {
        QueryEnumerator enumerator = null;
        try {
            enumerator = createQuery(override).run();
            return null == enumerator ? 0 : enumerator.getCount();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int count(String key, String value) {
        QueryEnumerator enumerator = find(key, value);
        return null == enumerator ? 0 : enumerator.getCount();
    }

    public int count(Map<String, String> where) {
        return count(where, false);
    }

    public int count(Map<String, String> where, boolean override) {
        QueryEnumerator enumerator = find(where, override);
        return null == enumerator ? 0 : enumerator.getCount();
    }

    public QueryEnumerator between(String key, String start, String end) {
        return between(key, start, end, false);
    }

    public QueryEnumerator between(String key, String start, String end, boolean override) {
        Query query = createQuery(key, false);
        query.setDescending(false);
        if (null != start)
            query.setStartKey(start);
        if (null != end)
            query.setEndKey(end);
        try {
            return query.run();
        } catch (CouchbaseLiteException e) {
            Log.i(TAG, "Cannot run query", e);
        }
        return null;
    }

    public List<Document> get() {
        return get(false);
    }

    public List<Document> get(boolean override) {
        try {
            return getList(createQuery(override).run());
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return new ArrayList<Document>();
    }

    public List<Document> get(String key, String value) {
        return get(key, value, false);
    }

    public List<Document> get(String key, String value, boolean override) {
        return getList(find(key, value, override));
    }

    public List<Document> get(Map<String, String> where, boolean override) {
        return get(where, override);
    }

    public List<Document> get(Map<String, String> where) {
        return getList(find(where));
    }

    public List<Document> getList(QueryEnumerator it) {
        List<Document> list = new ArrayList<Document>();
        for (; it.hasNext(); ) {
            list.add(new Document(it.next().getDocument()));
        }
        return list;
    }

    public boolean delete(Document document) {
        try {
            return document.getDocument().delete();
        } catch (CouchbaseLiteException e) {
            Log.d(TAG, e.getMessage(), e);
        }
        return false;
    }
}

