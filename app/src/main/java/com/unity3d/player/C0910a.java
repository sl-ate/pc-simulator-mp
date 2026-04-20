package com.unity3d.player;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.android.play.core.assetpacks.AssetPackLocation;
import com.google.android.play.core.assetpacks.AssetPackManager;
import com.google.android.play.core.assetpacks.AssetPackManagerFactory;
import com.google.android.play.core.assetpacks.AssetPackState;
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener;
import com.google.android.play.core.assetpacks.AssetPackStates;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.RuntimeExecutionException;
import com.google.android.play.core.tasks.Task;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/* JADX INFO: renamed from: com.unity3d.player.a */
/* JADX INFO: loaded from: classes2.dex */
final class C0910a implements InterfaceC0913d {

    /* JADX INFO: renamed from: a */
    private static C0910a f280a;

    /* JADX INFO: renamed from: b */
    private AssetPackManager f281b;

    /* JADX INFO: renamed from: c */
    private HashSet f282c;

    /* JADX INFO: renamed from: d */
    private Object f283d;

    /* JADX INFO: renamed from: com.unity3d.player.a$a */
    private static class a implements Runnable {

        /* JADX INFO: renamed from: a */
        private Set f284a;

        /* JADX INFO: renamed from: b */
        private String f285b;

        /* JADX INFO: renamed from: c */
        private int f286c;

        /* JADX INFO: renamed from: d */
        private long f287d;

        /* JADX INFO: renamed from: e */
        private long f288e;

        /* JADX INFO: renamed from: f */
        private int f289f;

        /* JADX INFO: renamed from: g */
        private int f290g;

        a(Set set, String str, int i, long j, long j2, int i2, int i3) {
            this.f284a = set;
            this.f285b = str;
            this.f286c = i;
            this.f287d = j;
            this.f288e = j2;
            this.f289f = i2;
            this.f290g = i3;
        }

        @Override // java.lang.Runnable
        public final void run() {
            Iterator it = this.f284a.iterator();
            while (it.hasNext()) {
                ((IAssetPackManagerDownloadStatusCallback) it.next()).onStatusUpdate(this.f285b, this.f286c, this.f287d, this.f288e, this.f289f, this.f290g);
            }
        }
    }

    /* JADX INFO: renamed from: com.unity3d.player.a$b */
    private class b implements AssetPackStateUpdateListener {

        /* JADX INFO: renamed from: b */
        private HashSet f292b;

        /* JADX INFO: renamed from: c */
        private Looper f293c;

        public b(C0910a c0910a, IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback) {
            this(iAssetPackManagerDownloadStatusCallback, Looper.myLooper());
        }

        public b(IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback, Looper looper) {
            HashSet hashSet = new HashSet();
            this.f292b = hashSet;
            hashSet.add(iAssetPackManagerDownloadStatusCallback);
            this.f293c = looper;
        }

        /* JADX INFO: renamed from: a */
        private Set m317a(HashSet hashSet) {
            return (Set) hashSet.clone();
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* JADX INFO: renamed from: a, reason: merged with bridge method [inline-methods] */
        public synchronized void onStateUpdate(AssetPackState assetPackState) {
            if (assetPackState.status() == 4 || assetPackState.status() == 5 || assetPackState.status() == 0) {
                synchronized (C0910a.f280a) {
                    C0910a.this.f282c.remove(assetPackState.name());
                    if (C0910a.this.f282c.isEmpty()) {
                        C0910a.this.mo312a(C0910a.this.f283d);
                        C0910a.m308c(C0910a.this);
                    }
                }
            }
            if (this.f292b.size() == 0) {
                return;
            }
            new Handler(this.f293c).post(new a(m317a(this.f292b), assetPackState.name(), assetPackState.status(), assetPackState.totalBytesToDownload(), assetPackState.bytesDownloaded(), assetPackState.transferProgressPercentage(), assetPackState.errorCode()));
        }

        /* JADX INFO: renamed from: a */
        public final synchronized void m319a(IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback) {
            this.f292b.add(iAssetPackManagerDownloadStatusCallback);
        }
    }

