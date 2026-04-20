package com.unity3d.player;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import java.io.FileInputStream;
import java.io.IOException;

/* JADX INFO: renamed from: com.unity3d.player.n */
/* JADX INFO: loaded from: classes2.dex */
public final class SurfaceHolderCallbackC0923n extends FrameLayout implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback, MediaController.MediaPlayerControl {

    /* JADX INFO: renamed from: a */
    private static boolean f406a = false;

    /* JADX INFO: renamed from: b */
    private final Context f407b;

    /* JADX INFO: renamed from: c */
    private final SurfaceView f408c;

    /* JADX INFO: renamed from: d */
    private final SurfaceHolder f409d;

    /* JADX INFO: renamed from: e */
    private final String f410e;

    /* JADX INFO: renamed from: f */
    private final int f411f;

    /* JADX INFO: renamed from: g */
    private final int f412g;

    /* JADX INFO: renamed from: h */
    private final boolean f413h;

    /* JADX INFO: renamed from: i */
    private final long f414i;

    /* JADX INFO: renamed from: j */
    private final long f415j;

    /* JADX INFO: renamed from: k */
    private final FrameLayout f416k;

    /* JADX INFO: renamed from: l */
    private final Display f417l;

    /* JADX INFO: renamed from: m */
    private int f418m;

    /* JADX INFO: renamed from: n */
    private int f419n;

    /* JADX INFO: renamed from: o */
    private int f420o;

    /* JADX INFO: renamed from: p */
    private int f421p;

    /* JADX INFO: renamed from: q */
    private MediaPlayer f422q;

    /* JADX INFO: renamed from: r */
    private MediaController f423r;

    /* JADX INFO: renamed from: s */
    private boolean f424s;

    /* JADX INFO: renamed from: t */
    private boolean f425t;

    /* JADX INFO: renamed from: u */
    private int f426u;

    /* JADX INFO: renamed from: v */
    private boolean f427v;

    /* JADX INFO: renamed from: w */
    private boolean f428w;

    /* JADX INFO: renamed from: x */
    private a f429x;

    /* JADX INFO: renamed from: y */
    private b f430y;

    /* JADX INFO: renamed from: z */
    private volatile int f431z;

    /* JADX INFO: renamed from: com.unity3d.player.n$a */
    public interface a {
        /* JADX INFO: renamed from: a */
        void mo411a(int i);
    }

    /* JADX INFO: renamed from: com.unity3d.player.n$b */
    public class b implements Runnable {

        /* JADX INFO: renamed from: b */
        private SurfaceHolderCallbackC0923n f433b;

        /* JADX INFO: renamed from: c */
        private boolean f434c = false;

        public b(SurfaceHolderCallbackC0923n surfaceHolderCallbackC0923n) {
            this.f433b = surfaceHolderCallbackC0923n;
        }

        /* JADX INFO: renamed from: a */
        public final void m412a() {
            this.f434c = true;
        }

