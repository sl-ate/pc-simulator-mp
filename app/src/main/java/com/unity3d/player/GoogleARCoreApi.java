package com.unity3d.player;

import android.app.Activity;

/* JADX INFO: loaded from: classes2.dex */
public class GoogleARCoreApi {
    public final native void initializeARCore(Activity activity);

    public final native void pauseARCore();

    public final native void resumeARCore();
}
