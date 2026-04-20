package com.unity3d.player;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.unity3d.player.C0919j;
import com.unity3d.player.C0924o;
import com.unity3d.player.UnityPermissions;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes2.dex */
public class UnityPlayer extends FrameLayout implements IUnityPlayerLifecycleEvents {
    private static final int ANR_TIMEOUT_SECONDS = 4;
    private static final String ARCORE_ENABLE_METADATA_NAME = "unity.arcore-enable";
    private static final String LAUNCH_FULLSCREEN = "unity.launch-fullscreen";
    private static final int RUN_STATE_CHANGED_MSG_CODE = 2269;
    private static final String SPLASH_ENABLE_METADATA_NAME = "unity.splash-enable";
    private static final String SPLASH_MODE_METADATA_NAME = "unity.splash-mode";
    private static final AtomicInteger sRenderedFrameCounter = new AtomicInteger(0);
    public static Activity currentActivity;
    private Activity mActivity;
    private Context mContext;
    private SurfaceView mGlView;
    Handler mHandler;
    private int mInitialScreenOrientation;
    private boolean mIsFullscreen;
    private BroadcastReceiver mKillingIsMyBusiness;
    private boolean mMainDisplayOverride;
    private int mNaturalOrientation;
    private OrientationEventListener mOrientationListener;
    private boolean mProcessKillRequested;
    private boolean mQuitting;
    DialogC0918i mSoftInputDialog;
    private C0922m mState;
    private C0924o mVideoPlayerProxy;
    private GoogleARCoreApi m_ARCoreApi;
    private boolean m_AddPhoneCallListener;
    private AudioVolumeHandler m_AudioVolumeHandler;
    private Camera2Wrapper m_Camera2Wrapper;
    private ClipboardManager m_ClipboardManager;
    private final ConcurrentLinkedQueue m_Events;
    private C0904a m_FakeListener;
    private HFPStatus m_HFPStatus;
    C0908e m_MainThread;
    private NetworkConnectivity m_NetworkConnectivity;
    private OrientationLockListener m_OrientationLockListener;
    private C0917h m_PersistentUnitySurface;
    private C0906c m_PhoneCallListener;
    private C0919j m_SplashScreen;
    private TelephonyManager m_TelephonyManager;
    private IUnityPlayerLifecycleEvents m_UnityPlayerLifecycleEvents;
    private Uri m_launchUri;

    /* JADX INFO: renamed from: com.unity3d.player.UnityPlayer$a */
    class C0904a implements SensorEventListener {
        C0904a() {
        }

        @Override // android.hardware.SensorEventListener
        public final void onAccuracyChanged(Sensor sensor, int i) {
        }

        @Override // android.hardware.SensorEventListener
        public final void onSensorChanged(SensorEvent sensorEvent) {
        }
    }

    /* JADX WARN: $VALUES field not found */
    /* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
    /* JADX INFO: renamed from: com.unity3d.player.UnityPlayer$b */
    static final class EnumC0905b {

        /* JADX INFO: renamed from: a */
        public static final int f253a = 1;

        /* JADX INFO: renamed from: b */
        public static final int f254b = 2;

        /* JADX INFO: renamed from: c */
        public static final int f255c = 3;

        /* JADX INFO: renamed from: d */
        private static final /* synthetic */ int[] f256d = {1, 2, 3};
    }

    /* JADX INFO: renamed from: com.unity3d.player.UnityPlayer$c */
    private class C0906c extends PhoneStateListener {
        private C0906c() {
        }

        /* synthetic */ C0906c(UnityPlayer unityPlayer, byte b) {
            this();
        }

        @Override // android.telephony.PhoneStateListener
        public final void onCallStateChanged(int i, String str) {
            UnityPlayer.this.nativeMuteMasterAudio(i == 1);
        }
    }

    /* JADX INFO: renamed from: com.unity3d.player.UnityPlayer$d */
    enum EnumC0907d {
        PAUSE,
        RESUME,
        QUIT,
        SURFACE_LOST,
        SURFACE_ACQUIRED,
        FOCUS_LOST,
        FOCUS_GAINED,
        NEXT_FRAME,
        URL_ACTIVATED,
        ORIENTATION_ANGLE_CHANGE
    }

    /* JADX INFO: renamed from: com.unity3d.player.UnityPlayer$e */
    private class C0908e extends Thread {

        /* JADX INFO: renamed from: a */
        Handler f269a;

        /* JADX INFO: renamed from: b */
        boolean f270b;

        /* JADX INFO: renamed from: c */
        boolean f271c;

        /* JADX INFO: renamed from: d */
        int f272d;

        /* JADX INFO: renamed from: e */
        int f273e;

        /* JADX INFO: renamed from: f */
        int f274f;

        /* JADX INFO: renamed from: g */
        int f275g;

        /* JADX INFO: renamed from: h */
        int f276h;

        private C0908e() {
            this.f270b = false;
            this.f271c = false;
            this.f272d = EnumC0905b.f254b;
            this.f273e = 0;
            this.f276h = 5;
        }

        /* synthetic */ C0908e(UnityPlayer unityPlayer, byte b) {
            this();
        }

        /* JADX INFO: renamed from: a */
        private void m290a(EnumC0907d enumC0907d) {
            Handler handler = this.f269a;
            if (handler != null) {
                Message.obtain(handler, UnityPlayer.RUN_STATE_CHANGED_MSG_CODE, enumC0907d).sendToTarget();
            }
        }

        /* JADX INFO: renamed from: a */
        public final void m291a() {
            m290a(EnumC0907d.QUIT);
        }

        /* JADX INFO: renamed from: a */
        public final void m292a(int i, int i2) {
            this.f274f = i;
            this.f275g = i2;
            m290a(EnumC0907d.ORIENTATION_ANGLE_CHANGE);
        }

        /* JADX INFO: renamed from: a */
        public final void m293a(Runnable runnable) {
            if (this.f269a == null) {
                return;
            }
            m290a(EnumC0907d.PAUSE);
            Message.obtain(this.f269a, runnable).sendToTarget();
        }

