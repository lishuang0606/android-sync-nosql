package com.znck.android.nosql.util;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSON {

    public static final String TAG = JSON.class.getName();

    public static String export(Map<String, Object> data) {
        Gson gson = new Gson();
        return gson.toJson(data);
    }

    public static Map<String, Object> parse(String jsonString) {
        Map<String, Object> properties = null;
        JSONObject json;
        try {
            json = new JSONObject(jsonString);
            properties = parse(json);
        } catch (JSONException e) {
            Log.d(TAG, "JSON parse exception", e);
        }
        return properties;
    }

    public static Map<String, Object> parse(JSONObject json) {
        try {
            return parse(json, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new HashMap<String, Object>();
    }

    protected static Map<String, Object> parse(JSONObject json, boolean in) throws JSONException {
        Map<String, Object> properties = new HashMap<String, Object>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);
            if (value instanceof JSONObject) {
                properties.put(key, parse((JSONObject) value));
            } else if (value instanceof JSONArray) {
                properties.put(key, parse((JSONArray) value));
            } else properties.put(key, value);
        }
        return properties;
    }

    protected static List<?> parse(JSONArray value) throws JSONException {
        List<Object> valueList = new ArrayList<Object>();
        for (int i = 0; i < ((JSONArray) value).length(); ++i) {
            Object object = ((JSONArray) value).get(i);
            if (object instanceof JSONObject) valueList.add(parse((JSONObject) object));
            else if (object instanceof JSONArray) valueList.add(parse((JSONArray) object));
            else valueList.add(object);
        }
        return valueList;
    }
}