    /* JADX INFO: renamed from: com.unity3d.player.a$c */
    private static class c implements OnSuccessListener<Integer> {

        /* JADX INFO: renamed from: a */
        private IAssetPackManagerMobileDataConfirmationCallback f294a;

        /* JADX INFO: renamed from: b */
        private Looper f295b = Looper.myLooper();

        /* JADX INFO: renamed from: com.unity3d.player.a$c$a */
        private static class a implements Runnable {

            /* JADX INFO: renamed from: a */
            private IAssetPackManagerMobileDataConfirmationCallback f296a;

            /* JADX INFO: renamed from: b */
            private boolean f297b;

            a(IAssetPackManagerMobileDataConfirmationCallback iAssetPackManagerMobileDataConfirmationCallback, boolean z) {
                this.f296a = iAssetPackManagerMobileDataConfirmationCallback;
                this.f297b = z;
            }

            @Override // java.lang.Runnable
            public final void run() {
                this.f296a.onMobileDataConfirmationResult(this.f297b);
            }
        }

        public c(IAssetPackManagerMobileDataConfirmationCallback iAssetPackManagerMobileDataConfirmationCallback) {
            this.f294a = iAssetPackManagerMobileDataConfirmationCallback;
        }

        public void onSuccess(Integer num) {
            if (this.f294a != null) {
                new Handler(this.f295b).post(new a(this.f294a, num.intValue() == -1));
            }
        }
    }

    /* JADX INFO: renamed from: com.unity3d.player.a$d */
    private static class d implements OnCompleteListener {

        /* JADX INFO: renamed from: a */
        private IAssetPackManagerDownloadStatusCallback f298a;

        /* JADX INFO: renamed from: b */
        private Looper f299b = Looper.myLooper();

        /* JADX INFO: renamed from: c */
        private String f300c;

        public d(IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback, String str) {
            this.f298a = iAssetPackManagerDownloadStatusCallback;
            this.f300c = str;
        }

        /* JADX INFO: renamed from: a */
        private void m321a(String str, int i, int i2, long j) {
            new Handler(this.f299b).post(new a(Collections.singleton(this.f298a), str, i, j, i == 4 ? j : 0L, 0, i2));
        }

        public final void onComplete(Task task) {
            try {
                AssetPackStates assetPackStates = (AssetPackStates) task.getResult();
                Map<String, AssetPackState> mapPackStates = assetPackStates.packStates();
                if (mapPackStates.size() == 0) {
                    return;
                }
                for (AssetPackState assetPackState : mapPackStates.values()) {
                    if (assetPackState.errorCode() != 0 || assetPackState.status() == 4 || assetPackState.status() == 5 || assetPackState.status() == 0) {
                        m321a(assetPackState.name(), assetPackState.status(), assetPackState.errorCode(), assetPackStates.totalBytes());
                    } else {
                        C0910a.f280a.m306a(assetPackState.name(), this.f298a, this.f299b);
                    }
                }
            } catch (RuntimeExecutionException e) {
                m321a(this.f300c, 0, e.getErrorCode(), 0L);
            }
        }
    }

    /* JADX INFO: renamed from: com.unity3d.player.a$e */
    private static class e implements OnCompleteListener {

        /* JADX INFO: renamed from: a */
        private IAssetPackManagerStatusQueryCallback f301a;

        /* JADX INFO: renamed from: b */
        private Looper f302b = Looper.myLooper();

        /* JADX INFO: renamed from: c */
        private String[] f303c;

        /* JADX INFO: renamed from: com.unity3d.player.a$e$a */
        private static class a implements Runnable {

            /* JADX INFO: renamed from: a */
            private IAssetPackManagerStatusQueryCallback f304a;

            /* JADX INFO: renamed from: b */
            private long f305b;

            /* JADX INFO: renamed from: c */
            private String[] f306c;

            /* JADX INFO: renamed from: d */
            private int[] f307d;

            /* JADX INFO: renamed from: e */
            private int[] f308e;

