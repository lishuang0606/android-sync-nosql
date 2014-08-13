package com.zunck.android.nosql.sync;

import android.content.Context;
import android.os.AsyncTask;

abstract class AsyncSyncManager extends BaseSyncManager {

    public AsyncSyncManager(Context context, MergeScheme mergeScheme) {
        super(context, mergeScheme);
    }

    @Override
    public final void startSync() {
        AsyncTask<String, Double, String> runner = (new AsyncTask<String, Double, String>() {
            @Override
            protected String doInBackground(String... strings) {
                run();
                return null;
            }
        }).execute();
    }

    private void startingSync() {
        super.startSync();
    }

    private void run(String... parameters) {
        AsyncTask<String, Double, String> runner = (new AsyncTask<String, Double, String>() {
            @Override
            protected String doInBackground(String... strings) {
                startingSync();
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
