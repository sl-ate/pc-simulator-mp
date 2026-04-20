package com.unity3d.player;

import android.util.Log;

/* JADX INFO: renamed from: com.unity3d.player.f */
/* JADX INFO: loaded from: classes2.dex */
final class C0915f {

    /* JADX INFO: renamed from: a */
    protected static boolean f356a = false;

    protected static void Log(int i, String str) {
        if (f356a) {
            return;
        }
        if (i == 6) {
            Log.e("Unity", str);
        }
        if (i == 5) {
            Log.w("Unity", str);
        }
    }
}
