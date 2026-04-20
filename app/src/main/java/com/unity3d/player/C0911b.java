package com.unity3d.player;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

/* JADX INFO: renamed from: com.unity3d.player.b */
/* JADX INFO: loaded from: classes2.dex */
final class C0911b {

    /* JADX INFO: renamed from: a */
    private final Context f309a;

    /* JADX INFO: renamed from: b */
    private final AudioManager f310b;

    /* JADX INFO: renamed from: c */
    private a f311c;

    /* JADX INFO: renamed from: com.unity3d.player.b$a */
    private class a extends ContentObserver {

        /* JADX INFO: renamed from: b */
        private final b f313b;

        /* JADX INFO: renamed from: c */
        private final AudioManager f314c;

        /* JADX INFO: renamed from: d */
        private final int f315d;

        /* JADX INFO: renamed from: e */
        private int f316e;

        public a(Handler handler, AudioManager audioManager, int i, b bVar) {
            super(handler);
            this.f314c = audioManager;
            this.f315d = 3;
            this.f313b = bVar;
            this.f316e = audioManager.getStreamVolume(3);
        }

        @Override // android.database.ContentObserver
        public final boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override // android.database.ContentObserver
        public final void onChange(boolean z, Uri uri) {
            int streamVolume;
            AudioManager audioManager = this.f314c;
            if (audioManager == null || this.f313b == null || (streamVolume = audioManager.getStreamVolume(this.f315d)) == this.f316e) {
                return;
            }
            this.f316e = streamVolume;
            this.f313b.onAudioVolumeChanged(streamVolume);
        }
    }

    /* JADX INFO: renamed from: com.unity3d.player.b$b */
    public interface b {
        void onAudioVolumeChanged(int i);
    }

    public C0911b(Context context) {
        this.f309a = context;
        this.f310b = (AudioManager) context.getSystemService("audio");
    }

    /* JADX INFO: renamed from: a */
    public final void m322a() {
        if (this.f311c != null) {
            this.f309a.getContentResolver().unregisterContentObserver(this.f311c);
            this.f311c = null;
        }
    }

    /* JADX INFO: renamed from: a */
    public final void m323a(b bVar) {
        this.f311c = new a(new Handler(), this.f310b, 3, bVar);
        this.f309a.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this.f311c);
    }
}
