package com.znck.android.nosql.sync;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;
import com.znck.android.nosql.Document;
import com.znck.android.nosql.util.Callback;
import com.znck.android.nosql.util.JSON;
import com.znck.android.nosql.util.OnResult;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class HttpSyncManager extends AsyncSyncManager {
    private static SyncHttpClient httpClient = new SyncHttpClient();

    protected String host;

    public HttpSyncManager(String host, Context context, MergeScheme mergeScheme) {
        super(context, mergeScheme);
        this.host = host;
    }

    public enum AuthType {
        BASIC_AUTH,
        GET_PARAMETER,
        POST_PARAMETER,
        NONE
    }

    public void stopSync() {
        httpClient.cancelAllRequests(true);
    }

    public AuthType authType = AuthType.NONE;

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public void setUser(String key, String user) {
        this.user = new Pair<String, String>(key, user);
    }

    public void setPass(String key, String pass) {
        this.pass = new Pair<String, String>(key, pass);
    }

    protected Pair<String, String> user;
    protected Pair<String, String> pass;

    protected class ResponseHandler extends TextHttpResponseHandler {
        private OnResult callback;

        public ResponseHandler(OnResult request) {
            this.callback = request != null ? request : new OnResult() {
                @Override
                public void run(int statusCode, Header[] headers, JSONObject response, String responseString) {

                }
            };
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, String responseString) {
            String contentType = "text/html";
            for (Header header : headers) {
                if (header.getName().equals("Content-Type")) {
                    contentType = header.getValue();
                    break;
                }
            }
            JSONObject object = null;
            if (contentType.matches(".*/xml$")) {

            } else if(contentType.matches(".*/json$")) {
                try {
                    object = new JSONObject(responseString);
                } catch (JSONException e) {
                    Log.d("NoSQL-SYNC", "Invalid JSON string", e);
                }
            } else if(contentType.matches(".*/plain")) {

            } else {

            }
            callback.run(statusCode, headers, object, responseString);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            onSuccess(statusCode, headers, responseString);
            Log.d("NoSQL-SYNC", "Network query failed", throwable);
        }
    }

    private Map<String, String> requestHeaders = new HashMap<String, String>();

    @Override
    protected final void startUpload(Date start) {
        List<Document> docs = uploadList(start);
        for (Document doc : docs) {
            JSONObject object = upload(doc);
            afterUpload(doc, object);
            doc.updateSyncTimestamp(start);
            finalize(doc);
        }
    }

    @Override
    protected final void startDownload(Date start) {
        List<String> urls = downloadList(start);
        for (String url : urls) {
            JSONObject object = download(url);
            Document doc = new Document(JSON.parse(parseDownload(object)));
            afterDownload(doc, object);
            detectConflict(doc);
            finalize(doc);
        }
    }

    public final void get(String request, Map<String, String> query, OnResult onResult) {
        RequestParams requestParams = new RequestParams(query);

        addAuth(requestParams, null);

        httpClient.get(host + request, requestParams, new ResponseHandler(onResult));
    }

    public final void post(String request, Map<String, String> query, Callback callback, OnResult onResult) {
        post(request, query, null, callback, onResult);
    }

    public final void post(String request, Map<String, String> query, Map<String, String> body, OnResult onResult) {
        post(request, query, body, null, onResult);
    }

    public final void post(String request, Map<String, String> query, Map<String, String> body, Callback callback, OnResult onResult) {
        RequestParams requestParams = new RequestParams(query);
        RequestParams postParams = null == body ? null : new RequestParams(body);

        if (null != callback) callback.run(postParams);

        addAuth(requestParams, postParams);

        httpClient.post(host + request + "?" + requestParams.toString(), postParams, new ResponseHandler(onResult));
    }

    public final void put(String request, Map<String, String> query, Callback callback, OnResult onResult) {
        put(request, query, null, callback, onResult);
    }

    public final void put(String request, Map<String, String> query, Map<String, String> body, OnResult onResult) {
        put(request, query, body, null, onResult);
    }

    public final void put(String request, Map<String, String> query, Map<String, String> body, Callback callback, OnResult onResult) {
        RequestParams requestParams = new RequestParams(query);
        RequestParams postParams = null == body ? null : new RequestParams(body);

        if (null != callback) callback.run(postParams);

        addAuth(requestParams, postParams);

        httpClient.put(host + request + "?" + requestParams.toString(), postParams, new ResponseHandler(onResult));
    }

    public final void delete(String request, Map<String, String> query, OnResult onResult) {
        RequestParams requestParams = new RequestParams(query);

        addAuth(requestParams, null);

        httpClient.delete(host + request + "?" + requestParams.toString(), new ResponseHandler(onResult));
    }

    public final void head(String request, Map<String, String> query, OnResult onResult) {
        RequestParams requestParams = new RequestParams(query);

        addAuth(requestParams, null);

        httpClient.head(host + request + "?" + requestParams.toString(), new ResponseHandler(onResult));
    }

    public final boolean addHeader(String headerName, String header) {
        return addHeader(headerName, header, false);
    }

    public final boolean addHeader(String headerName, String header, boolean overwrite) {
        if (requestHeaders.containsKey(headerName) && !overwrite) {
            return false;
        }
        requestHeaders.put(headerName, header);
        loadHeaders();
        return true;
    }

    public final void removeHeader(String headerName) {
        requestHeaders.remove(headerName);
    }

    public final void resetHeaders() {
        requestHeaders.clear();
    }

    protected void addAuth(RequestParams get, RequestParams post) {
        switch (authType) {
            case POST_PARAMETER:
                if (null == post)
                    throw new IllegalArgumentException("Cannot add authentication. Either get request or post request with binary data");
                post.add(user.first, user.second);
                post.add(pass.first, pass.second);
                break;
            case GET_PARAMETER:
                get.add(user.first, user.second);
                get.add(pass.first, pass.second);
                break;
            case BASIC_AUTH:
                httpClient.setBasicAuth(user.second, pass.second);
                break;
            case NONE:
            default:
        }
    }

    public final void loadHeaders() {
        httpClient.removeAllHeaders();
        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            httpClient.addHeader(entry.getKey(), entry.getValue());
        }
    }

    protected abstract void afterUpload(Document document, JSONObject response);

    protected abstract JSONObject parseDownload(JSONObject response);

    protected abstract void afterDownload(Document document, JSONObject response);
}