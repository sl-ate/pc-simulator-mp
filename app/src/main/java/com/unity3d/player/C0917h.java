package com.unity3d.player;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;

/* JADX INFO: renamed from: com.unity3d.player.h */
/* JADX INFO: loaded from: classes2.dex */
final class C0917h implements Application.ActivityLifecycleCallbacks {

    /* JADX INFO: renamed from: b */
    Activity f368b;

    /* JADX INFO: renamed from: a */
    WeakReference f367a = new WeakReference(null);

    /* JADX INFO: renamed from: c */
    a f369c = null;

    /* JADX INFO: renamed from: com.unity3d.player.h$a */
    class a extends View implements PixelCopy.OnPixelCopyFinishedListener {

        /* JADX INFO: renamed from: a */
        Bitmap f370a;

        a(Context context) {
            super(context);
        }

        /* JADX INFO: renamed from: a */
        public final void m372a(SurfaceView surfaceView) {
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
            this.f370a = bitmapCreateBitmap;
            PixelCopy.request(surfaceView, bitmapCreateBitmap, this, new Handler(Looper.getMainLooper()));
        }

        @Override // android.view.PixelCopy.OnPixelCopyFinishedListener
        public final void onPixelCopyFinished(int i) {
            if (i == 0) {
                setBackground(new LayerDrawable(new Drawable[]{new ColorDrawable(0xFF000000), new BitmapDrawable(getResources(), this.f370a)}));
            }
        }
    }

    C0917h(Context context) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            this.f368b = activity;
            activity.getApplication().registerActivityLifecycleCallbacks(this);
        }
    }

    /* JADX INFO: renamed from: a */
    public final void m368a() {
        Activity activity = this.f368b;
        if (activity != null) {
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
        }
    }

    /* JADX INFO: renamed from: a */
    public final void m369a(SurfaceView surfaceView) {
        if (PlatformSupport.NOUGAT_SUPPORT && this.f369c == null) {
            a aVar = new a(this.f368b);
            this.f369c = aVar;
            aVar.m372a(surfaceView);
        }
    }

    /* JADX INFO: renamed from: a */
    public final void m370a(ViewGroup viewGroup) {
        a aVar = this.f369c;
        if (aVar == null || aVar.getParent() != null) {
            return;
        }
        viewGroup.addView(this.f369c);
        viewGroup.bringChildToFront(this.f369c);
    }

    /* JADX INFO: renamed from: b */
    public final void m371b(ViewGroup viewGroup) {
        a aVar = this.f369c;
        if (aVar == null || aVar.getParent() == null) {
            return;
        }
        viewGroup.removeView(this.f369c);
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityDestroyed(Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityPaused(Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityResumed(Activity activity) {
        this.f367a = new WeakReference(activity);
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityStarted(Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityStopped(Activity activity) {
    }
}
