package com.znck.android.nosql.util;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSON {

    public static final String TAG = JSON.class.getName();

    private static Map<String, String> flatten(Map<String, String> to, Object cur, String property) {
        if (cur instanceof String) {
            to.put(property, (String) cur);
        } else if (cur instanceof Collection) {
            int i = 0;
            for (Object object : (Collection<?>) cur) {
                flatten(to, object, property.length() > 0 ? property + "[" + i + "]" : "" + i);
                i++;
            }
            if (i == 0) {
                to.put(property, "");
            }
        } else if (cur instanceof Map) {
            boolean isEmpty = true;
            Map<String, Object> map = (Map) cur;
            for (Map.Entry<String, Object> object : map.entrySet()) {
                isEmpty = false;
                String p = object.getKey();
                flatten(to, object.getValue(), property.length() > 0 ? property + "." + p : p);
            }
            if (isEmpty)
                to.put(property, "");
        } else if (cur instanceof JSONObject) {
            boolean isEmpty = true;
            Iterator<String> it = ((JSONObject) cur).keys();
            while (it.hasNext()) {
                isEmpty = false;
                String p = it.next();
                try {
                    flatten(to, ((JSONObject) cur).get(p), property.length() > 0 ? property + "." + p : p);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (isEmpty)
                to.put(property, "");
        } else if (cur instanceof JSONArray) {
            int i = 0;
            for (; i < ((JSONArray) cur).length(); ++i) {
                try {
                    flatten(to, ((JSONArray) cur).get(i), property.length() > 0 ? property + "[" + i + "]" : "" + i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (i == 0) {
                to.put(property, "");
            }
        } else {
            to.put(property, cur.toString());
        }
        return to;
    }

    public static Map<String, String> flatten(Map<String, Object> map) {
        Map<String, String> flat = new HashMap<String, String>();
        return (Map<String, String>) flatten(flat, map, "");
    }

//    public static Map<String, Object> deflate(Map<String, Object> map) {
//        Map<String, Object> to = new HashMap<String, Object>();
//        String regex = "\\.?([^.\\[\\]]+)|\\[(\\d+)\\]";
//        for (Map.Entry<String, Object> entry : map.entrySet()) {
//            String key = entry.getKey();
//            Object cur = null;
//            while (!key.matches(regex)) {
//                String[] subKeys = key.split("\\.|\\[|\\]", 2);
//
//            }
//        }
//        return to;
//    }

    public static Map<String, String> fromGetString(String data) {
        String[] vars = data.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String var : vars) {
            String[] pair = var.split("=");
            map.put(pair[0], pair[1]);
        }
        return map;
    }

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
        if (null != json) {
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
        }
        return properties;
    }

    protected static List<?> parse(JSONArray value) throws JSONException {
        List<Object> valueList = new ArrayList<Object>();
        if (null != value) {
            for (int i = 0; i < value.length(); ++i) {
                Object object = value.get(i);
                if (object instanceof JSONObject) valueList.add(parse((JSONObject) object));
                else if (object instanceof JSONArray) valueList.add(parse((JSONArray) object));
                else valueList.add(object);
            }
        }
        return valueList;
    }
}
