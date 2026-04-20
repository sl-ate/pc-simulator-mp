package com.unity3d.player;

import android.os.Build;

/* JADX INFO: loaded from: classes2.dex */
public class PlatformSupport {
    static final boolean MARSHMALLOW_SUPPORT;
    static final boolean NOUGAT_SUPPORT;

    static {
        MARSHMALLOW_SUPPORT = Build.VERSION.SDK_INT >= 23;
        NOUGAT_SUPPORT = Build.VERSION.SDK_INT >= 24;
    }
}
