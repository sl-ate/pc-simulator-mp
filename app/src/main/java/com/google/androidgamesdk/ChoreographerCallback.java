package com.google.androidgamesdk;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;

/* JADX INFO: loaded from: classes.dex */
public class ChoreographerCallback implements Choreographer.FrameCallback {
    private static final String LOG_TAG = "ChoreographerCallback";
    private long mCookie;
    private C0366a mLooper;

    /* JADX INFO: renamed from: com.google.androidgamesdk.ChoreographerCallback$a */
    private class C0366a extends Thread {

        /* JADX INFO: renamed from: a */
        public Handler f45a;

        private C0366a() {
        }

        /* synthetic */ C0366a(ChoreographerCallback choreographerCallback, byte b) {
            this();
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public final void run() {
            Log.i(ChoreographerCallback.LOG_TAG, "Starting looper thread");
            Looper.prepare();
            this.f45a = new Handler();
            Looper.loop();
            Log.i(ChoreographerCallback.LOG_TAG, "Terminating looper thread");
        }
    }

    public ChoreographerCallback(long j) {
        this.mCookie = j;
        C0366a c0366a = new C0366a(this, (byte) 0);
        this.mLooper = c0366a;
        c0366a.start();
    }

    @Override // android.view.Choreographer.FrameCallback
    public void doFrame(long j) {
        nOnChoreographer(this.mCookie, j);
    }

    public native void nOnChoreographer(long j, long j2);

    public void postFrameCallback() {
        this.mLooper.f45a.post(new Runnable() { // from class: com.google.androidgamesdk.ChoreographerCallback.1
            @Override // java.lang.Runnable
            public final void run() {
                Choreographer.getInstance().postFrameCallback(ChoreographerCallback.this);
            }
        });
    }

    public void postFrameCallbackDelayed(long j) {
        Choreographer.getInstance().postFrameCallbackDelayed(this, j);
    }

    public void terminate() {
        this.mLooper.f45a.getLooper().quit();
    }
}