        @Override // java.lang.Runnable
        public final void run() {
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException unused) {
                Thread.currentThread().interrupt();
            }
            if (this.f434c) {
                return;
            }
            if (SurfaceHolderCallbackC0923n.f406a) {
                SurfaceHolderCallbackC0923n.m406b("Stopping the video player due to timeout.");
            }
            this.f433b.CancelOnPrepare();
        }
    }

    protected SurfaceHolderCallbackC0923n(Context context, String str, int i, int i2, int i3, boolean z, long j, long j2, a aVar) {
        super(context);
        this.f424s = false;
        this.f425t = false;
        this.f426u = 0;
        this.f427v = false;
        this.f428w = false;
        this.f431z = 0;
        this.f429x = aVar;
        this.f407b = context;
        this.f416k = this;
        SurfaceView surfaceView = new SurfaceView(context);
        this.f408c = surfaceView;
        SurfaceHolder holder = surfaceView.getHolder();
        this.f409d = holder;
        holder.addCallback(this);
        this.f416k.setBackgroundColor(i);
        this.f416k.addView(this.f408c);
        this.f417l = ((WindowManager) this.f407b.getSystemService("window")).getDefaultDisplay();
        this.f410e = str;
        this.f411f = i2;
        this.f412g = i3;
        this.f413h = z;
        this.f414i = j;
        this.f415j = j2;
        if (f406a) {
            m406b("fileName: " + this.f410e);
        }
        if (f406a) {
            m406b("backgroundColor: " + i);
        }
        if (f406a) {
            m406b("controlMode: " + this.f411f);
        }
        if (f406a) {
            m406b("scalingMode: " + this.f412g);
        }
        if (f406a) {
            m406b("isURL: " + this.f413h);
        }
        if (f406a) {
            m406b("videoOffset: " + this.f414i);
        }
        if (f406a) {
            m406b("videoLength: " + this.f415j);
        }
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /* JADX INFO: renamed from: a */
    private void m404a(int i) {
        this.f431z = i;
        a aVar = this.f429x;
        if (aVar != null) {
            aVar.mo411a(this.f431z);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: b */
    public static void m406b(String str) {
        Log.i("Video", "VideoPlayer: " + str);
    }

    /* JADX INFO: renamed from: c */
    private void m408c() {
        FileInputStream fileInputStream = null;
        MediaPlayer mediaPlayer = this.f422q;
        if (mediaPlayer != null) {
            mediaPlayer.setDisplay(this.f409d);
            if (this.f427v) {
                return;
            }
            if (f406a) {
                m406b("Resuming playback");
            }
            this.f422q.start();
            return;
        }
        m404a(0);
        doCleanUp();
        try {
            MediaPlayer mediaPlayer2 = new MediaPlayer();
            this.f422q = mediaPlayer2;
            if (this.f413h) {
                mediaPlayer2.setDataSource(this.f407b, Uri.parse(this.f410e));
            } else {
                if (this.f415j != 0) {
                    fileInputStream = new FileInputStream(this.f410e);
                    this.f422q.setDataSource(fileInputStream.getFD(), this.f414i, this.f415j);
                } else {
                    try {
                        AssetFileDescriptor assetFileDescriptorOpenFd = getResources().getAssets().openFd(this.f410e);
                        this.f422q.setDataSource(assetFileDescriptorOpenFd.getFileDescriptor(), assetFileDescriptorOpenFd.getStartOffset(), assetFileDescriptorOpenFd.getLength());
                        assetFileDescriptorOpenFd.close();
                    } catch (IOException unused) {
                        fileInputStream = new FileInputStream(this.f410e);
                        this.f422q.setDataSource(fileInputStream.getFD());
                        fileInputStream.close();
                    }
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            }
            this.f422q.setDisplay(this.f409d);
            this.f422q.setScreenOnWhilePlaying(true);
            this.f422q.setOnBufferingUpdateListener(this);
            this.f422q.setOnCompletionListener(this);
            this.f422q.setOnPreparedListener(this);
            this.f422q.setOnVideoSizeChangedListener(this);
            this.f422q.setAudioStreamType(3);
            this.f422q.prepareAsync();
            this.f430y = new b(this);
            new Thread(this.f430y).start();
        } catch (Exception e) {
            if (f406a) {
                m406b("error: " + e.getMessage() + e);
            }
            m404a(2);
        }
    }

    /* JADX INFO: renamed from: d */
    private void m409d() {
        if (isPlaying()) {
            return;
        }
        m404a(1);
        if (f406a) {
            m406b("startVideoPlayback");
        }
        updateVideoLayout();
        if (this.f427v) {
            return;
        }
        start();
    }

    public final void CancelOnPrepare() {
        m404a(2);
    }

    /* JADX INFO: renamed from: a */
    final boolean m410a() {
        return this.f427v;
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final boolean canPause() {
        return true;
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final boolean canSeekBackward() {
        return true;
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final boolean canSeekForward() {
        return true;
    }

    protected final void destroyPlayer() {
        if (f406a) {
            m406b("destroyPlayer");
        }
        if (!this.f427v) {
            pause();
        }
        doCleanUp();
    }

    protected final void doCleanUp() {
        b bVar = this.f430y;
        if (bVar != null) {
            bVar.m412a();
            this.f430y = null;
        }
        MediaPlayer mediaPlayer = this.f422q;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            this.f422q = null;
        }
        this.f420o = 0;
        this.f421p = 0;
        this.f425t = false;
        this.f424s = false;
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final int getAudioSessionId() {
        MediaPlayer mediaPlayer = this.f422q;
        if (mediaPlayer == null) {
            return 0;
        }
        return mediaPlayer.getAudioSessionId();
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final int getBufferPercentage() {
        if (this.f413h) {
            return this.f426u;
        }
        return 100;
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final int getCurrentPosition() {
        MediaPlayer mediaPlayer = this.f422q;
        if (mediaPlayer == null) {
            return 0;
        }
        return mediaPlayer.getCurrentPosition();
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final int getDuration() {
        MediaPlayer mediaPlayer = this.f422q;
        if (mediaPlayer == null) {
            return 0;
        }
        return mediaPlayer.getDuration();
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final boolean isPlaying() {
        boolean z = this.f425t && this.f424s;
        MediaPlayer mediaPlayer = this.f422q;
        return mediaPlayer == null ? !z : mediaPlayer.isPlaying() || !z;
    }

    @Override // android.media.MediaPlayer.OnBufferingUpdateListener
    public final void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        if (f406a) {
            m406b("onBufferingUpdate percent:" + i);
        }
        this.f426u = i;
    }

    @Override // android.media.MediaPlayer.OnCompletionListener
    public final void onCompletion(MediaPlayer mediaPlayer) {
        if (f406a) {
            m406b("onCompletion called");
        }
        destroyPlayer();
        m404a(3);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public final boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i != 4 && (this.f411f != 2 || i == 0 || keyEvent.isSystem())) {
            MediaController mediaController = this.f423r;
            return mediaController != null ? mediaController.onKeyDown(i, keyEvent) : super.onKeyDown(i, keyEvent);
        }
        destroyPlayer();
        m404a(3);
        return true;
    }

    @Override // android.media.MediaPlayer.OnPreparedListener
    public final void onPrepared(MediaPlayer mediaPlayer) {
        if (f406a) {
            m406b("onPrepared called");
        }
        b bVar = this.f430y;
        if (bVar != null) {
            bVar.m412a();
            this.f430y = null;
        }
        int i = this.f411f;
        if (i == 0 || i == 1) {
            MediaController mediaController = new MediaController(this.f407b);
            this.f423r = mediaController;
            mediaController.setMediaPlayer(this);
            this.f423r.setAnchorView(this);
            this.f423r.setEnabled(true);
            Context context = this.f407b;
            if (context instanceof Activity) {
                this.f423r.setSystemUiVisibility(((Activity) context).getWindow().getDecorView().getSystemUiVisibility());
            }
            this.f423r.show();
        }
        this.f425t = true;
        if (1 == 0 || !this.f424s) {
            return;
        }
        m409d();
    }

    @Override // android.view.View
    public final boolean onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction() & 255;
        if (this.f411f != 2 || action != 0) {
            MediaController mediaController = this.f423r;
            return mediaController != null ? mediaController.onTouchEvent(motionEvent) : super.onTouchEvent(motionEvent);
        }
        destroyPlayer();
        m404a(3);
        return true;
    }

    @Override // android.media.MediaPlayer.OnVideoSizeChangedListener
    public final void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i2) {
        if (f406a) {
            m406b("onVideoSizeChanged called " + i + "x" + i2);
        }
        if (i != 0 && i2 != 0) {
            this.f424s = true;
            this.f420o = i;
            this.f421p = i2;
            if (!this.f425t || 1 == 0) {
                return;
            }
            m409d();
            return;
        }
        if (f406a) {
            m406b("invalid video width(" + i + ") or height(" + i2 + ")");
        }
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final void pause() {
        MediaPlayer mediaPlayer = this.f422q;
        if (mediaPlayer == null) {
            return;
        }
        if (this.f428w) {
            mediaPlayer.pause();
        }
        this.f427v = true;
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final void seekTo(int i) {
        MediaPlayer mediaPlayer = this.f422q;
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.seekTo(i);
    }

    @Override // android.widget.MediaController.MediaPlayerControl
    public final void start() {
        if (f406a) {
            m406b("Start");
        }
        MediaPlayer mediaPlayer = this.f422q;
        if (mediaPlayer == null) {
            return;
        }
        if (this.f428w) {
            mediaPlayer.start();
        }
        this.f427v = false;
    }

    @Override // android.view.SurfaceHolder.Callback
    public final void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        if (f406a) {
            m406b("surfaceChanged called " + i + " " + i2 + "x" + i3);
        }
        if (this.f418m == i2 && this.f419n == i3) {
            return;
        }
        this.f418m = i2;
        this.f419n = i3;
        if (this.f428w) {
            updateVideoLayout();
        }
    }

    @Override // android.view.SurfaceHolder.Callback
    public final void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (f406a) {
            m406b("surfaceCreated called");
        }
        this.f428w = true;
        m408c();
    }

    @Override // android.view.SurfaceHolder.Callback
    public final void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (f406a) {
            m406b("surfaceDestroyed called");
        }
        this.f428w = false;
    }

    /* JADX WARN: Removed duplicated region for block: B:19:0x004f  */
    /* JADX WARN: Removed duplicated region for block: B:20:0x0053  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected final void updateVideoLayout() {
        if (f406a) {
            m406b("updateVideoLayout");
        }
        if (this.f422q == null) {
            return;
        }
        if (this.f418m == 0 || this.f419n == 0) {
            WindowManager windowManager = (WindowManager) this.f407b.getSystemService("window");
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            this.f418m = displayMetrics.widthPixels;
            this.f419n = displayMetrics.heightPixels;
        }
        int i = this.f418m;
        int i2 = this.f419n;
        if (this.f424s) {
            int i3 = this.f420o;
            int i4 = this.f421p;
            float f = i3 / i4;
            float f2 = i / i2;
            int i5 = this.f412g;
            if (i5 == 1) {
                if (f2 <= f) {
                    i2 = (int) (i / f);
                } else {
                    i = (int) (i2 * f);
                }
            } else if (i5 == 2) {
                if (f2 >= f) {
                }
            } else if (i5 == 0) {
                i = i3;
                i2 = i4;
            }
        } else if (f406a) {
            m406b("updateVideoLayout: Video size is not known yet");
        }
        if (this.f418m == i && this.f419n == i2) {
            return;
        }
        if (f406a) {
            m406b("frameWidth = " + i + "; frameHeight = " + i2);
        }
        this.f416k.updateViewLayout(this.f408c, new FrameLayout.LayoutParams(i, i2, 17));
    }
}
