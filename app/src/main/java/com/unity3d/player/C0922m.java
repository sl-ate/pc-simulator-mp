package com.unity3d.player;

/* JADX INFO: renamed from: com.unity3d.player.m */
/* JADX INFO: loaded from: classes2.dex */
final class C0922m {

    /* JADX INFO: renamed from: a */
    private static boolean f401a = false;

    /* JADX INFO: renamed from: b */
    private boolean f402b = false;

    /* JADX INFO: renamed from: c */
    private boolean f403c = false;

    /* JADX INFO: renamed from: d */
    private boolean f404d = true;

    /* JADX INFO: renamed from: e */
    private boolean f405e = false;

    C0922m() {
    }

    /* JADX INFO: renamed from: a */
    static void m393a() {
        f401a = true;
    }

    /* JADX INFO: renamed from: b */
    static void m394b() {
        f401a = false;
    }

    /* JADX INFO: renamed from: c */
    static boolean m395c() {
        return f401a;
    }

    /* JADX INFO: renamed from: a */
    final void m396a(boolean z) {
        this.f402b = z;
    }

    /* JADX INFO: renamed from: b */
    final void m397b(boolean z) {
        this.f404d = z;
    }

    /* JADX INFO: renamed from: c */
    final void m398c(boolean z) {
        this.f405e = z;
    }

    /* JADX INFO: renamed from: d */
    final void m399d(boolean z) {
        this.f403c = z;
    }

    /* JADX INFO: renamed from: d */
    final boolean m400d() {
        return this.f404d;
    }

    /* JADX INFO: renamed from: e */
    final boolean m401e() {
        return this.f405e;
    }

    /* JADX INFO: renamed from: e */
    final boolean m402e(boolean z) {
        if (f401a) {
            return ((!z && !this.f402b) || this.f404d || this.f403c) ? false : true;
        }
        return false;
    }

    /* JADX INFO: renamed from: f */
    final boolean m403f() {
        return this.f403c;
    }

    public final String toString() {
        return super.toString();
    }
}
