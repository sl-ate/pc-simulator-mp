package com.unity3d.player;

import android.app.Activity;
import android.content.Context;
import com.unity3d.player.SurfaceHolderCallbackC0923n;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* JADX INFO: renamed from: com.unity3d.player.o */
/* JADX INFO: loaded from: classes2.dex */
final class C0924o {

    /* JADX INFO: renamed from: a */
    private UnityPlayer f435a;

    /* JADX INFO: renamed from: c */
    private a f437c;

    /* JADX INFO: renamed from: b */
    private Context f436b = null;

    /* JADX INFO: renamed from: d */
    private final Semaphore f438d = new Semaphore(0);

    /* JADX INFO: renamed from: e */
    private final Lock f439e = new ReentrantLock();

    /* JADX INFO: renamed from: f */
    private SurfaceHolderCallbackC0923n f440f = null;

    /* JADX INFO: renamed from: g */
    private int f441g = 2;

    /* JADX INFO: renamed from: h */
    private boolean f442h = false;

    /* JADX INFO: renamed from: i */
    private boolean f443i = false;

    /* JADX INFO: renamed from: com.unity3d.player.o$1, reason: invalid class name */
    final class AnonymousClass1 implements Runnable {

        /* JADX INFO: renamed from: a */
        final /* synthetic */ String f444a;

        /* JADX INFO: renamed from: b */
        final /* synthetic */ int f445b;

        /* JADX INFO: renamed from: c */
        final /* synthetic */ int f446c;

        /* JADX INFO: renamed from: d */
        final /* synthetic */ int f447d;

        /* JADX INFO: renamed from: e */
        final /* synthetic */ boolean f448e;

        /* JADX INFO: renamed from: f */
        final /* synthetic */ long f449f;

        /* JADX INFO: renamed from: g */
        final /* synthetic */ long f450g;

        AnonymousClass1(String str, int i, int i2, int i3, boolean z, long j, long j2) {
            this.f444a = str;
            this.f445b = i;
            this.f446c = i2;
            this.f447d = i3;
            this.f448e = z;
            this.f449f = j;
            this.f450g = j2;
        }

        @Override // java.lang.Runnable
        public final void run() {
            if (C0924o.this.f440f != null) {
                C0915f.Log(5, "Video already playing");
                C0924o.this.f441g = 2;
                C0924o.this.f438d.release();
            } else {
                C0924o.this.f440f = new SurfaceHolderCallbackC0923n(C0924o.this.f436b, this.f444a, this.f445b, this.f446c, this.f447d, this.f448e, this.f449f, this.f450g, new SurfaceHolderCallbackC0923n.a() { // from class: com.unity3d.player.o.1.1
                    @Override // com.unity3d.player.SurfaceHolderCallbackC0923n.a
                    /* JADX INFO: renamed from: a */
                    public final void mo411a(int i) {
                        C0924o.this.f439e.lock();
                        C0924o.this.f441g = i;
                        if (i == 3 && C0924o.this.f443i) {
                            C0924o.this.runOnUiThread(new Runnable() { // from class: com.unity3d.player.o.1.1.1
                                @Override // java.lang.Runnable
                                public final void run() {
                                    C0924o.this.m419d();
                                    C0924o.this.f435a.resume();
                                }
                            });
                        }
                        if (i != 0) {
                            C0924o.this.f438d.release();
                        }
                        C0924o.this.f439e.unlock();
                    }
                });
                if (C0924o.this.f440f != null) {
                    C0924o.this.f435a.addView(C0924o.this.f440f);
                }
            }
        }
    }

    /* JADX INFO: renamed from: com.unity3d.player.o$a */
    public interface a {
        /* JADX INFO: renamed from: a */
        void mo289a();
    }

    C0924o(UnityPlayer unityPlayer) {
        this.f435a = null;
        this.f435a = unityPlayer;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: d */
    public void m419d() {
        SurfaceHolderCallbackC0923n surfaceHolderCallbackC0923n = this.f440f;
        if (surfaceHolderCallbackC0923n != null) {
            this.f435a.removeViewFromPlayer(surfaceHolderCallbackC0923n);
            this.f443i = false;
            this.f440f.destroyPlayer();
            this.f440f = null;
            a aVar = this.f437c;
            if (aVar != null) {
                aVar.mo289a();
            }
        }
    }

    /* JADX INFO: renamed from: h */
    static /* synthetic */ boolean m423h(C0924o c0924o) {
        c0924o.f443i = true;
        return true;
    }

    /* JADX INFO: renamed from: a */
    public final void m424a() {
        this.f439e.lock();
        SurfaceHolderCallbackC0923n surfaceHolderCallbackC0923n = this.f440f;
        if (surfaceHolderCallbackC0923n != null) {
            if (this.f441g == 0) {
                surfaceHolderCallbackC0923n.CancelOnPrepare();
            } else if (this.f443i) {
                boolean zM410a = surfaceHolderCallbackC0923n.m410a();
                this.f442h = zM410a;
                if (!zM410a) {
                    this.f440f.pause();
                }
            }
        }
        this.f439e.unlock();
    }

    /* JADX INFO: renamed from: a */
    public final boolean m425a(Context context, String str, int i, int i2, int i3, boolean z, long j, long j2, a aVar) {
        this.f439e.lock();
        this.f437c = aVar;
        this.f436b = context;
        this.f438d.drainPermits();
        this.f441g = 2;
        runOnUiThread(new AnonymousClass1(str, i, i2, i3, z, j, j2));
        boolean z2 = false;
        try {
            this.f439e.unlock();
            this.f438d.acquire();
            this.f439e.lock();
            if (this.f441g != 2) {
                z2 = true;
            }
        } catch (InterruptedException unused) {
        }
        runOnUiThread(new Runnable() { // from class: com.unity3d.player.o.2
            @Override // java.lang.Runnable
            public final void run() {
                C0924o.this.f435a.pause();
            }
        });
        runOnUiThread((!z2 || this.f441g == 3) ? new Runnable() { // from class: com.unity3d.player.o.4
            @Override // java.lang.Runnable
            public final void run() {
                C0924o.this.m419d();
                C0924o.this.f435a.resume();
            }
        } : new Runnable() { // from class: com.unity3d.player.o.3
            @Override // java.lang.Runnable
            public final void run() {
                if (C0924o.this.f440f != null) {
                    C0924o.this.f435a.addViewToPlayer(C0924o.this.f440f, true);
                    C0924o.m423h(C0924o.this);
                    C0924o.this.f440f.requestFocus();
                }
            }
        });
        this.f439e.unlock();
        return z2;
    }

    /* JADX INFO: renamed from: b */
    public final void m426b() {
        this.f439e.lock();
        SurfaceHolderCallbackC0923n surfaceHolderCallbackC0923n = this.f440f;
        if (surfaceHolderCallbackC0923n != null && this.f443i && !this.f442h) {
            surfaceHolderCallbackC0923n.start();
        }
        this.f439e.unlock();
    }

    /* JADX INFO: renamed from: c */
    public final void m427c() {
        this.f439e.lock();
        SurfaceHolderCallbackC0923n surfaceHolderCallbackC0923n = this.f440f;
        if (surfaceHolderCallbackC0923n != null) {
            surfaceHolderCallbackC0923n.updateVideoLayout();
        }
        this.f439e.unlock();
    }

    protected final void runOnUiThread(Runnable runnable) {
        Context context = this.f436b;
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(runnable);
        } else {
            C0915f.Log(5, "Not running from an Activity; Ignoring execution request...");
        }
    }
}
