package com.unity3d.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

/* JADX INFO: loaded from: classes2.dex */
public class HFPStatus {

    /* JADX INFO: renamed from: a */
    private Context f155a;

    /* JADX INFO: renamed from: e */
    private AudioManager f159e;

    /* JADX INFO: renamed from: b */
    private BroadcastReceiver f156b = null;

    /* JADX INFO: renamed from: c */
    private Intent f157c = null;

    /* JADX INFO: renamed from: d */
    private boolean f158d = false;

    /* JADX INFO: renamed from: f */
    private boolean f160f = false;

    /* JADX INFO: renamed from: g */
    private int f161g = EnumC0874a.f163a;

    /* JADX WARN: $VALUES field not found */
    /* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
    /* JADX INFO: renamed from: com.unity3d.player.HFPStatus$a */
    static final class EnumC0874a {

        /* JADX INFO: renamed from: a */
        public static final int f163a = 1;

        /* JADX INFO: renamed from: b */
        public static final int f164b = 2;

        /* JADX INFO: renamed from: c */
        private static final /* synthetic */ int[] f165c = {1, 2};
    }

    public HFPStatus(Context context) {
        this.f159e = null;
        this.f155a = context;
        this.f159e = (AudioManager) context.getSystemService("audio");
        initHFPStatusJni();
    }

    /* JADX INFO: renamed from: b */
    private void m264b() {
        BroadcastReceiver broadcastReceiver = this.f156b;
        if (broadcastReceiver != null) {
            this.f155a.unregisterReceiver(broadcastReceiver);
            this.f156b = null;
            this.f157c = null;
        }
        this.f161g = EnumC0874a.f163a;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: c */
    public void m267c() {
        if (this.f160f) {
            this.f160f = false;
            this.f159e.stopBluetoothSco();
        }
    }

    private final native void deinitHFPStatusJni();

    private final native void initHFPStatusJni();

    /* JADX INFO: renamed from: a */
    public final void m268a() {
        clearHFPStat();
        deinitHFPStatusJni();
    }

    protected void clearHFPStat() {
        m264b();
        m267c();
    }

    protected boolean getHFPStat() {
        return this.f161g == EnumC0874a.f164b;
    }

    protected void requestHFPStat() {
        clearHFPStat();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.unity3d.player.HFPStatus.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (intent.getIntExtra("android.media.extra.SCO_AUDIO_STATE", -1) != 1) {
                    return;
                }
                HFPStatus.this.f161g = EnumC0874a.f164b;
                HFPStatus.this.m267c();
                if (HFPStatus.this.f158d) {
                    HFPStatus.this.f159e.setMode(3);
                }
            }
        };
        this.f156b = broadcastReceiver;
        this.f157c = this.f155a.registerReceiver(broadcastReceiver, new IntentFilter("android.media.ACTION_SCO_AUDIO_STATE_UPDATED"));
        try {
            this.f160f = true;
            this.f159e.startBluetoothSco();
        } catch (NullPointerException unused) {
            C0915f.Log(5, "startBluetoothSco() failed. no bluetooth device connected.");
        }
    }

    protected void setHFPRecordingStat(boolean z) {
        this.f158d = z;
        if (z) {
            return;
        }
        this.f159e.setMode(0);
    }
}
