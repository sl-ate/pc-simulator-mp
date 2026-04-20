package com.unity3d.player;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

/* JADX INFO: renamed from: com.unity3d.player.k */
/* JADX INFO: loaded from: classes2.dex */
final class C0920k {

    /* JADX INFO: renamed from: a */
    private Context f396a;

    /* JADX INFO: renamed from: b */
    private b f397b;

    /* JADX INFO: renamed from: com.unity3d.player.k$a */
    public interface a {
        /* JADX INFO: renamed from: b */
        void mo273b();
    }

    /* JADX INFO: renamed from: com.unity3d.player.k$b */
    private class b extends ContentObserver {

        /* JADX INFO: renamed from: b */
        private a f399b;

        public b(Handler handler, a aVar) {
            super(handler);
            this.f399b = aVar;
        }

        @Override // android.database.ContentObserver
        public final boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override // android.database.ContentObserver
        public final void onChange(boolean z) {
            a aVar = this.f399b;
            if (aVar != null) {
                aVar.mo273b();
            }
        }
    }

    public C0920k(Context context) {
        this.f396a = context;
    }

    /* JADX INFO: renamed from: a */
    public final void m390a() {
        if (this.f397b != null) {
            this.f396a.getContentResolver().unregisterContentObserver(this.f397b);
            this.f397b = null;
        }
    }

    /* JADX INFO: renamed from: a */
    public final void m391a(a aVar, String str) {
        this.f397b = new b(new Handler(Looper.getMainLooper()), aVar);
        this.f396a.getContentResolver().registerContentObserver(Settings.System.getUriFor(str), true, this.f397b);
    }
}
