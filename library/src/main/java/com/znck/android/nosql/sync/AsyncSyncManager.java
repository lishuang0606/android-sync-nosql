package com.znck.android.nosql.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

abstract class AsyncSyncManager extends BaseSyncManager {

    public AsyncSyncManager(Context context, MergeScheme mergeScheme) {
        super(context, mergeScheme);
    }

    @Override
    public final void startSync() {
        Log.d("SYNC", "Sync service");
        run();
    }

    private void startingSync() {
        super.startSync();
    }

    private void run(String... parameters) {
        try {
            AsyncTask<String, Double, String> runner = (new AsyncTask<String, Double, String>() {
                @Override
                protected String doInBackground(String... strings) {
                    Looper.prepare();
                    Log.d("SYNC", "Sync service: in background");
                    startingSync();
                    Looper.loop();
                    return "";
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    before();
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                    after();
                    result(s);
                }

                @Override
                protected void onProgressUpdate(Double... values) {
                    super.onProgressUpdate(values);
                    progress(values[0]);
                }

                @Override
                protected void onCancelled() {
                    super.onCancelled();
                    cancelled();
                }
            }).execute(parameters);
        } catch (Exception e) {
            Log.d("SYNC", "terminated due to error", e);
        }
    }

    public void before() {
    }

    public void after() {
    }

    public void cancelled() {

    }

    public void progress(Double progress) {
    }

    public void result(String content) {
    }
}
