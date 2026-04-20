package org.fmod;

import android.media.AudioTrack;
import android.util.Log;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes2.dex */
public class FMODAudioDevice implements Runnable {

    /* JADX INFO: renamed from: h */
    private static int f481h = 0;

    /* JADX INFO: renamed from: i */
    private static int f482i = 1;

    /* JADX INFO: renamed from: j */
    private static int f483j = 2;

    /* JADX INFO: renamed from: k */
    private static int f484k = 3;

    /* JADX INFO: renamed from: l */
    private static int f485l = 4;

    /* JADX INFO: renamed from: a */
    private volatile Thread f486a = null;

    /* JADX INFO: renamed from: b */
    private volatile boolean f487b = false;

    /* JADX INFO: renamed from: c */
    private AudioTrack f488c = null;

    /* JADX INFO: renamed from: d */
    private boolean f489d = false;

    /* JADX INFO: renamed from: e */
    private ByteBuffer f490e = null;

    /* JADX INFO: renamed from: f */
    private byte[] f491f = null;

    /* JADX INFO: renamed from: g */
    private volatile RunnableC1288a f492g;

    private native int fmodGetInfo(int i);

    private native int fmodProcess(ByteBuffer byteBuffer);

    private void releaseAudioTrack() {
        AudioTrack audioTrack = this.f488c;
        if (audioTrack != null) {
            if (audioTrack.getState() == 1) {
                this.f488c.stop();
            }
            this.f488c.release();
            this.f488c = null;
        }
        this.f490e = null;
        this.f491f = null;
        this.f489d = false;
    }

    public synchronized void close() {
        stop();
    }

    native int fmodProcessMicData(ByteBuffer byteBuffer, int i);

    public boolean isRunning() {
        return this.f486a != null && this.f486a.isAlive();
    }

    @Override // java.lang.Runnable
    public void run() {
        int i = 3;
        while (this.f487b) {
            if (!this.f489d && i > 0) {
                releaseAudioTrack();
                int iFmodGetInfo = fmodGetInfo(f481h);
                int i2 = fmodGetInfo(f485l) == 1 ? 4 : 12;
                int minBufferSize = AudioTrack.getMinBufferSize(iFmodGetInfo, i2, 2);
                int iFmodGetInfo2 = fmodGetInfo(f485l) * 2;
                int iRound = Math.round(minBufferSize * 1.1f) & (~(iFmodGetInfo2 - 1));
                int iFmodGetInfo3 = fmodGetInfo(f482i);
                int iFmodGetInfo4 = fmodGetInfo(f483j) * iFmodGetInfo3 * iFmodGetInfo2;
                AudioTrack audioTrack = new AudioTrack(3, iFmodGetInfo, i2, 2, iFmodGetInfo4 > iRound ? iFmodGetInfo4 : iRound, 1);
                this.f488c = audioTrack;
                boolean z = audioTrack.getState() == 1;
                this.f489d = z;
                if (z) {
                    ByteBuffer byteBufferAllocateDirect = ByteBuffer.allocateDirect(iFmodGetInfo3 * iFmodGetInfo2);
                    this.f490e = byteBufferAllocateDirect;
                    this.f491f = new byte[byteBufferAllocateDirect.capacity()];
                    this.f488c.play();
                    i = 3;
                } else {
                    Log.e("FMOD", "AudioTrack failed to initialize (status " + this.f488c.getState() + ")");
                    releaseAudioTrack();
                    i += -1;
                }
            }
            if (this.f489d) {
                if (fmodGetInfo(f484k) == 1) {
                    fmodProcess(this.f490e);
                    ByteBuffer byteBuffer = this.f490e;
                    byteBuffer.get(this.f491f, 0, byteBuffer.capacity());
                    this.f488c.write(this.f491f, 0, this.f490e.capacity());
                    this.f490e.position(0);
                } else {
                    releaseAudioTrack();
                }
            }
        }
        releaseAudioTrack();
    }

    public synchronized void start() {
        if (this.f486a != null) {
            stop();
        }
        this.f486a = new Thread(this, "FMODAudioDevice");
        this.f486a.setPriority(10);
        this.f487b = true;
        this.f486a.start();
        if (this.f492g != null) {
            this.f492g.m458b();
        }
    }

    public synchronized int startAudioRecord(int i, int i2, int i3) {
        if (this.f492g == null) {
            this.f492g = new RunnableC1288a(this, i, i2);
            this.f492g.m458b();
        }
        return this.f492g.m457a();
    }

    public synchronized void stop() {
        while (this.f486a != null) {
            this.f487b = false;
            try {
                this.f486a.join();
                this.f486a = null;
            } catch (InterruptedException unused) {
            }
        }
        if (this.f492g != null) {
            this.f492g.m459c();
        }
    }

    public synchronized void stopAudioRecord() {
        if (this.f492g != null) {
            this.f492g.m459c();
            this.f492g = null;
        }
    }
}
