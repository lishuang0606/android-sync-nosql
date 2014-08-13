package com.zunck.android.nosql.util;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class WaitForResult {
    private Lock lock = new ReentrantLock();
    private boolean working = false;
    private Condition workingCondition = lock.newCondition();

    abstract public void run();

    public final void execute() {
        lock.lock();
        try {
            while (working) {
                workingCondition.awaitUninterruptibly();
            }
        } finally {
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