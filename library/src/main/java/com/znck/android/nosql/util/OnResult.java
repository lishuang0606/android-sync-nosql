package com.znck.android.nosql.util;

import org.apache.http.Header;
import org.json.JSONObject;

abstract public class OnResult {
    public enum ContentType {
        XML,
        OdML,
        JSON,
        TEXT,
        UNKNOWN
    }
    public ContentType contentType;

    public abstract void run(int statusCode, Header[] headers, JSONObject response, String responseString);

    public void fail(int statusCode, Header[] headers, String responseString, Throwable throwable) {

    }
}
