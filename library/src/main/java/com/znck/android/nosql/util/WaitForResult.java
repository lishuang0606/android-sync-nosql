package com.znck.android.nosql.util;

import android.util.Log;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class WaitForResult {
    private Lock lock = new ReentrantLock();
    private boolean working = false;
    private Condition workingCondition = lock.newCondition();

    abstract public void run();

    public final void execute() {
        Log.d("SYNC", "Executing tasks");
        lock.lock();
        try {
            run();
            while (working) {
                Log.d("SYNC", "Waiting for tasks");
                workingCondition.awaitUninterruptibly();
            }
        } finally {
            Log.d("SYNC", "Tasks finished");
            unlock();
            lock.unlock();
        }
    }

    public final void lock() {
        working = true;
    }

    public final void unlock() {
        working = false;
    }
}