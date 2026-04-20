package com.unity3d.player;

import android.os.Build;
import java.lang.Thread;

/* JADX INFO: renamed from: com.unity3d.player.l */
/* JADX INFO: loaded from: classes2.dex */
final class C0921l implements Thread.UncaughtExceptionHandler {

    /* JADX INFO: renamed from: a */
    private volatile Thread.UncaughtExceptionHandler f400a;

    C0921l() {
    }

    /* JADX INFO: renamed from: a */
    final synchronized boolean m392a() {
        boolean z;
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (defaultUncaughtExceptionHandler == this) {
            z = false;
        } else {
            this.f400a = defaultUncaughtExceptionHandler;
            Thread.setDefaultUncaughtExceptionHandler(this);
            z = true;
        }
        return z;
    }

    @Override // java.lang.Thread.UncaughtExceptionHandler
    public final synchronized void uncaughtException(Thread thread, Throwable th) {
        try {
            Error error = new Error(String.format("FATAL EXCEPTION [%s]\n", thread.getName()) + String.format("Unity version     : %s\n", "2021.3.17f1") + String.format("Device model      : %s %s\n", Build.MANUFACTURER, Build.MODEL) + String.format("Device fingerprint: %s\n", Build.FINGERPRINT) + String.format("Build Type        : %s\n", "Release") + String.format("Scripting Backend : %s\n", "IL2CPP") + String.format("ABI               : %s\n", Build.CPU_ABI) + String.format("Strip Engine Code : %s\n", true));
            error.setStackTrace(new StackTraceElement[0]);
            error.initCause(th);
            this.f400a.uncaughtException(thread, error);
        } catch (Throwable unused) {
            this.f400a.uncaughtException(thread, th);
        }
    }
}