        /* JADX INFO: renamed from: b */
        public final void m294b() {
            m290a(EnumC0907d.RESUME);
        }

        /* JADX INFO: renamed from: b */
        public final void m295b(Runnable runnable) {
            if (this.f269a == null) {
                return;
            }
            m290a(EnumC0907d.SURFACE_LOST);
            Message.obtain(this.f269a, runnable).sendToTarget();
        }

        /* JADX INFO: renamed from: c */
        public final void m296c() {
            m290a(EnumC0907d.FOCUS_GAINED);
        }

        /* JADX INFO: renamed from: c */
        public final void m297c(Runnable runnable) {
            Handler handler = this.f269a;
            if (handler == null) {
                return;
            }
            Message.obtain(handler, runnable).sendToTarget();
            m290a(EnumC0907d.SURFACE_ACQUIRED);
        }

        /* JADX INFO: renamed from: d */
        public final void m298d() {
            m290a(EnumC0907d.FOCUS_LOST);
        }

        /* JADX INFO: renamed from: d */
        public final void m299d(Runnable runnable) {
            Handler handler = this.f269a;
            if (handler != null) {
                Message.obtain(handler, runnable).sendToTarget();
            }
        }

        /* JADX INFO: renamed from: e */
        public final void m300e() {
            m290a(EnumC0907d.URL_ACTIVATED);
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public final void run() {
            setName("UnityMain");
            Looper.prepare();
            this.f269a = new Handler(new Handler.Callback() { // from class: com.unity3d.player.UnityPlayer.e.1
                /* JADX INFO: renamed from: a */
                private void m301a() {
                    if (C0908e.this.f272d == EnumC0905b.f255c && C0908e.this.f271c) {
                        UnityPlayer.this.nativeFocusChanged(true);
                        C0908e.this.f272d = EnumC0905b.f253a;
                    }
                }

                @Override // android.os.Handler.Callback
                public final boolean handleMessage(Message message) {
                    if (message.what != UnityPlayer.RUN_STATE_CHANGED_MSG_CODE) {
                        return false;
                    }
                    EnumC0907d enumC0907d = (EnumC0907d) message.obj;
                    if (enumC0907d == EnumC0907d.NEXT_FRAME) {
                        C0908e.this.f273e--;
                        UnityPlayer.this.executeGLThreadJobs();
                        if (!C0908e.this.f270b || !C0908e.this.f271c) {
                            return true;
                        }
                        if (C0908e.this.f276h >= 0) {
                            if (C0908e.this.f276h == 0 && UnityPlayer.this.getSplashEnabled()) {
                                UnityPlayer.this.DisableStaticSplashScreen();
                            }
                            C0908e.this.f276h--;
                        }
                        if (!UnityPlayer.this.isFinishing()) {
                            if (!UnityPlayer.this.nativeRender()) {
                                UnityPlayer.this.finish();
                            } else {
                                UnityPlayer.sRenderedFrameCounter.incrementAndGet();
                            }
                        }
                    } else if (enumC0907d == EnumC0907d.QUIT) {
                        Looper.myLooper().quit();
                    } else if (enumC0907d == EnumC0907d.RESUME) {
                        C0908e.this.f270b = true;
                    } else if (enumC0907d == EnumC0907d.PAUSE) {
                        C0908e.this.f270b = false;
                    } else if (enumC0907d == EnumC0907d.SURFACE_LOST) {
                        C0908e.this.f271c = false;
                    } else {
                        if (enumC0907d == EnumC0907d.SURFACE_ACQUIRED) {
                            C0908e.this.f271c = true;
                        } else if (enumC0907d == EnumC0907d.FOCUS_LOST) {
                            if (C0908e.this.f272d == EnumC0905b.f253a) {
                                UnityPlayer.this.nativeFocusChanged(false);
                            }
                            C0908e.this.f272d = EnumC0905b.f254b;
                        } else if (enumC0907d == EnumC0907d.FOCUS_GAINED) {
                            C0908e.this.f272d = EnumC0905b.f255c;
                        } else if (enumC0907d == EnumC0907d.URL_ACTIVATED) {
                            UnityPlayer.this.nativeSetLaunchURL(UnityPlayer.this.getLaunchURL());
                        } else if (enumC0907d == EnumC0907d.ORIENTATION_ANGLE_CHANGE) {
                            UnityPlayer.this.nativeOrientationChanged(C0908e.this.f274f, C0908e.this.f275g);
                        }
                        m301a();
                    }
                    if (C0908e.this.f270b && C0908e.this.f273e <= 0) {
                        Message.obtain(C0908e.this.f269a, UnityPlayer.RUN_STATE_CHANGED_MSG_CODE, EnumC0907d.NEXT_FRAME).sendToTarget();
                        C0908e.this.f273e++;
                    }
                    return true;
                }
            });
            Looper.loop();
        }
    }

    /* JADX INFO: renamed from: com.unity3d.player.UnityPlayer$f */
    private abstract class AbstractRunnableC0909f implements Runnable {
        private AbstractRunnableC0909f() {
        }

        /* synthetic */ AbstractRunnableC0909f(UnityPlayer unityPlayer, byte b) {
            this();
        }

        /* JADX INFO: renamed from: a */
        public abstract void mo288a();

        @Override // java.lang.Runnable
        public final void run() {
            if (UnityPlayer.this.isFinishing()) {
                return;
            }
            mo288a();
        }
    }

    static {
        new C0921l().m392a();
    }

    public UnityPlayer(Context context) {
        this(context, null);
    }