            a(IAssetPackManagerStatusQueryCallback iAssetPackManagerStatusQueryCallback, long j, String[] strArr, int[] iArr, int[] iArr2) {
                this.f304a = iAssetPackManagerStatusQueryCallback;
                this.f305b = j;
                this.f306c = strArr;
                this.f307d = iArr;
                this.f308e = iArr2;
            }

            @Override // java.lang.Runnable
            public final void run() {
                this.f304a.onStatusResult(this.f305b, this.f306c, this.f307d, this.f308e);
            }
        }

        public e(IAssetPackManagerStatusQueryCallback iAssetPackManagerStatusQueryCallback, String[] strArr) {
            this.f301a = iAssetPackManagerStatusQueryCallback;
            this.f303c = strArr;
        }

        public final void onComplete(Task task) {
            if (this.f301a == null) {
                return;
            }
            int i = 0;
            try {
                AssetPackStates assetPackStates = (AssetPackStates) task.getResult();
                Map<String, AssetPackState> mapPackStates = assetPackStates.packStates();
                int size = mapPackStates.size();
                String[] strArr = new String[size];
                int[] iArr = new int[size];
                int[] iArr2 = new int[size];
                for (AssetPackState assetPackState : mapPackStates.values()) {
                    strArr[i] = assetPackState.name();
                    iArr[i] = assetPackState.status();
                    iArr2[i] = assetPackState.errorCode();
                    i++;
                }
                new Handler(this.f302b).post(new a(this.f301a, assetPackStates.totalBytes(), strArr, iArr, iArr2));
            } catch (RuntimeExecutionException e) {
                String message = e.getMessage();
                for (String str : this.f303c) {
                    if (message.contains(str)) {
                        new Handler(this.f302b).post(new a(this.f301a, 0L, new String[]{str}, new int[]{0}, new int[]{e.getErrorCode()}));
                        return;
                    }
                }
                String[] strArr2 = this.f303c;
                int[] iArr3 = new int[strArr2.length];
                int[] iArr4 = new int[strArr2.length];
                for (int i2 = 0; i2 < this.f303c.length; i2++) {
                    iArr3[i2] = 0;
                    iArr4[i2] = e.getErrorCode();
                }
                new Handler(this.f302b).post(new a(this.f301a, 0L, this.f303c, iArr3, iArr4));
            }
        }
    }

    private C0910a(Context context) {
        if (f280a != null) {
            throw new RuntimeException("AssetPackManagerWrapper should be created only once. Use getInstance() instead.");
        }
        try {
            this.f281b = AssetPackManagerFactory.getInstance(context);
        } catch (Throwable th) {
            C0915f.Log(5, "Play Core init failed: " + th.getMessage());
            this.f281b = null;
        }
        this.f282c = new HashSet();
    }

