package com.unity3d.player;

import android.app.Activity;
import android.content.Context;

/* JADX INFO: loaded from: classes2.dex */
class PlayAssetDeliveryUnityWrapper {

    /* JADX INFO: renamed from: a */
    private static PlayAssetDeliveryUnityWrapper f175a;

    /* JADX INFO: renamed from: b */
    private InterfaceC0913d f176b;

    private PlayAssetDeliveryUnityWrapper(Context context) {
        if (f175a != null) {
            throw new RuntimeException("PlayAssetDeliveryUnityWrapper should be created only once. Use getInstance() instead.");
        }
        try {
            Class.forName("com.google.android.play.core.assetpacks.AssetPackManager");
            this.f176b = m274a(context);
        } catch (ClassNotFoundException unused) {
            this.f176b = null;
        }
    }

    /* JADX INFO: renamed from: a */
    private static InterfaceC0913d m274a(Context context) {
        return C0910a.m303a(context);
    }

    /* JADX INFO: renamed from: a */
    private void m275a() {
        if (playCoreApiMissing()) {
            throw new RuntimeException("AssetPackManager API is not available! Make sure your gradle project includes \"com.google.android.play:core\" dependency.");
        }
    }

    public static synchronized PlayAssetDeliveryUnityWrapper getInstance() {
        while (f175a == null) {
            try {
                PlayAssetDeliveryUnityWrapper.class.wait(3000L);
            } catch (InterruptedException e) {
                C0915f.Log(6, e.getMessage());
            }
        }
        if (f175a == null) {
            throw new RuntimeException("PlayAssetDeliveryUnityWrapper is not yet initialised.");
        }
        return f175a;
    }

    public static synchronized PlayAssetDeliveryUnityWrapper init(Context context) {
        if (f175a != null) {
            throw new RuntimeException("PlayAssetDeliveryUnityWrapper.init() should be called only once. Use getInstance() instead.");
        }
        f175a = new PlayAssetDeliveryUnityWrapper(context);
        PlayAssetDeliveryUnityWrapper.class.notifyAll();
        return f175a;
    }

    public void cancelAssetPackDownload(String str) {
        cancelAssetPackDownloads(new String[]{str});
    }

    public void cancelAssetPackDownloads(String[] strArr) {
        m275a();
        this.f176b.mo313a(strArr);
    }

    public void downloadAssetPack(String str, IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback) {
        downloadAssetPacks(new String[]{str}, iAssetPackManagerDownloadStatusCallback);
    }

    public void downloadAssetPacks(String[] strArr, IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback) {
        m275a();
        this.f176b.mo314a(strArr, iAssetPackManagerDownloadStatusCallback);
    }

    public String getAssetPackPath(String str) {
        m275a();
        return this.f176b.mo310a(str);
    }

    public void getAssetPackState(String str, IAssetPackManagerStatusQueryCallback iAssetPackManagerStatusQueryCallback) {
        getAssetPackStates(new String[]{str}, iAssetPackManagerStatusQueryCallback);
    }

    public void getAssetPackStates(String[] strArr, IAssetPackManagerStatusQueryCallback iAssetPackManagerStatusQueryCallback) {
        m275a();
        this.f176b.mo315a(strArr, iAssetPackManagerStatusQueryCallback);
    }

    public boolean playCoreApiMissing() {
        return this.f176b == null;
    }

    public Object registerDownloadStatusListener(IAssetPackManagerDownloadStatusCallback iAssetPackManagerDownloadStatusCallback) {
        m275a();
        return this.f176b.mo309a(iAssetPackManagerDownloadStatusCallback);
    }

    public void removeAssetPack(String str) {
        m275a();
        this.f176b.mo316b(str);
    }

    public void requestToUseMobileData(Activity activity, IAssetPackManagerMobileDataConfirmationCallback iAssetPackManagerMobileDataConfirmationCallback) {
        m275a();
        this.f176b.mo311a(activity, iAssetPackManagerMobileDataConfirmationCallback);
    }

    public void unregisterDownloadStatusListener(Object obj) {
        m275a();
        this.f176b.mo312a(obj);
    }
}