    public UnityPlayer(Context context, IUnityPlayerLifecycleEvents iUnityPlayerLifecycleEvents) {
        super(context);
        this.mHandler = new Handler();
        this.mInitialScreenOrientation = -1;
        byte b = 0;
        this.mMainDisplayOverride = false;
        this.mIsFullscreen = true;
        this.mState = new C0922m();
        this.m_Events = new ConcurrentLinkedQueue();
        this.mKillingIsMyBusiness = null;
        this.mOrientationListener = null;
        this.m_MainThread = new C0908e(this, b);
        this.m_AddPhoneCallListener = false;
        this.m_PhoneCallListener = new C0906c(this, b);
        this.m_ARCoreApi = null;
        this.m_FakeListener = new C0904a();
        this.m_Camera2Wrapper = null;
        this.m_HFPStatus = null;
        this.m_AudioVolumeHandler = null;
        this.m_OrientationLockListener = null;
        this.m_launchUri = null;
        this.m_NetworkConnectivity = null;
        this.m_UnityPlayerLifecycleEvents = null;
        this.mProcessKillRequested = true;
        this.mSoftInputDialog = null;
        this.m_UnityPlayerLifecycleEvents = iUnityPlayerLifecycleEvents == null ? this : iUnityPlayerLifecycleEvents;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            this.mActivity = activity;
            currentActivity = activity;
            this.mInitialScreenOrientation = activity.getRequestedOrientation();
            this.m_launchUri = this.mActivity.getIntent().getData();
        }
        this.mContext = context;
        EarlyEnableFullScreenIfEnabled();
        this.mNaturalOrientation = getNaturalOrientation(getResources().getConfiguration().orientation);
        if (this.mActivity != null && getSplashEnabled()) {
            C0919j c0919j = new C0919j(this.mContext, C0919j.a.m389a()[getSplashMode()]);
            this.m_SplashScreen = c0919j;
            addView(c0919j);
        }
        if (currentActivity != null) {
            this.m_PersistentUnitySurface = new C0917h(this.mContext);
        }
        preloadJavaPlugins();
        String strLoadNative = loadNative(getUnityNativeLibraryPath(this.mContext));
        if (!C0922m.m395c()) {
            C0915f.Log(6, "Your hardware does not support this application.");
            AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setTitle("Failure to initialize!").setPositiveButton("OK", new DialogInterface.OnClickListener() { // from class: com.unity3d.player.UnityPlayer.1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    UnityPlayer.this.finish();
                }
            }).setMessage("Your hardware does not support this application.\n\n" + strLoadNative + "\n\n Press OK to quit.").create();
            alertDialogCreate.setCancelable(false);
            alertDialogCreate.show();
            return;
        }
        initJni(context);
        this.mState.m398c(true);
        SurfaceView surfaceViewCreateGlView = CreateGlView();
        this.mGlView = surfaceViewCreateGlView;
        surfaceViewCreateGlView.setContentDescription(GetGlViewContentDescription(context));
        addView(this.mGlView);
        if (this.m_SplashScreen != null) {
            bringChildToFront(this.m_SplashScreen);
        }
        this.mQuitting = false;
        hideStatusBar();
        this.m_TelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.m_ClipboardManager = (ClipboardManager) this.mContext.getSystemService("clipboard");
        this.m_Camera2Wrapper = new Camera2Wrapper(this.mContext);
        this.m_HFPStatus = new HFPStatus(this.mContext);
        this.m_MainThread.start();
    }

    private SurfaceView CreateGlView() {
        SurfaceView surfaceView = new SurfaceView(this.mContext);
        surfaceView.setId(this.mContext.getResources().getIdentifier("unitySurfaceView", "id", this.mContext.getPackageName()));
        if (IsWindowTranslucent()) {
            surfaceView.getHolder().setFormat(-3);
            surfaceView.setZOrderOnTop(true);
        } else {
            surfaceView.getHolder().setFormat(-1);
        }
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() { // from class: com.unity3d.player.UnityPlayer.19
            @Override // android.view.SurfaceHolder.Callback
            public final void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
                UnityPlayer.this.updateGLDisplay(0, surfaceHolder.getSurface());
                UnityPlayer.this.sendSurfaceChangedEvent();
            }

            @Override // android.view.SurfaceHolder.Callback
            public final void surfaceCreated(SurfaceHolder surfaceHolder) {
                UnityPlayer.this.updateGLDisplay(0, surfaceHolder.getSurface());
                if (UnityPlayer.this.m_PersistentUnitySurface != null) {
                    UnityPlayer.this.m_PersistentUnitySurface.m370a(UnityPlayer.this);
                }
            }

            @Override // android.view.SurfaceHolder.Callback
            public final void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (UnityPlayer.this.m_PersistentUnitySurface != null) {
                    UnityPlayer.this.m_PersistentUnitySurface.m369a(UnityPlayer.this.mGlView);
                }
                UnityPlayer.this.updateGLDisplay(0, null);
            }
        });
        surfaceView.setFocusable(true);
        surfaceView.setFocusableInTouchMode(true);
        return surfaceView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void DisableStaticSplashScreen() {
        runOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.18
            @Override // java.lang.Runnable
            public final void run() {
                UnityPlayer unityPlayer = UnityPlayer.this;
                unityPlayer.removeView(unityPlayer.m_SplashScreen);
                UnityPlayer.this.m_SplashScreen = null;
            }
        });
    }

    private void EarlyEnableFullScreenIfEnabled() {
        View decorView;
        Activity activity = this.mActivity;
        if (activity == null || activity.getWindow() == null) {
            return;
        }
        if ((getLaunchFullscreen() || this.mActivity.getIntent().getBooleanExtra("android.intent.extra.VR_LAUNCH", false)) && (decorView = this.mActivity.getWindow().getDecorView()) != null) {
            decorView.setSystemUiVisibility(7);
        }
    }

    private String GetGlViewContentDescription(Context context) {
        return context.getResources().getString(context.getResources().getIdentifier("game_view_content_description", "string", context.getPackageName()));
    }

    private boolean IsWindowTranslucent() {
        Activity activity = this.mActivity;
        if (activity == null) {
            return false;
        }
        TypedArray typedArrayObtainStyledAttributes = activity.getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowIsTranslucent});
        boolean z = typedArrayObtainStyledAttributes.getBoolean(0, false);
        typedArrayObtainStyledAttributes.recycle();
        return z;
    }

    public static void UnitySendMessage(String str, String str2, String str3) {
        if (C0922m.m395c()) {
            try {
                nativeUnitySendMessage(str, str2, str3.getBytes("UTF-8"));
                return;
            } catch (UnsupportedEncodingException unused) {
                return;
            }
        }
        C0915f.Log(5, "Native libraries not loaded - dropping message for " + str + "." + str2);
    }

    private void checkResumePlayer() {
        Activity activity = this.mActivity;
        if (this.mState.m402e(activity != null ? MultiWindowSupport.getAllowResizableWindow(activity) : false)) {
            this.mState.m399d(true);
            queueGLThreadEvent(new Runnable() { // from class: com.unity3d.player.UnityPlayer.3
                @Override // java.lang.Runnable
                public final void run() {
                    UnityPlayer.this.nativeResume();
                    UnityPlayer.this.runOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.3.1
                        @Override // java.lang.Runnable
                        public final void run() {
                            if (UnityPlayer.this.m_PersistentUnitySurface != null) {
                                UnityPlayer.this.m_PersistentUnitySurface.m371b(UnityPlayer.this);
                            }
                        }
                    });
                }
            });
            this.m_MainThread.m294b();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finish() {
        Activity activity = this.mActivity;
        if (activity == null || activity.isFinishing()) {
            return;
        }
        this.mActivity.finish();
    }

    private boolean getARCoreEnabled() {
        try {
            return getApplicationInfo().metaData.getBoolean(ARCORE_ENABLE_METADATA_NAME);
        } catch (Exception unused) {
            return false;
        }
    }

    private ApplicationInfo getApplicationInfo() {
        try {
            return this.mContext.getPackageManager().getApplicationInfo(this.mContext.getPackageName(), 128);
        } catch (Exception unused) {
            return this.mContext.getApplicationInfo();
        }
    }

    private boolean getLaunchFullscreen() {
        try {
            return getApplicationInfo().metaData.getBoolean(LAUNCH_FULLSCREEN);
        } catch (Exception unused) {
            return false;
        }
    }

    private int getNaturalOrientation(int i) {
        int rotation = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRotation();
        if ((rotation == 0 || rotation == 2) && i == 2) {
            return 0;
        }
        return ((rotation == 1 || rotation == 3) && i == 1) ? 0 : 1;
    }

    private String getProcessName() {
        int iMyPid = Process.myPid();
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (runningAppProcesses == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            if (runningAppProcessInfo.pid == iMyPid) {
                return runningAppProcessInfo.processName;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getSplashEnabled() {
        return false;
    }

    private static String getUnityNativeLibraryPath(Context context) {
        return context.getApplicationInfo().nativeLibraryDir;
    }

    private void hideStatusBar() {
        Activity activity = this.mActivity;
        if (activity != null) {
            activity.getWindow().setFlags(1024, 1024);
        }
    }

    private final native void initJni(Context context);

    private static String loadNative(String str) {
        String str2 = str + "/libmain.so";
        try {
            try {
                try {
                    System.load(str2);
                } catch (SecurityException e) {
                    return logLoadLibMainError(str2, e.toString());
                }
            } catch (UnsatisfiedLinkError e2) {
                return logLoadLibMainError(str2, e2.toString());
            }
        } catch (UnsatisfiedLinkError unused) {
            System.loadLibrary("main");
        }
        if (NativeLoader.load(str)) {
            C0922m.m393a();
            return "";
        }
        C0915f.Log(6, "NativeLoader.load failure, Unity libraries were not loaded.");
        return "NativeLoader.load failure, Unity libraries were not loaded.";
    }

    private static String logLoadLibMainError(String str, String str2) {
        String str3 = "Failed to load 'libmain.so'\n\n" + str2;
        C0915f.Log(6, str3);
        return str3;
    }

    public static int consumeRenderedFrameCount() {
        return sRenderedFrameCounter.getAndSet(0);
    }

    private final native void nativeApplicationUnload();

    private final native boolean nativeDone();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeFocusChanged(boolean z);

    private final native boolean nativeInjectEvent(InputEvent inputEvent);

    /* JADX INFO: Access modifiers changed from: private */
    public final native boolean nativeIsAutorotationOn();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeLowMemory();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeMuteMasterAudio(boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeOrientationChanged(int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public final native boolean nativePause();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeRecreateGfxState(int i, Surface surface);

    /* JADX INFO: Access modifiers changed from: private */
    public final native boolean nativeRender();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeReportKeyboardConfigChanged();

    private final native void nativeRestartActivityIndicator();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeResume();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeSendSurfaceChangedEvent();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeSetInputArea(int i, int i2, int i3, int i4);

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeSetInputSelection(int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeSetInputString(String str);

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeSetKeyboardIsVisible(boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeSetLaunchURL(String str);

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeSoftInputCanceled();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeSoftInputClosed();

    /* JADX INFO: Access modifiers changed from: private */
    public final native void nativeSoftInputLostFocus();

    private static native void nativeUnitySendMessage(String str, String str2, byte[] bArr);

    private void pauseUnity() {
        reportSoftInputStr(null, 1, true);
        if (this.mState.m403f()) {
            if (C0922m.m395c()) {
                final Semaphore semaphore = new Semaphore(0);
                this.m_MainThread.m293a(isFinishing() ? new Runnable() { // from class: com.unity3d.player.UnityPlayer.23
                    @Override // java.lang.Runnable
                    public final void run() {
                        UnityPlayer.this.shutdown();
                        semaphore.release();
                    }
                } : new Runnable() { // from class: com.unity3d.player.UnityPlayer.24
                    @Override // java.lang.Runnable
                    public final void run() {
                        if (!UnityPlayer.this.nativePause()) {
                            semaphore.release();
                            return;
                        }
                        UnityPlayer.this.mQuitting = true;
                        UnityPlayer.this.shutdown();
                        semaphore.release(2);
                    }
                });
                try {
                    if (!semaphore.tryAcquire(4L, TimeUnit.SECONDS)) {
                        C0915f.Log(5, "Timeout while trying to pause the Unity Engine.");
                    }
                } catch (InterruptedException unused) {
                    C0915f.Log(5, "UI thread got interrupted while trying to pause the Unity Engine.");
                }
                if (semaphore.drainPermits() > 0) {
                    destroy();
                }
            }
            this.mState.m399d(false);
            this.mState.m397b(true);
            if (this.m_AddPhoneCallListener) {
                this.m_TelephonyManager.listen(this.m_PhoneCallListener, 0);
            }
        }
    }

    private static void preloadJavaPlugins() {
        try {
            Class.forName("com.unity3d.JavaPluginPreloader");
        } catch (ClassNotFoundException unused) {
        } catch (LinkageError e) {
            C0915f.Log(6, "Java class preloading failed: " + e.getMessage());
        }
    }

    private void queueGLThreadEvent(AbstractRunnableC0909f abstractRunnableC0909f) {
        if (isFinishing()) {
            return;
        }
        queueGLThreadEvent((Runnable) abstractRunnableC0909f);
    }

    private void queueGLThreadEvent(Runnable runnable) {
        if (C0922m.m395c()) {
            if (Thread.currentThread() == this.m_MainThread) {
                runnable.run();
            } else {
                this.m_Events.add(runnable);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSurfaceChangedEvent() {
        if (C0922m.m395c() && this.mState.m401e()) {
            this.m_MainThread.m299d(new Runnable() { // from class: com.unity3d.player.UnityPlayer.20
                @Override // java.lang.Runnable
                public final void run() {
                    UnityPlayer.this.nativeSendSurfaceChangedEvent();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void shutdown() {
        this.mProcessKillRequested = nativeDone();
        this.mState.m398c(false);
    }

    private void swapViews(View view, View view2) {
        boolean z;
        if (this.mState.m400d()) {
            z = false;
        } else {
            pause();
            z = true;
        }
        if (view != null) {
            ViewParent parent = view.getParent();
            if (!(parent instanceof UnityPlayer) || ((UnityPlayer) parent) != this) {
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(view);
                }
                addView(view);
                bringChildToFront(view);
                view.setVisibility(0);
            }
        }
        if (view2 != null && view2.getParent() == this) {
            view2.setVisibility(8);
            removeView(view2);
        }
        if (z) {
            resume();
        }
    }

    private static void unloadNative() {
        if (C0922m.m395c()) {
            if (!NativeLoader.unload()) {
                throw new UnsatisfiedLinkError("Unable to unload libraries from libmain.so");
            }
            C0922m.m394b();
        }
    }

    private boolean updateDisplayInternal(final int i, final Surface surface) {
        if (!C0922m.m395c() || !this.mState.m401e()) {
            return false;
        }
        final Semaphore semaphore = new Semaphore(0);
        Runnable runnable = new Runnable() { // from class: com.unity3d.player.UnityPlayer.21
            @Override // java.lang.Runnable
            public final void run() {
                UnityPlayer.this.nativeRecreateGfxState(i, surface);
                semaphore.release();
            }
        };
        if (i == 0) {
            C0908e c0908e = this.m_MainThread;
            if (surface == null) {
                c0908e.m295b(runnable);
            } else {
                c0908e.m297c(runnable);
            }
        } else {
            runnable.run();
        }
        if (surface != null || i != 0) {
            return true;
        }
        try {
            if (semaphore.tryAcquire(4L, TimeUnit.SECONDS)) {
                return true;
            }
            C0915f.Log(5, "Timeout while trying detaching primary window.");
            return true;
        } catch (InterruptedException unused) {
            C0915f.Log(5, "UI thread got interrupted while trying to detach the primary window from the Unity Engine.");
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateGLDisplay(int i, Surface surface) {
        if (this.mMainDisplayOverride) {
            return;
        }
        updateDisplayInternal(i, surface);
    }

    protected void addPhoneCallListener() {
        this.m_AddPhoneCallListener = true;
        this.m_TelephonyManager.listen(this.m_PhoneCallListener, 32);
    }

    public boolean addViewToPlayer(View view, boolean z) {
        swapViews(view, z ? this.mGlView : null);
        boolean z2 = true;
        boolean z3 = view.getParent() == this;
        boolean z4 = z && this.mGlView.getParent() == null;
        boolean z5 = this.mGlView.getParent() == this;
        if (!z3 || (!z4 && !z5)) {
            z2 = false;
        }
        if (!z2) {
            if (!z3) {
                C0915f.Log(6, "addViewToPlayer: Failure adding view to hierarchy");
            }
            if (!z4 && !z5) {
                C0915f.Log(6, "addViewToPlayer: Failure removing old view from hierarchy");
            }
        }
        return z2;
    }

    public void configurationChanged(Configuration configuration) {
        SurfaceView surfaceView = this.mGlView;
        if (surfaceView instanceof SurfaceView) {
            surfaceView.getHolder().setSizeFromLayout();
        }
        C0924o c0924o = this.mVideoPlayerProxy;
        if (c0924o != null) {
            c0924o.m427c();
        }
    }

    public void destroy() {
        C0917h c0917h = this.m_PersistentUnitySurface;
        if (c0917h != null) {
            c0917h.m368a();
            this.m_PersistentUnitySurface = null;
        }
        Camera2Wrapper camera2Wrapper = this.m_Camera2Wrapper;
        if (camera2Wrapper != null) {
            camera2Wrapper.m259a();
            this.m_Camera2Wrapper = null;
        }
        HFPStatus hFPStatus = this.m_HFPStatus;
        if (hFPStatus != null) {
            hFPStatus.m268a();
            this.m_HFPStatus = null;
        }
        NetworkConnectivity networkConnectivity = this.m_NetworkConnectivity;
        if (networkConnectivity != null) {
            networkConnectivity.m271b();
            this.m_NetworkConnectivity = null;
        }
        this.mQuitting = true;
        if (!this.mState.m400d()) {
            pause();
        }
        this.m_MainThread.m291a();
        try {
            this.m_MainThread.join(4000L);
        } catch (InterruptedException unused) {
            this.m_MainThread.interrupt();
        }
        BroadcastReceiver broadcastReceiver = this.mKillingIsMyBusiness;
        if (broadcastReceiver != null) {
            this.mContext.unregisterReceiver(broadcastReceiver);
        }
        this.mKillingIsMyBusiness = null;
        if (C0922m.m395c()) {
            removeAllViews();
        }
        if (this.mProcessKillRequested) {
            this.m_UnityPlayerLifecycleEvents.onUnityPlayerQuitted();
            kill();
        }
        unloadNative();
    }

    protected void disableLogger() {
        C0915f.f356a = true;
    }

    public boolean displayChanged(int i, Surface surface) {
        if (i == 0) {
            this.mMainDisplayOverride = surface != null;
            runOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.22
                @Override // java.lang.Runnable
                public final void run() {
                    if (UnityPlayer.this.mMainDisplayOverride) {
                        UnityPlayer unityPlayer = UnityPlayer.this;
                        unityPlayer.removeView(unityPlayer.mGlView);
                    } else {
                        UnityPlayer unityPlayer2 = UnityPlayer.this;
                        unityPlayer2.addView(unityPlayer2.mGlView);
                    }
                }
            });
        }
        return updateDisplayInternal(i, surface);
    }

    protected void executeGLThreadJobs() {
        while (true) {
            Runnable runnable = (Runnable) this.m_Events.poll();
            if (runnable == null) {
                return;
            } else {
                runnable.run();
            }
        }
    }

    protected String getClipboardText() {
        ClipData primaryClip = this.m_ClipboardManager.getPrimaryClip();
        return primaryClip != null ? primaryClip.getItemAt(0).coerceToText(this.mContext).toString() : "";
    }

    protected String getKeyboardLayout() {
        DialogC0918i dialogC0918i = this.mSoftInputDialog;
        if (dialogC0918i == null) {
            return null;
        }
        return dialogC0918i.m381a();
    }

    protected String getLaunchURL() {
        Uri uri = this.m_launchUri;
        if (uri != null) {
            return uri.toString();
        }
        return null;
    }

    protected int getNetworkConnectivity() {
        if (!PlatformSupport.NOUGAT_SUPPORT) {
            return 0;
        }
        if (this.m_NetworkConnectivity == null) {
            this.m_NetworkConnectivity = new NetworkConnectivity(this.mContext);
        }
        return this.m_NetworkConnectivity.m270a();
    }

    public String getNetworkProxySettings(String str) {
        String str2;
        String str3;
        if (!str.startsWith("http:")) {
            if (str.startsWith("https:")) {
                str2 = "https.proxyHost";
                str3 = "https.proxyPort";
            }
            return null;
        }
        str2 = "http.proxyHost";
        str3 = "http.proxyPort";
        String property = System.getProperties().getProperty(str2);
        if (property != null && !"".equals(property)) {
            StringBuilder sb = new StringBuilder(property);
            String property2 = System.getProperties().getProperty(str3);
            if (property2 != null && !"".equals(property2)) {
                sb.append(":");
                sb.append(property2);
            }
            String property3 = System.getProperties().getProperty("http.nonProxyHosts");
            if (property3 != null && !"".equals(property3)) {
                sb.append('\n');
                sb.append(property3);
            }
            return sb.toString();
        }
        return null;
    }

    public Bundle getSettings() {
        return Bundle.EMPTY;
    }

    protected int getSplashMode() {
        try {
            return getApplicationInfo().metaData.getInt(SPLASH_MODE_METADATA_NAME);
        } catch (Exception unused) {
            return 0;
        }
    }

    protected int getUaaLLaunchProcessType() {
        String processName = getProcessName();
        return (processName == null || processName.equals(this.mContext.getPackageName())) ? 0 : 1;
    }

    public View getView() {
        return this;
    }

    protected void hideSoftInput() {
        postOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.5
            @Override // java.lang.Runnable
            public final void run() {
                UnityPlayer.this.reportSoftInputArea(new Rect());
                UnityPlayer.this.reportSoftInputIsVisible(false);
                if (UnityPlayer.this.mSoftInputDialog != null) {
                    UnityPlayer.this.mSoftInputDialog.dismiss();
                    UnityPlayer.this.mSoftInputDialog = null;
                    UnityPlayer.this.nativeReportKeyboardConfigChanged();
                }
            }
        });
    }

    public void init(int i, boolean z) {
    }

    protected boolean initializeGoogleAr() {
        if (this.m_ARCoreApi != null || this.mActivity == null || !getARCoreEnabled()) {
            return false;
        }
        GoogleARCoreApi googleARCoreApi = new GoogleARCoreApi();
        this.m_ARCoreApi = googleARCoreApi;
        googleARCoreApi.initializeARCore(this.mActivity);
        if (this.mState.m400d()) {
            return false;
        }
        this.m_ARCoreApi.resumeARCore();
        return false;
    }

    public boolean injectEvent(InputEvent inputEvent) {
        if (C0922m.m395c()) {
            return nativeInjectEvent(inputEvent);
        }
        return false;
    }

    protected boolean isFinishing() {
        if (this.mQuitting) {
            return true;
        }
        Activity activity = this.mActivity;
        if (activity != null) {
            this.mQuitting = activity.isFinishing();
        }
        return this.mQuitting;
    }

    protected boolean isUaaLUseCase() {
        String callingPackage;
        Activity activity = this.mActivity;
        return (activity == null || (callingPackage = activity.getCallingPackage()) == null || !callingPackage.equals(this.mContext.getPackageName())) ? false : true;
    }

    protected void kill() {
        Process.killProcess(Process.myPid());
    }

    protected boolean loadLibrary(String str) {
        try {
            System.loadLibrary(str);
            return true;
        } catch (Exception | UnsatisfiedLinkError unused) {
            return false;
        }
    }

    public void lowMemory() {
        if (C0922m.m395c()) {
            queueGLThreadEvent(new Runnable() { // from class: com.unity3d.player.UnityPlayer.2
                @Override // java.lang.Runnable
                public final void run() {
                    UnityPlayer.this.nativeLowMemory();
                }
            });
        }
    }

    public void newIntent(Intent intent) {
        this.m_launchUri = intent.getData();
        this.m_MainThread.m300e();
    }

    @Override // android.view.View
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return injectEvent(motionEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return injectEvent(keyEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        return injectEvent(keyEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyMultiple(int i, int i2, KeyEvent keyEvent) {
        return injectEvent(keyEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return injectEvent(keyEvent);
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return injectEvent(motionEvent);
    }

    @Override // com.unity3d.player.IUnityPlayerLifecycleEvents
    public void onUnityPlayerQuitted() {
    }

    @Override // com.unity3d.player.IUnityPlayerLifecycleEvents
    public void onUnityPlayerUnloaded() {
    }

    public void pause() {
        GoogleARCoreApi googleARCoreApi = this.m_ARCoreApi;
        if (googleARCoreApi != null) {
            googleARCoreApi.pauseARCore();
        }
        C0924o c0924o = this.mVideoPlayerProxy;
        if (c0924o != null) {
            c0924o.m424a();
        }
        AudioVolumeHandler audioVolumeHandler = this.m_AudioVolumeHandler;
        if (audioVolumeHandler != null) {
            audioVolumeHandler.m257a();
            this.m_AudioVolumeHandler = null;
        }
        OrientationLockListener orientationLockListener = this.m_OrientationLockListener;
        if (orientationLockListener != null) {
            orientationLockListener.m272a();
            this.m_OrientationLockListener = null;
        }
        pauseUnity();
    }

    protected void pauseJavaAndCallUnloadCallback() {
        runOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.16
            @Override // java.lang.Runnable
            public final void run() {
                UnityPlayer.this.pause();
                UnityPlayer.this.windowFocusChanged(false);
                UnityPlayer.this.m_UnityPlayerLifecycleEvents.onUnityPlayerUnloaded();
            }
        });
    }

    void postOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public void quit() {
        destroy();
    }

    public void removeViewFromPlayer(View view) {
        swapViews(this.mGlView, view);
        boolean z = view.getParent() == null;
        boolean z2 = this.mGlView.getParent() == this;
        if (z && z2) {
            return;
        }
        if (!z) {
            C0915f.Log(6, "removeViewFromPlayer: Failure removing view from hierarchy");
        }
        if (z2) {
            return;
        }
        C0915f.Log(6, "removeVireFromPlayer: Failure agging old view to hierarchy");
    }

    public void reportError(String str, String str2) {
        C0915f.Log(6, str + ": " + str2);
    }

    protected void reportSoftInputArea(final Rect rect) {
        queueGLThreadEvent(new AbstractRunnableC0909f(UnityPlayer.this, (byte) 0) { // from class: com.unity3d.player.UnityPlayer.12
            @Override // com.unity3d.player.UnityPlayer.AbstractRunnableC0909f
            /* JADX INFO: renamed from: a */
            public final void mo288a() {
                UnityPlayer.this.nativeSetInputArea(rect.left, rect.top, rect.right, rect.bottom);
            }
        });
    }

    protected void reportSoftInputIsVisible(final boolean z) {
        queueGLThreadEvent(new AbstractRunnableC0909f(UnityPlayer.this, (byte) 0) { // from class: com.unity3d.player.UnityPlayer.13
            @Override // com.unity3d.player.UnityPlayer.AbstractRunnableC0909f
            /* JADX INFO: renamed from: a */
            public final void mo288a() {
                UnityPlayer.this.nativeSetKeyboardIsVisible(z);
            }
        });
    }

    protected void reportSoftInputSelection(final int i, final int i2) {
        queueGLThreadEvent(new AbstractRunnableC0909f(UnityPlayer.this, (byte) 0) { // from class: com.unity3d.player.UnityPlayer.11
            @Override // com.unity3d.player.UnityPlayer.AbstractRunnableC0909f
            /* JADX INFO: renamed from: a */
            public final void mo288a() {
                UnityPlayer.this.nativeSetInputSelection(i, i2);
            }
        });
    }

    protected void reportSoftInputStr(final String str, final int i, final boolean z) {
        if (i == 1) {
            hideSoftInput();
        }
        queueGLThreadEvent(new AbstractRunnableC0909f(UnityPlayer.this, (byte) 0) { // from class: com.unity3d.player.UnityPlayer.10
            @Override // com.unity3d.player.UnityPlayer.AbstractRunnableC0909f
            /* JADX INFO: renamed from: a */
            public final void mo288a() {
                if (z) {
                    UnityPlayer.this.nativeSoftInputCanceled();
                } else {
                    String str2 = str;
                    if (str2 != null) {
                        UnityPlayer.this.nativeSetInputString(str2);
                    }
                }
                if (i == 1) {
                    UnityPlayer.this.nativeSoftInputClosed();
                }
            }
        });
    }

    protected void requestUserAuthorization(String str) {
        if (str == null || str.isEmpty() || this.mActivity == null) {
            return;
        }
        UnityPermissions.ModalWaitForPermissionResponse modalWaitForPermissionResponse = new UnityPermissions.ModalWaitForPermissionResponse();
        UnityPermissions.requestUserPermissions(this.mActivity, new String[]{str}, modalWaitForPermissionResponse);
        modalWaitForPermissionResponse.waitForResponse();
    }

    public void resume() {
        GoogleARCoreApi googleARCoreApi = this.m_ARCoreApi;
        if (googleARCoreApi != null) {
            googleARCoreApi.resumeARCore();
        }
        this.mState.m397b(false);
        C0924o c0924o = this.mVideoPlayerProxy;
        if (c0924o != null) {
            c0924o.m426b();
        }
        checkResumePlayer();
        if (C0922m.m395c()) {
            nativeRestartActivityIndicator();
        }
        if (this.m_AudioVolumeHandler == null) {
            this.m_AudioVolumeHandler = new AudioVolumeHandler(this.mContext);
        }
        if (this.m_OrientationLockListener == null && C0922m.m395c()) {
            this.m_OrientationLockListener = new OrientationLockListener(this.mContext);
        }
    }

    void runOnAnonymousThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    void runOnUiThread(Runnable runnable) {
        Activity activity = this.mActivity;
        if (activity != null) {
            activity.runOnUiThread(runnable);
        } else if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            this.mHandler.post(runnable);
        } else {
            runnable.run();
        }
    }

    protected void setCharacterLimit(final int i) {
        runOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.7
            @Override // java.lang.Runnable
            public final void run() {
                if (UnityPlayer.this.mSoftInputDialog != null) {
                    UnityPlayer.this.mSoftInputDialog.m382a(i);
                }
            }
        });
    }

    protected void setClipboardText(String str) {
        this.m_ClipboardManager.setPrimaryClip(ClipData.newPlainText("Text", str));
    }

    protected void setHideInputField(final boolean z) {
        runOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.8
            @Override // java.lang.Runnable
            public final void run() {
                if (UnityPlayer.this.mSoftInputDialog != null) {
                    UnityPlayer.this.mSoftInputDialog.m385a(z);
                }
            }
        });
    }

    protected void setSelection(final int i, final int i2) {
        runOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.9
            @Override // java.lang.Runnable
            public final void run() {
                if (UnityPlayer.this.mSoftInputDialog != null) {
                    UnityPlayer.this.mSoftInputDialog.m383a(i, i2);
                }
            }
        });
    }

    protected void setSoftInputStr(final String str) {
        runOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.6
            @Override // java.lang.Runnable
            public final void run() {
                if (UnityPlayer.this.mSoftInputDialog == null || str == null) {
                    return;
                }
                UnityPlayer.this.mSoftInputDialog.m384a(str);
            }
        });
    }

    protected void showSoftInput(final String str, final int i, final boolean z, final boolean z2, final boolean z3, final boolean z4, final String str2, final int i2, final boolean z5, final boolean z6) {
        postOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.4
            @Override // java.lang.Runnable
            public final void run() {
                UnityPlayer.this.mSoftInputDialog = new DialogC0918i(UnityPlayer.this.mContext, UnityPlayer.this, str, i, z, z2, z3, str2, i2, z5, z6);
                UnityPlayer.this.mSoftInputDialog.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.unity3d.player.UnityPlayer.4.1
                    @Override // android.content.DialogInterface.OnCancelListener
                    public final void onCancel(DialogInterface dialogInterface) {
                        UnityPlayer.this.nativeSoftInputLostFocus();
                        UnityPlayer.this.reportSoftInputStr(null, 1, false);
                    }
                });
                UnityPlayer.this.mSoftInputDialog.show();
                UnityPlayer.this.nativeReportKeyboardConfigChanged();
            }
        });
    }

    protected boolean showVideoPlayer(String str, int i, int i2, int i3, boolean z, int i4, int i5) {
        if (this.mVideoPlayerProxy == null) {
            this.mVideoPlayerProxy = new C0924o(this);
        }
        boolean zM425a = this.mVideoPlayerProxy.m425a(this.mContext, str, i, i2, i3, z, i4, i5, new C0924o.a() { // from class: com.unity3d.player.UnityPlayer.14
            @Override // com.unity3d.player.C0924o.a
            /* JADX INFO: renamed from: a */
            public final void mo289a() {
                UnityPlayer.this.mVideoPlayerProxy = null;
            }
        });
        if (zM425a) {
            runOnUiThread(new Runnable() { // from class: com.unity3d.player.UnityPlayer.15
                @Override // java.lang.Runnable
                public final void run() {
                    if (!UnityPlayer.this.nativeIsAutorotationOn() || UnityPlayer.this.mActivity == null) {
                        return;
                    }
                    ((Activity) UnityPlayer.this.mContext).setRequestedOrientation(UnityPlayer.this.mInitialScreenOrientation);
                }
            });
        }
        return zM425a;
    }

    protected boolean skipPermissionsDialog() {
        Activity activity = this.mActivity;
        if (activity != null) {
            return UnityPermissions.skipPermissionsDialog(activity);
        }
        return false;
    }

    public boolean startOrientationListener(int i) {
        String str;
        if (this.mOrientationListener != null) {
            str = "Orientation Listener already started.";
        } else {
            OrientationEventListener orientationEventListener = new OrientationEventListener(this.mContext, i) { // from class: com.unity3d.player.UnityPlayer.17
                @Override // android.view.OrientationEventListener
                public final void onOrientationChanged(int i2) {
                    UnityPlayer.this.m_MainThread.m292a(UnityPlayer.this.mNaturalOrientation, i2);
                }
            };
            this.mOrientationListener = orientationEventListener;
            if (orientationEventListener.canDetectOrientation()) {
                this.mOrientationListener.enable();
                return true;
            }
            str = "Orientation Listener cannot detect orientation.";
        }
        C0915f.Log(5, str);
        return false;
    }

    public boolean stopOrientationListener() {
        OrientationEventListener orientationEventListener = this.mOrientationListener;
        if (orientationEventListener == null) {
            C0915f.Log(5, "Orientation Listener was not started.");
            return false;
        }
        orientationEventListener.disable();
        this.mOrientationListener = null;
        return true;
    }

    protected void toggleGyroscopeSensor(boolean z) {
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        Sensor defaultSensor = sensorManager.getDefaultSensor(11);
        if (z) {
            sensorManager.registerListener(this.m_FakeListener, defaultSensor, 1);
        } else {
            sensorManager.unregisterListener(this.m_FakeListener);
        }
    }

    public void unload() {
        nativeApplicationUnload();
    }

    public void windowFocusChanged(boolean z) {
        this.mState.m396a(z);
        if (this.mState.m401e()) {
            DialogC0918i dialogC0918i = this.mSoftInputDialog;
            if (dialogC0918i == null || dialogC0918i.f374a) {
                if (z) {
                    this.m_MainThread.m296c();
                } else {
                    this.m_MainThread.m298d();
                }
                checkResumePlayer();
            }
        }
    }
}
