package com.unity3d.player;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;

/* JADX INFO: loaded from: classes2.dex */
public class Camera2Wrapper implements InterfaceC0914e {

    /* JADX INFO: renamed from: a */
    private Context f152a;

    /* JADX INFO: renamed from: b */
    private C0912c f153b = null;

    /* JADX INFO: renamed from: c */
    private final int f154c = 100;

    public Camera2Wrapper(Context context) {
        this.f152a = context;
        initCamera2Jni();
    }

    /* JADX INFO: renamed from: a */
    private static int m258a(float f) {
        return (int) Math.min(Math.max((f * 2000.0f) - 1000.0f, -900.0f), 900.0f);
    }

    private final native void deinitCamera2Jni();

    private final native void initCamera2Jni();

    private final native void nativeFrameReady(Object obj, Object obj2, Object obj3, int i, int i2, int i3);

    private final native void nativeSurfaceTextureReady(Object obj);

    /* JADX INFO: renamed from: a */
    public final void m259a() {
        deinitCamera2Jni();
        closeCamera2();
    }

    @Override // com.unity3d.player.InterfaceC0914e
    /* JADX INFO: renamed from: a */
    public final void mo260a(Object obj) {
        nativeSurfaceTextureReady(obj);
    }

    @Override // com.unity3d.player.InterfaceC0914e
    /* JADX INFO: renamed from: a */
    public final void mo261a(Object obj, Object obj2, Object obj3, int i, int i2, int i3) {
        nativeFrameReady(obj, obj2, obj3, i, i2, i3);
    }

    protected void closeCamera2() {
        C0912c c0912c = this.f153b;
        if (c0912c != null) {
            c0912c.m362b();
        }
        this.f153b = null;
    }

    protected int getCamera2Count() {
        return C0912c.m324a(this.f152a);
    }

    protected int getCamera2FocalLengthEquivalent(int i) {
        return C0912c.m345d(this.f152a, i);
    }

    protected int[] getCamera2Resolutions(int i) {
        return C0912c.m348e(this.f152a, i);
    }

    protected int getCamera2SensorOrientation(int i) {
        return C0912c.m325a(this.f152a, i);
    }

    protected Object getCameraFocusArea(float f, float f2) {
        int iM258a = m258a(f);
        int iM258a2 = m258a(1.0f - f2);
        return new Camera.Area(new Rect(iM258a - 100, iM258a2 - 100, iM258a + 100, iM258a2 + 100), 1000);
    }

    protected Rect getFrameSizeCamera2() {
        C0912c c0912c = this.f153b;
        return c0912c != null ? c0912c.m359a() : new Rect();
    }

    protected boolean initializeCamera2(int i, int i2, int i3, int i4, int i5) {
        if (this.f153b != null || UnityPlayer.currentActivity == null) {
            return false;
        }
        C0912c c0912c = new C0912c(this);
        this.f153b = c0912c;
        return c0912c.m361a(this.f152a, i, i2, i3, i4, i5);
    }

    protected boolean isCamera2AutoFocusPointSupported(int i) {
        return C0912c.m343c(this.f152a, i);
    }

    protected boolean isCamera2FrontFacing(int i) {
        return C0912c.m341b(this.f152a, i);
    }

    protected void pauseCamera2() {
        C0912c c0912c = this.f153b;
        if (c0912c != null) {
            c0912c.m364d();
        }
    }

    protected boolean setAutoFocusPoint(float f, float f2) {
        C0912c c0912c = this.f153b;
        if (c0912c != null) {
            return c0912c.m360a(f, f2);
        }
        return false;
    }

    protected void startCamera2() {
        C0912c c0912c = this.f153b;
        if (c0912c != null) {
            c0912c.m363c();
        }
    }

    protected void stopCamera2() {
        C0912c c0912c = this.f153b;
        if (c0912c != null) {
            c0912c.m365e();
        }
    }
}
