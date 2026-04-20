package com.unity3d.player;

import android.content.Context;
import android.provider.Settings;
import com.unity3d.player.C0920k;

/* JADX INFO: loaded from: classes2.dex */
public class OrientationLockListener implements C0920k.a {

    /* JADX INFO: renamed from: a */
    private C0920k f173a;

    /* JADX INFO: renamed from: b */
    private Context f174b;

    OrientationLockListener(Context context) {
        this.f174b = context;
        this.f173a = new C0920k(context);
        nativeUpdateOrientationLockState(Settings.System.getInt(this.f174b.getContentResolver(), "accelerometer_rotation", 0));
        this.f173a.m391a(this, "accelerometer_rotation");
    }

    /* JADX INFO: renamed from: a */
    public final void m272a() {
        this.f173a.m390a();
        this.f173a = null;
    }

    @Override // com.unity3d.player.C0920k.a
    /* JADX INFO: renamed from: b */
    public final void mo273b() {
        nativeUpdateOrientationLockState(Settings.System.getInt(this.f174b.getContentResolver(), "accelerometer_rotation", 0));
    }

    public final native void nativeUpdateOrientationLockState(int i);
}
