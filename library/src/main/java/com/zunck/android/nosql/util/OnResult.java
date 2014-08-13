package com.zunck.android.nosql.util;

import org.apache.http.Header;
import org.json.JSONObject;

abstract public class OnResult {
    public abstract void run(int statusCode, Header[] headers, JSONObject response);
}
