package com.unity3d.player;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

/* JADX INFO: loaded from: classes2.dex */
public class NetworkConnectivity extends Activity {

    /* JADX INFO: renamed from: d */
    private int f169d;

    /* JADX INFO: renamed from: e */
    private ConnectivityManager f170e;

    /* JADX INFO: renamed from: a */
    private final int f166a = 0;

    /* JADX INFO: renamed from: b */
    private final int f167b = 1;

    /* JADX INFO: renamed from: c */
    private final int f168c = 2;

    /* JADX INFO: renamed from: f */
    private final ConnectivityManager.NetworkCallback f171f = new ConnectivityManager.NetworkCallback() { // from class: com.unity3d.player.NetworkConnectivity.1
        @Override // android.net.ConnectivityManager.NetworkCallback
        public final void onAvailable(Network network) {
            super.onAvailable(network);
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public final void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            NetworkConnectivity networkConnectivity;
            int i;
            super.onCapabilitiesChanged(network, networkCapabilities);
            if (networkCapabilities.hasTransport(0)) {
                networkConnectivity = NetworkConnectivity.this;
                i = 1;
            } else {
                networkConnectivity = NetworkConnectivity.this;
                i = 2;
            }
            networkConnectivity.f169d = i;
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public final void onLost(Network network) {
            super.onLost(network);
            NetworkConnectivity.this.f169d = 0;
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public final void onUnavailable() {
            super.onUnavailable();
            NetworkConnectivity.this.f169d = 0;
        }
    };

    public NetworkConnectivity(Context context) {
        this.f169d = 0;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.f170e = connectivityManager;
        connectivityManager.registerDefaultNetworkCallback(this.f171f);
        NetworkInfo activeNetworkInfo = this.f170e.getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            return;
        }
        this.f169d = activeNetworkInfo.getType() != 0 ? 2 : 1;
    }

    /* JADX INFO: renamed from: a */
    public final int m270a() {
        return this.f169d;
    }

    /* JADX INFO: renamed from: b */
    public final void m271b() {
        this.f170e.unregisterNetworkCallback(this.f171f);
    }
}
