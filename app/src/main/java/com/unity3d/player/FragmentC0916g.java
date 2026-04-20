package com.unity3d.player;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.unity3d.player.UnityPermissions;

/* JADX INFO: renamed from: com.unity3d.player.g */
/* JADX INFO: loaded from: classes2.dex */
public final class FragmentC0916g extends Fragment {

    /* JADX INFO: renamed from: a */
    private final IPermissionRequestCallbacks f357a;

    /* JADX INFO: renamed from: b */
    private final Activity f358b;

    /* JADX INFO: renamed from: c */
    private final Looper f359c;

    /* JADX INFO: renamed from: com.unity3d.player.g$a */
    class a implements Runnable {

        /* JADX INFO: renamed from: b */
        private IPermissionRequestCallbacks f363b;

        /* JADX INFO: renamed from: c */
        private String f364c;

        /* JADX INFO: renamed from: d */
        private int f365d;

        /* JADX INFO: renamed from: e */
        private boolean f366e;

        a(IPermissionRequestCallbacks iPermissionRequestCallbacks, String str, int i, boolean z) {
            this.f363b = iPermissionRequestCallbacks;
            this.f364c = str;
            this.f365d = i;
            this.f366e = z;
        }

        @Override // java.lang.Runnable
        public final void run() {
            int i = this.f365d;
            if (i != -1) {
                if (i == 0) {
                    this.f363b.onPermissionGranted(this.f364c);
                }
            } else if (this.f366e) {
                this.f363b.onPermissionDenied(this.f364c);
            } else {
                this.f363b.onPermissionDeniedAndDontAskAgain(this.f364c);
            }
        }
    }

    public FragmentC0916g() {
        this.f357a = null;
        this.f358b = null;
        this.f359c = null;
    }

    public FragmentC0916g(Activity activity, IPermissionRequestCallbacks iPermissionRequestCallbacks) {
        this.f357a = iPermissionRequestCallbacks;
        this.f358b = activity;
        this.f359c = Looper.myLooper();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: a */
    public void m367a(String[] strArr) {
        for (String str : strArr) {
            this.f357a.onPermissionDenied(str);
        }
    }

    @Override // android.app.Fragment
    public final void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestPermissions(getArguments().getStringArray("PermissionNames"), 96489);
    }

    @Override // android.app.Fragment
    public final void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i != 96489) {
            return;
        }
        if (strArr.length != 0) {
            for (int i2 = 0; i2 < strArr.length && i2 < iArr.length; i2++) {
                IPermissionRequestCallbacks iPermissionRequestCallbacks = this.f357a;
                if (iPermissionRequestCallbacks != null && this.f358b != null && this.f359c != null) {
                    if (iPermissionRequestCallbacks instanceof UnityPermissions.ModalWaitForPermissionResponse) {
                        iPermissionRequestCallbacks.onPermissionGranted(strArr[i2]);
                    } else {
                        String str = strArr[i2] == null ? "<null>" : strArr[i2];
                        new Handler(this.f359c).post(new a(this.f357a, str, iArr[i2], this.f358b.shouldShowRequestPermissionRationale(str)));
                    }
                }
            }
        } else if (this.f357a != null && this.f358b != null && this.f359c != null) {
            final String[] stringArray = getArguments().getStringArray("PermissionNames");
            if (this.f357a instanceof UnityPermissions.ModalWaitForPermissionResponse) {
                m367a(stringArray);
            } else {
                new Handler(this.f359c).post(new Runnable() { // from class: com.unity3d.player.g.1
                    @Override // java.lang.Runnable
                    public final void run() {
                        FragmentC0916g.this.m367a(stringArray);
                    }
                });
            }
        }
        FragmentTransaction fragmentTransactionBeginTransaction = getActivity().getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.remove(this);
        fragmentTransactionBeginTransaction.commit();
    }
}
