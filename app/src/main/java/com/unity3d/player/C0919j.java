package com.unity3d.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

/* JADX INFO: renamed from: com.unity3d.player.j */
/* JADX INFO: loaded from: classes2.dex */
public final class C0919j extends View {

    /* JADX INFO: renamed from: a */
    final int f387a;

    /* JADX INFO: renamed from: b */
    final int f388b;

    /* JADX INFO: renamed from: c */
    Bitmap f389c;

    /* JADX INFO: renamed from: d */
    Bitmap f390d;

    /* JADX INFO: renamed from: com.unity3d.player.j$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {

        /* JADX INFO: renamed from: a */
        static final /* synthetic */ int[] f391a;

        static {
            int[] iArr = new int[a.m389a().length];
            f391a = iArr;
            try {
                iArr[a.f392a - 1] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                f391a[a.f393b - 1] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                f391a[a.f394c - 1] = 3;
            } catch (NoSuchFieldError unused3) {
            }
        }
    }

    /* JADX WARN: $VALUES field not found */
    /* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
    /* JADX INFO: renamed from: com.unity3d.player.j$a */
    static final class a {

        /* JADX INFO: renamed from: a */
        public static final int f392a = 1;

        /* JADX INFO: renamed from: b */
        public static final int f393b = 2;

        /* JADX INFO: renamed from: c */
        public static final int f394c = 3;

        /* JADX INFO: renamed from: d */
        private static final /* synthetic */ int[] f395d = {1, 2, 3};

        /* JADX INFO: renamed from: a */
        public static int[] m389a() {
            return (int[]) f395d.clone();
        }
    }

    public C0919j(Context context, int i) {
        super(context);
        this.f387a = i;
        int identifier = getResources().getIdentifier("unity_static_splash", "drawable", getContext().getPackageName());
        this.f388b = identifier;
        if (identifier != 0) {
            forceLayout();
        }
    }

    @Override // android.view.View
    public final void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Bitmap bitmap = this.f389c;
        if (bitmap != null) {
            bitmap.recycle();
            this.f389c = null;
        }
        Bitmap bitmap2 = this.f390d;
        if (bitmap2 != null) {
            bitmap2.recycle();
            this.f390d = null;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:32:0x006d  */
    @Override // android.view.View
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.f388b == 0) {
            return;
        }
        if (this.f389c == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            this.f389c = BitmapFactory.decodeResource(getResources(), this.f388b, options);
        }
        int width = this.f389c.getWidth();
        int height = this.f389c.getHeight();
        int width2 = getWidth();
        int height2 = getHeight();
        if (width2 == 0 || height2 == 0) {
            return;
        }
        float f = width / height;
        float f2 = width2;
        float f3 = height2;
        boolean z2 = f2 / f3 <= f;
        int i5 = AnonymousClass1.f391a[this.f387a - 1];
        if (i5 == 1) {
            if (width2 < width) {
                height = (int) (f2 / f);
                width = width2;
            }
            if (height2 < height) {
            }
        } else if (i5 == 2 || i5 == 3) {
            if ((this.f387a == a.f394c) ^ z2) {
                height = (int) (f2 / f);
                width = width2;
            } else {
                width = (int) (f3 * f);
                height = height2;
            }
        }
        Bitmap bitmap = this.f390d;
        if (bitmap != null) {
            if (bitmap.getWidth() == width && this.f390d.getHeight() == height) {
                return;
            }
            Bitmap bitmap2 = this.f390d;
            if (bitmap2 != this.f389c) {
                bitmap2.recycle();
                this.f390d = null;
            }
        }
        Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(this.f389c, width, height, true);
        this.f390d = bitmapCreateScaledBitmap;
        bitmapCreateScaledBitmap.setDensity(getResources().getDisplayMetrics().densityDpi);
        ColorDrawable colorDrawable = new ColorDrawable(0xFF000000);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), this.f390d);
        bitmapDrawable.setGravity(17);
        setBackground(new LayerDrawable(new Drawable[]{colorDrawable, bitmapDrawable}));
    }
}
