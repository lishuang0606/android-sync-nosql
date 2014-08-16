package com.znck.android.nosql.sync;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import com.couchbase.lite.QueryEnumerator;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;
import com.znck.android.nosql.DatabaseHelper;
import com.znck.android.nosql.Document;
import com.znck.android.nosql.util.Callback;
import com.znck.android.nosql.util.JSON;
import com.znck.android.nosql.util.OnResult;
import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

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

    protected static class ResponseHandler extends TextHttpResponseHandler {
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
                Log.d("SYNC-Document-type", responseString.trim().substring(responseString.length() - 7).toLowerCase());
                if (responseString.length() > 7 && responseString.trim().substring(responseString.length() - 7).toLowerCase().contains("</odml>")) {
                    callback.contentType = OnResult.ContentType.OdML;
                } else {
                    callback.contentType = OnResult.ContentType.XML;
                    try {
                        object = XML.toJSONObject(responseString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (contentType.matches(".*/json$")) {
                callback.contentType = OnResult.ContentType.JSON;
                try {
                    object = new JSONObject(responseString);
                } catch (JSONException e) {
                    Log.d("NoSQL-SYNC", "Invalid JSON string", e);
                }
            } else if (contentType.matches(".*/plain")) {
                callback.contentType = OnResult.ContentType.TEXT;
            } else {
                callback.contentType = OnResult.ContentType.UNKNOWN;
            }
            callback.run(statusCode, headers, object, responseString);
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            Log.d("NoSQL-SYNC", "Network query failed", throwable);
            callback.fail(statusCode, headers, responseString, throwable);
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
        QueryEnumerator it = DatabaseHelper.getInstance().between(Document.UPDATED_AT, dateFormat.format(start), null);
        Map<String, Boolean> have = new HashMap<String, Boolean>();
        while (it.hasNext()) {
            have.put((String) it.next().getDocumentProperties().get(Document.REMOTE_ID), true);
        }

        for (String url : urls) {
            String[] urlMeta = url.split("#", 2);
            JSONObject object = download(urlMeta[0], urlMeta[1], have.containsKey(urlMeta[0]));
            if (null != object) {
                Document doc = new Document((Map) JSON.parse(parseDownload(object)));
                doc.setRemoteId(urlMeta[0], start);
                afterDownload(doc, object);
                detectConflict(doc);
                finalize(doc);
            }
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
