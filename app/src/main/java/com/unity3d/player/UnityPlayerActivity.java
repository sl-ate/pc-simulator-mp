package com.unity3d.player;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;

/* JADX INFO: loaded from: classes2.dex */
public class UnityPlayerActivity extends Activity implements IUnityPlayerLifecycleEvents, View.OnClickListener {
    private static final String MOD_TAG = "ModHooks";
    private static final String JAVA_CRASH_LOG_FILE = "mp_java_crash.log";
    private static final String EXIT_INFO_LOG_FILE = "mp_exit_info.log";
    private static volatile boolean sCrashLoggerInstalled = false;
    private static volatile Method sQueueGlThreadEventMethod = null;
    protected UnityPlayer mUnityPlayer;

    private static native void nativeInitMod();
    private native void nativeOnUnityReady();
    private native void nativeOnUnityTick();
    private native void nativeOnUiButtonClick(int i);

    static {
        try {
            System.loadLibrary("modhooks");
            nativeInitMod();
            Log.i(MOD_TAG, "modhooks loaded");
        } catch (Throwable th) {
            Log.w(MOD_TAG, "modhooks load failed: " + th.getMessage());
        }
    }

    @Override // com.unity3d.player.IUnityPlayerLifecycleEvents
    public void onUnityPlayerQuitted() {
    }

    protected String updateUnityCommandLineArguments(String str) {
        return str;
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        requestWindowFeature(1);
        super.onCreate(bundle);
        installCrashLoggers();
        getIntent().putExtra("unity", updateUnityCommandLineArguments(getIntent().getStringExtra("unity")));
        UnityPlayer unityPlayer = new UnityPlayer(this, this);
        this.mUnityPlayer = unityPlayer;
        setContentView(unityPlayer);
        this.mUnityPlayer.requestFocus();
        try {
            nativeOnUnityReady();
        } catch (Throwable th) {
            Log.w(MOD_TAG, "native unity-ready hook failed: " + th.getMessage());
        }
    }

    @Override // com.unity3d.player.IUnityPlayerLifecycleEvents
    public void onUnityPlayerUnloaded() {
        moveTaskToBack(true);
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        this.mUnityPlayer.newIntent(intent);
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.mUnityPlayer.destroy();
        super.onDestroy();
    }

    @Override // android.app.Activity
    protected void onStop() {
        super.onStop();
        if (MultiWindowSupport.getAllowResizableWindow(this)) {
            this.mUnityPlayer.pause();
        }
    }

    @Override // android.app.Activity
    protected void onStart() {
        super.onStart();
        if (MultiWindowSupport.getAllowResizableWindow(this)) {
            this.mUnityPlayer.resume();
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        MultiWindowSupport.saveMultiWindowMode(this);
        if (MultiWindowSupport.getAllowResizableWindow(this)) {
            return;
        }
        this.mUnityPlayer.pause();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (!MultiWindowSupport.getAllowResizableWindow(this) || MultiWindowSupport.isMultiWindowModeChangedToTrue(this)) {
            this.mUnityPlayer.resume();
        }
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onLowMemory() {
        super.onLowMemory();
        this.mUnityPlayer.lowMemory();
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks2
    public void onTrimMemory(int i) {
        super.onTrimMemory(i);
        if (i == 15) {
            this.mUnityPlayer.lowMemory();
        }
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mUnityPlayer.configurationChanged(configuration);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        this.mUnityPlayer.windowFocusChanged(z);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == 2) {
            return this.mUnityPlayer.injectEvent(keyEvent);
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return this.mUnityPlayer.injectEvent(keyEvent);
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return this.mUnityPlayer.injectEvent(keyEvent);
    }

    @Override // android.app.Activity
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mUnityPlayer.injectEvent(motionEvent);
    }

    @Override // android.app.Activity
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return this.mUnityPlayer.injectEvent(motionEvent);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == null) {
            return;
        }
        try {
            Log.i(MOD_TAG, "ui click id=0x" + Integer.toHexString(view.getId()));
            nativeOnUiButtonClick(view.getId());
        } catch (Throwable th) {
            Log.w(MOD_TAG, "native ui click failed: " + th.getMessage());
        }
    }

