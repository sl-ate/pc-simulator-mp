package com.unity3d.player;

import android.content.Context;
import com.unity3d.player.C0911b;

/* JADX INFO: loaded from: classes2.dex */
public class AudioVolumeHandler implements C0911b.b {

    /* JADX INFO: renamed from: a */
    private C0911b f151a;

    AudioVolumeHandler(Context context) {
        C0911b c0911b = new C0911b(context);
        this.f151a = c0911b;
        c0911b.m323a(this);
    }

    /* JADX INFO: renamed from: a */
    public final void m257a() {
        this.f151a.m322a();
        this.f151a = null;
    }

    @Override // com.unity3d.player.C0911b.b
    public final native void onAudioVolumeChanged(int i);
}