    /* JADX INFO: renamed from: a */
    public static InterfaceC0913d m303a(Context context) {
        if (f280a == null) {
            try {
                f280a = new C0910a(context);
            } catch (Throwable th) {
                C0915f.Log(5, "AssetPack wrapper init failed: " + th.getMessage());
                return null;
            }
        }
        return f280a;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: a */
    public void m306a(String str, IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback, Looper looper) {
        if (this.f281b == null) {
            return;
        }
        synchronized (f280a) {
            try {
                if (this.f283d == null) {
                    b bVar = new b(iAssetPackManagerDownloadStatusCallback, looper);
                    this.f281b.registerListener(bVar);
                    this.f283d = bVar;
                } else {
                    ((b) this.f283d).m319a(iAssetPackManagerDownloadStatusCallback);
                }
                this.f282c.add(str);
                this.f281b.fetch(Collections.singletonList(str));
            } catch (Throwable th) {
                C0915f.Log(5, "AssetPack fetch/register failed: " + th.getMessage());
            }
        }
    }

    /* JADX INFO: renamed from: c */
    static /* synthetic */ Object m308c(C0910a c0910a) {
        c0910a.f283d = null;
        return null;
    }

    @Override // com.unity3d.player.InterfaceC0913d
    /* JADX INFO: renamed from: a */
    public final Object mo309a(IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback) {
        if (this.f281b == null) {
            return null;
        }
        try {
            b bVar = new b(this, iAssetPackManagerDownloadStatusCallback);
            this.f281b.registerListener(bVar);
            return bVar;
        } catch (Throwable th) {
            C0915f.Log(5, "AssetPack registerListener failed: " + th.getMessage());
            return null;
        }
    }

    @Override // com.unity3d.player.InterfaceC0913d
    /* JADX INFO: renamed from: a */
    public final String mo310a(String str) {
        if (this.f281b == null) {
            return "";
        }
        try {
            AssetPackLocation packLocation = this.f281b.getPackLocation(str);
            return packLocation == null ? "" : packLocation.assetsPath();
        } catch (Throwable th) {
            C0915f.Log(5, "AssetPack getPackLocation failed: " + th.getMessage());
            return "";
        }
    }

    @Override // com.unity3d.player.InterfaceC0913d
    /* JADX INFO: renamed from: a */
    public final void mo311a(Activity activity, IAssetPackManagerMobileDataConfirmationCallback iAssetPackManagerMobileDataConfirmationCallback) {
        if (iAssetPackManagerMobileDataConfirmationCallback == null) {
            return;
        }
        if (this.f281b == null) {
            iAssetPackManagerMobileDataConfirmationCallback.onMobileDataConfirmationResult(false);
            return;
        }
        try {
            this.f281b.showCellularDataConfirmation(activity).addOnSuccessListener(new c(iAssetPackManagerMobileDataConfirmationCallback));
        } catch (Throwable th) {
            C0915f.Log(5, "AssetPack mobile-data request failed: " + th.getMessage());
            iAssetPackManagerMobileDataConfirmationCallback.onMobileDataConfirmationResult(false);
        }
    }

    @Override // com.unity3d.player.InterfaceC0913d
    /* JADX INFO: renamed from: a */
    public final void mo312a(Object obj) {
        if (this.f281b == null) {
            return;
        }
        if (obj instanceof b) {
            try {
                this.f281b.unregisterListener((b) obj);
            } catch (Throwable th) {
                C0915f.Log(5, "AssetPack unregisterListener failed: " + th.getMessage());
            }
        }
    }

    @Override // com.unity3d.player.InterfaceC0913d
    /* JADX INFO: renamed from: a */
    public final void mo313a(String[] strArr) {
        if (this.f281b == null) {
            return;
        }
        try {
            this.f281b.cancel(Arrays.asList(strArr));
        } catch (Throwable th) {
            C0915f.Log(5, "AssetPack cancel failed: " + th.getMessage());
        }
    }

    @Override // com.unity3d.player.InterfaceC0913d
    /* JADX INFO: renamed from: a */
    public final void mo314a(String[] strArr, IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback) {
        if (this.f281b == null) {
            return;
        }
        for (String str : strArr) {
            try {
                this.f281b.getPackStates(Collections.singletonList(str)).addOnCompleteListener(new d(iAssetPackManagerDownloadStatusCallback, str));
            } catch (Throwable th) {
                C0915f.Log(5, "AssetPack getPackStates(download) failed: " + th.getMessage());
            }
        }
    }

    @Override // com.unity3d.player.InterfaceC0913d
    /* JADX INFO: renamed from: a */
    public final void mo315a(String[] strArr, IAssetPackManagerStatusQueryCallback iAssetPackManagerStatusQueryCallback) {
        if (this.f281b == null) {
            return;
        }
        try {
            this.f281b.getPackStates(Arrays.asList(strArr)).addOnCompleteListener(new e(iAssetPackManagerStatusQueryCallback, strArr));
        } catch (Throwable th) {
            C0915f.Log(5, "AssetPack getPackStates(query) failed: " + th.getMessage());
        }
    }

    @Override // com.unity3d.player.InterfaceC0913d
    /* JADX INFO: renamed from: b */
    public final void mo316b(String str) {
        if (this.f281b == null) {
            return;
        }
        try {
            this.f281b.removePack(str);
        } catch (Throwable th) {
            C0915f.Log(5, "AssetPack removePack failed: " + th.getMessage());
        }
    }
}