    public void pumpNativeOnUnityThread() {
        UnityPlayer unityPlayer = this.mUnityPlayer;
        if (unityPlayer == null) {
            return;
        }
        Method queueMethod = sQueueGlThreadEventMethod;
        if (queueMethod == null) {
            try {
                Method m = UnityPlayer.class.getDeclaredMethod("queueGLThreadEvent", Runnable.class);
                m.setAccessible(true);
                sQueueGlThreadEventMethod = m;
                queueMethod = m;
            } catch (Throwable th) {
                Log.w(MOD_TAG, "queueGLThreadEvent reflect failed: " + th.getMessage());
                return;
            }
        }
        try {
            queueMethod.invoke(unityPlayer, new Runnable() { // from class: com.unity3d.player.UnityPlayerActivity.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    UnityPlayerActivity.this.nativeOnUnityTick();
                } catch (Throwable th) {
                    Log.w(MOD_TAG, "nativeOnUnityTick failed: " + th.getMessage());
                }
            }
            });
        } catch (Throwable th) {
            Log.w(MOD_TAG, "queueGLThreadEvent invoke failed: " + th.getMessage());
        }
    }

    private File getModLogBaseDir() {
        File ext = getExternalFilesDir(null);
        return ext != null ? ext : getFilesDir();
    }

    private void appendText(File file, String text) {
        if (file == null || text == null || text.isEmpty()) {
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write(text);
            writer.flush();
            writer.close();
        } catch (Throwable th) {
            Log.w(MOD_TAG, "appendText failed: " + th.getMessage());
        }
    }

    private void installCrashLoggers() {
        installJavaCrashLogger();
        logRecentExitInfo();
    }

    private void installJavaCrashLogger() {
        if (sCrashLoggerInstalled) {
            return;
        }
        final Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        final File crashFile = new File(getModLogBaseDir(), JAVA_CRASH_LOG_FILE);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { // from class: com.unity3d.player.UnityPlayerActivity.1
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    pw.println("=== Java Crash ===");
                    pw.println("timeMs=" + System.currentTimeMillis());
                    pw.println("thread=" + (thread != null ? thread.getName() : "unknown"));
                    if (throwable != null) {
                        throwable.printStackTrace(pw);
                    }
                    pw.println();
                    pw.flush();
                    UnityPlayerActivity.this.appendText(crashFile, sw.toString());
                } catch (Throwable th) {
                    Log.w(MOD_TAG, "java crash log write failed: " + th.getMessage());
                }
                if (previousHandler != null) {
                    previousHandler.uncaughtException(thread, throwable);
                    return;
                }
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
        sCrashLoggerInstalled = true;
        Log.i(MOD_TAG, "Java crash logger installed: " + crashFile.getAbsolutePath());
    }

    private void logRecentExitInfo() {
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am == null) {
                return;
            }
            List<ApplicationExitInfo> infos = am.getHistoricalProcessExitReasons(getPackageName(), 0, 1);
            if (infos == null || infos.isEmpty()) {
                return;
            }
            ApplicationExitInfo info = infos.get(0);
            String line = "ts=" + info.getTimestamp() +
                    " reason=" + info.getReason() +
                    " status=" + info.getStatus() +
                    " desc=" + info.getDescription() + "\n";
            File exitInfoFile = new File(getModLogBaseDir(), EXIT_INFO_LOG_FILE);
            appendText(exitInfoFile, line);
            Log.i(MOD_TAG, "LastExitInfo reason=" + info.getReason() + " status=" + info.getStatus());
        } catch (Throwable th) {
            Log.w(MOD_TAG, "logRecentExitInfo failed: " + th.getMessage());
        }
    }
}
