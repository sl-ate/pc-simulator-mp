package org.fmod;

import android.media.AudioRecord;
import android.util.Log;
import java.nio.ByteBuffer;

/* JADX INFO: renamed from: org.fmod.a */
/* JADX INFO: loaded from: classes2.dex */
final class RunnableC1288a implements Runnable {

    /* JADX INFO: renamed from: a */
    private final FMODAudioDevice f493a;

    /* JADX INFO: renamed from: b */
    private final ByteBuffer f494b;

    /* JADX INFO: renamed from: c */
    private final int f495c;

    /* JADX INFO: renamed from: d */
    private final int f496d;

    /* JADX INFO: renamed from: e */
    private final int f497e = 2;

    /* JADX INFO: renamed from: f */
    private volatile Thread f498f;

    /* JADX INFO: renamed from: g */
    private volatile boolean f499g;

    /* JADX INFO: renamed from: h */
    private AudioRecord f500h;

    /* JADX INFO: renamed from: i */
    private boolean f501i;

    RunnableC1288a(FMODAudioDevice fMODAudioDevice, int i, int i2) {
        this.f493a = fMODAudioDevice;
        this.f495c = i;
        this.f496d = i2;
        this.f494b = ByteBuffer.allocateDirect(AudioRecord.getMinBufferSize(i, i2, 2));
    }

    /* JADX INFO: renamed from: d */
    private void m456d() {
        AudioRecord audioRecord = this.f500h;
        if (audioRecord != null) {
            if (audioRecord.getState() == 1) {
                this.f500h.stop();
            }
            this.f500h.release();
            this.f500h = null;
        }
        this.f494b.position(0);
        this.f501i = false;
    }

    /* JADX INFO: renamed from: a */
    public final int m457a() {
        return this.f494b.capacity();
    }

    /* JADX INFO: renamed from: b */
    public final void m458b() {
        if (this.f498f != null) {
            m459c();
        }
        this.f499g = true;
        this.f498f = new Thread(this);
        this.f498f.start();
    }

    /* JADX INFO: renamed from: c */
    public final void m459c() {
        while (this.f498f != null) {
            this.f499g = false;
            try {
                this.f498f.join();
                this.f498f = null;
            } catch (InterruptedException unused) {
            }
        }
    }

    @Override // java.lang.Runnable
    public final void run() {
        int i = 3;
        while (this.f499g) {
            if (!this.f501i && i > 0) {
                m456d();
                AudioRecord audioRecord = new AudioRecord(1, this.f495c, this.f496d, this.f497e, this.f494b.capacity());
                this.f500h = audioRecord;
                boolean z = audioRecord.getState() == 1;
                this.f501i = z;
                if (z) {
                    this.f494b.position(0);
                    this.f500h.startRecording();
                    i = 3;
                } else {
                    Log.e("FMOD", "AudioRecord failed to initialize (status " + this.f500h.getState() + ")");
                    i += -1;
                    m456d();
                }
            }
            if (this.f501i && this.f500h.getRecordingState() == 3) {
                AudioRecord audioRecord2 = this.f500h;
                ByteBuffer byteBuffer = this.f494b;
                this.f493a.fmodProcessMicData(this.f494b, audioRecord2.read(byteBuffer, byteBuffer.capacity()));
                this.f494b.position(0);
            }
        }
        m456d();
    }
}
