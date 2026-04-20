package com.unity3d.player;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/* JADX INFO: renamed from: com.unity3d.player.c */
/* JADX INFO: loaded from: classes2.dex */
public final class C0912c {

    /* JADX INFO: renamed from: b */
    private static CameraManager f317b;

    /* JADX INFO: renamed from: c */
    private static String[] f318c;

    /* JADX INFO: renamed from: e */
    private static Semaphore f319e = new Semaphore(1);

    /* JADX INFO: renamed from: a */
    private InterfaceC0914e f324a;

    /* JADX INFO: renamed from: d */
    private CameraDevice f325d;

    /* JADX INFO: renamed from: f */
    private HandlerThread f326f;

    /* JADX INFO: renamed from: g */
    private Handler f327g;

    /* JADX INFO: renamed from: h */
    private Rect f328h;

    /* JADX INFO: renamed from: i */
    private Rect f329i;

    /* JADX INFO: renamed from: j */
    private int f330j;

    /* JADX INFO: renamed from: k */
    private int f331k;

    /* JADX INFO: renamed from: n */
    private int f334n;

    /* JADX INFO: renamed from: o */
    private int f335o;

    /* JADX INFO: renamed from: q */
    private Range f337q;

    /* JADX INFO: renamed from: s */
    private Image f339s;

    /* JADX INFO: renamed from: t */
    private CaptureRequest.Builder f340t;

    /* JADX INFO: renamed from: w */
    private int f343w;

    /* JADX INFO: renamed from: x */
    private SurfaceTexture f344x;

    /* JADX INFO: renamed from: l */
    private float f332l = -1.0f;

    /* JADX INFO: renamed from: m */
    private float f333m = -1.0f;

    /* JADX INFO: renamed from: p */
    private boolean f336p = false;

    /* JADX INFO: renamed from: r */
    private ImageReader f338r = null;

    /* JADX INFO: renamed from: u */
    private CameraCaptureSession f341u = null;

    /* JADX INFO: renamed from: v */
    private Object f342v = new Object();

    /* JADX INFO: renamed from: y */
    private Surface f345y = null;

    /* JADX INFO: renamed from: z */
    private int f346z = a.f354c;

    /* JADX INFO: renamed from: A */
    private CameraCaptureSession.CaptureCallback f320A = new CameraCaptureSession.CaptureCallback() { // from class: com.unity3d.player.c.1
        @Override // android.hardware.camera2.CameraCaptureSession.CaptureCallback
        public final void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            C0912c.this.m335a(captureRequest.getTag());
        }

        @Override // android.hardware.camera2.CameraCaptureSession.CaptureCallback
        public final void onCaptureFailed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureFailure captureFailure) {
            C0915f.Log(5, "Camera2: Capture session failed " + captureRequest.getTag() + " reason " + captureFailure.getReason());
            C0912c.this.m335a(captureRequest.getTag());
        }

        @Override // android.hardware.camera2.CameraCaptureSession.CaptureCallback
        public final void onCaptureSequenceAborted(CameraCaptureSession cameraCaptureSession, int i) {
        }

        @Override // android.hardware.camera2.CameraCaptureSession.CaptureCallback
        public final void onCaptureSequenceCompleted(CameraCaptureSession cameraCaptureSession, int i, long j) {
        }
    };

    /* JADX INFO: renamed from: B */
    private final CameraDevice.StateCallback f321B = new CameraDevice.StateCallback() { // from class: com.unity3d.player.c.3
        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public final void onClosed(CameraDevice cameraDevice) {
            C0912c.f319e.release();
        }

        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public final void onDisconnected(CameraDevice cameraDevice) {
            C0915f.Log(5, "Camera2: CameraDevice disconnected.");
            C0912c.this.m333a(cameraDevice);
            C0912c.f319e.release();
        }

        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public final void onError(CameraDevice cameraDevice, int i) {
            C0915f.Log(6, "Camera2: Error opeining CameraDevice " + i);
            C0912c.this.m333a(cameraDevice);
            C0912c.f319e.release();
        }

        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public final void onOpened(CameraDevice cameraDevice) {
            C0912c.this.f325d = cameraDevice;
            C0912c.f319e.release();
        }
    };

    /* JADX INFO: renamed from: C */
    private final ImageReader.OnImageAvailableListener f322C = new ImageReader.OnImageAvailableListener() { // from class: com.unity3d.player.c.4
        @Override // android.media.ImageReader.OnImageAvailableListener
        public final void onImageAvailable(ImageReader imageReader) {
            if (C0912c.f319e.tryAcquire()) {
                Image imageAcquireNextImage = imageReader.acquireNextImage();
                if (imageAcquireNextImage != null) {
                    Image.Plane[] planes = imageAcquireNextImage.getPlanes();
                    if (imageAcquireNextImage.getFormat() == 35 && planes != null && planes.length == 3) {
                        C0912c.this.f324a.mo261a(planes[0].getBuffer(), planes[1].getBuffer(), planes[2].getBuffer(), planes[0].getRowStride(), planes[1].getRowStride(), planes[1].getPixelStride());
                    } else {
                        C0915f.Log(6, "Camera2: Wrong image format.");
                    }
                    if (C0912c.this.f339s != null) {
                        C0912c.this.f339s.close();
                    }
                    C0912c.this.f339s = imageAcquireNextImage;
                }
                C0912c.f319e.release();
            }
        }
    };

    /* JADX INFO: renamed from: D */
    private final SurfaceTexture.OnFrameAvailableListener f323D = new SurfaceTexture.OnFrameAvailableListener() { // from class: com.unity3d.player.c.5
        @Override // android.graphics.SurfaceTexture.OnFrameAvailableListener
        public final void onFrameAvailable(SurfaceTexture surfaceTexture) {
            C0912c.this.f324a.mo260a(surfaceTexture);
        }
    };

    /* JADX WARN: $VALUES field not found */
    /* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
    /* JADX INFO: renamed from: com.unity3d.player.c$a */
    private static final class a {

        /* JADX INFO: renamed from: a */
        public static final int f352a = 1;

        /* JADX INFO: renamed from: b */
        public static final int f353b = 2;

        /* JADX INFO: renamed from: c */
        public static final int f354c = 3;

        /* JADX INFO: renamed from: d */
        private static final /* synthetic */ int[] f355d = {1, 2, 3};
    }

    protected C0912c(InterfaceC0914e interfaceC0914e) {
        this.f324a = null;
        this.f324a = interfaceC0914e;
        m351g();
    }

    /* JADX INFO: renamed from: a */
    public static int m324a(Context context) {
        return m344c(context).length;
    }

    /* JADX INFO: renamed from: a */
    public static int m325a(Context context, int i) {
        try {
            return ((Integer) m337b(context).getCameraCharacteristics(m344c(context)[i]).get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        } catch (CameraAccessException e) {
            C0915f.Log(6, "Camera2: CameraAccessException " + e);
            return 0;
        }
    }

    /* JADX INFO: renamed from: a */
    private static int m326a(Range[] rangeArr, int i) {
        int i2 = -1;
        double d = Double.MAX_VALUE;
        for (int i3 = 0; i3 < rangeArr.length; i3++) {
            int iIntValue = ((Integer) rangeArr[i3].getLower()).intValue();
            int iIntValue2 = ((Integer) rangeArr[i3].getUpper()).intValue();
            float f = i;
            if (f + 0.1f > iIntValue && f - 0.1f < iIntValue2) {
                return i;
            }
            double dMin = Math.min(Math.abs(f - iIntValue), Math.abs(f - iIntValue2));
            if (dMin < d) {
                i2 = i3;
                d = dMin;
            }
        }
        return ((Integer) (i > ((Integer) rangeArr[i2].getUpper()).intValue() ? rangeArr[i2].getUpper() : rangeArr[i2].getLower())).intValue();
    }

    /* JADX INFO: renamed from: a */
    private static Rect m327a(Size[] sizeArr, double d, double d2) {
        double d3 = Double.MAX_VALUE;
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < sizeArr.length; i3++) {
            int width = sizeArr[i3].getWidth();
            int height = sizeArr[i3].getHeight();
            double dAbs = Math.abs(Math.log(d / ((double) width))) + Math.abs(Math.log(d2 / ((double) height)));
            if (dAbs < d3) {
                i = width;
                i2 = height;
                d3 = dAbs;
            }
        }
        return new Rect(0, 0, i, i2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: a */
    public void m333a(CameraDevice cameraDevice) {
        synchronized (this.f342v) {
            this.f341u = null;
        }
        cameraDevice.close();
        this.f325d = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: a */
    public void m335a(Object obj) {
        if (obj != "Focus") {
            if (obj == "Cancel focus") {
                synchronized (this.f342v) {
                    if (this.f341u != null) {
                        m357j();
                    }
                }
                return;
            }
            return;
        }
        this.f336p = false;
        synchronized (this.f342v) {
            if (this.f341u != null) {
                try {
                    this.f340t.set(CaptureRequest.CONTROL_AF_TRIGGER, 0);
                    this.f340t.setTag("Regular");
                    this.f341u.setRepeatingRequest(this.f340t.build(), this.f320A, this.f327g);
                } catch (CameraAccessException e) {
                    C0915f.Log(6, "Camera2: CameraAccessException " + e);
                }
            }
        }
    }

    /* JADX INFO: renamed from: a */
    private static Size[] m336a(CameraCharacteristics cameraCharacteristics) {
        StreamConfigurationMap streamConfigurationMap = (StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigurationMap == null) {
            C0915f.Log(6, "Camera2: configuration map is not available.");
            return null;
        }
        Size[] outputSizes = streamConfigurationMap.getOutputSizes(35);
        if (outputSizes == null || outputSizes.length == 0) {
            return null;
        }
        return outputSizes;
    }

    /* JADX INFO: renamed from: b */
    private static CameraManager m337b(Context context) {
        if (f317b == null) {
            f317b = (CameraManager) context.getSystemService("camera");
        }
        return f317b;
    }

    /* JADX INFO: renamed from: b */
    private void m339b(CameraCharacteristics cameraCharacteristics) {
        int iIntValue = ((Integer) cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)).intValue();
        this.f331k = iIntValue;
        if (iIntValue > 0) {
            this.f329i = (Rect) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            float fWidth = this.f328h.width() / this.f328h.height();
            if (fWidth > (((float) this.f329i.width()) / this.f329i.height())) {
                this.f334n = 0;
                this.f335o = (int) ((this.f329i.height() - (this.f329i.width() / fWidth)) / 2.0f);
            } else {
                this.f335o = 0;
                this.f334n = (int) ((this.f329i.width() - (this.f329i.height() * fWidth)) / 2.0f);
            }
            this.f330j = Math.min(this.f329i.width(), this.f329i.height()) / 20;
        }
    }

    /* JADX INFO: renamed from: b */
    public static boolean m341b(Context context, int i) {
        try {
            return ((Integer) m337b(context).getCameraCharacteristics(m344c(context)[i]).get(CameraCharacteristics.LENS_FACING)).intValue() == 0;
        } catch (CameraAccessException e) {
            C0915f.Log(6, "Camera2: CameraAccessException " + e);
            return false;
        }
    }

    /* JADX INFO: renamed from: c */
    public static boolean m343c(Context context, int i) {
        try {
            return ((Integer) m337b(context).getCameraCharacteristics(m344c(context)[i]).get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)).intValue() > 0;
        } catch (CameraAccessException e) {
            C0915f.Log(6, "Camera2: CameraAccessException " + e);
            return false;
        }
    }

    /* JADX INFO: renamed from: c */
    private static String[] m344c(Context context) {
        if (f318c == null) {
            try {
                f318c = m337b(context).getCameraIdList();
            } catch (CameraAccessException e) {
                C0915f.Log(6, "Camera2: CameraAccessException " + e);
                f318c = new String[0];
            }
        }
        return f318c;
    }

    /* JADX INFO: renamed from: d */
    public static int m345d(Context context, int i) {
        try {
            CameraCharacteristics cameraCharacteristics = m337b(context).getCameraCharacteristics(m344c(context)[i]);
            float[] fArr = (float[]) cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            SizeF sizeF = (SizeF) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            if (fArr.length > 0) {
                return (int) ((fArr[0] * 36.0f) / sizeF.getWidth());
            }
        } catch (CameraAccessException e) {
            C0915f.Log(6, "Camera2: CameraAccessException " + e);
        }
        return 0;
    }

    /* JADX INFO: renamed from: e */
    public static int[] m348e(Context context, int i) {
        try {
            Size[] sizeArrM336a = m336a(m337b(context).getCameraCharacteristics(m344c(context)[i]));
            if (sizeArrM336a == null) {
                return null;
            }
            int[] iArr = new int[sizeArrM336a.length * 2];
            for (int i2 = 0; i2 < sizeArrM336a.length; i2++) {
                int i3 = i2 * 2;
                iArr[i3] = sizeArrM336a[i2].getWidth();
                iArr[i3 + 1] = sizeArrM336a[i2].getHeight();
            }
            return iArr;
        } catch (CameraAccessException e) {
            C0915f.Log(6, "Camera2: CameraAccessException " + e);
            return null;
        }
    }

    /* JADX INFO: renamed from: g */
    private void m351g() {
        HandlerThread handlerThread = new HandlerThread("CameraBackground");
        this.f326f = handlerThread;
        handlerThread.start();
        this.f327g = new Handler(this.f326f.getLooper());
    }

    /* JADX INFO: renamed from: h */
    private void m354h() {
        this.f326f.quit();
        try {
            this.f326f.join(4000L);
            this.f326f = null;
            this.f327g = null;
        } catch (InterruptedException e) {
            this.f326f.interrupt();
            C0915f.Log(6, "Camera2: Interrupted while waiting for the background thread to finish " + e);
        }
    }

    /* JADX INFO: renamed from: i */
    private void m356i() {
        try {
            if (!f319e.tryAcquire(4L, TimeUnit.SECONDS)) {
                C0915f.Log(5, "Camera2: Timeout waiting to lock camera for closing.");
                return;
            }
            this.f325d.close();
            try {
                if (!f319e.tryAcquire(4L, TimeUnit.SECONDS)) {
                    C0915f.Log(5, "Camera2: Timeout waiting to close camera.");
                }
            } catch (InterruptedException e) {
                C0915f.Log(6, "Camera2: Interrupted while waiting to close camera " + e);
            }
            this.f325d = null;
            f319e.release();
        } catch (InterruptedException e2) {
            C0915f.Log(6, "Camera2: Interrupted while trying to lock camera for closing " + e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: j */
    public void m357j() {
        try {
            if (this.f331k != 0 && this.f332l >= 0.0f && this.f332l <= 1.0f && this.f333m >= 0.0f && this.f333m <= 1.0f) {
                this.f336p = true;
                this.f340t.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(Math.max(this.f330j + 1, Math.min((int) (((this.f329i.width() - (this.f334n * 2)) * this.f332l) + this.f334n), (this.f329i.width() - this.f330j) - 1)) - this.f330j, Math.max(this.f330j + 1, Math.min((int) ((((double) (this.f329i.height() - (this.f335o * 2))) * (1.0d - ((double) this.f333m))) + ((double) this.f335o)), (this.f329i.height() - this.f330j) - 1)) - this.f330j, this.f330j * 2, this.f330j * 2, 999)});
                this.f340t.set(CaptureRequest.CONTROL_AF_MODE, 1);
                this.f340t.set(CaptureRequest.CONTROL_AF_TRIGGER, 1);
                this.f340t.setTag("Focus");
                this.f341u.capture(this.f340t.build(), this.f320A, this.f327g);
                return;
            }
            this.f340t.set(CaptureRequest.CONTROL_AF_MODE, 4);
            this.f340t.setTag("Regular");
            if (this.f341u != null) {
                this.f341u.setRepeatingRequest(this.f340t.build(), this.f320A, this.f327g);
            }
        } catch (CameraAccessException e) {
            C0915f.Log(6, "Camera2: CameraAccessException " + e);
        }
    }

    /* JADX INFO: renamed from: k */
    private void m358k() {
        try {
            if (this.f341u != null) {
                this.f341u.stopRepeating();
                this.f340t.set(CaptureRequest.CONTROL_AF_TRIGGER, 2);
                this.f340t.set(CaptureRequest.CONTROL_AF_MODE, 0);
                this.f340t.setTag("Cancel focus");
                this.f341u.capture(this.f340t.build(), this.f320A, this.f327g);
            }
        } catch (CameraAccessException e) {
            C0915f.Log(6, "Camera2: CameraAccessException " + e);
        }
    }

    /* JADX INFO: renamed from: a */
    public final Rect m359a() {
        return this.f328h;
    }

    /* JADX INFO: renamed from: a */
    public final boolean m360a(float f, float f2) {
        if (this.f331k <= 0) {
            return false;
        }
        if (this.f336p) {
            C0915f.Log(5, "Camera2: Setting manual focus point already started.");
            return false;
        }
        this.f332l = f;
        this.f333m = f2;
        synchronized (this.f342v) {
            if (this.f341u != null && this.f346z != a.f353b) {
                m358k();
            }
        }
        return true;
    }

    /* JADX INFO: renamed from: a */
    public final boolean m361a(Context context, int i, int i2, int i3, int i4, int i5) {
        try {
            CameraCharacteristics cameraCharacteristics = f317b.getCameraCharacteristics(m344c(context)[i]);
            if (((Integer) cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).intValue() == 2) {
                C0915f.Log(5, "Camera2: only LEGACY hardware level is supported.");
                return false;
            }
            Size[] sizeArrM336a = m336a(cameraCharacteristics);
            if (sizeArrM336a != null && sizeArrM336a.length != 0) {
                this.f328h = m327a(sizeArrM336a, i2, i3);
                Range[] rangeArr = (Range[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                if (rangeArr != null && rangeArr.length != 0) {
                    int iM326a = m326a(rangeArr, i4);
                    this.f337q = new Range(Integer.valueOf(iM326a), Integer.valueOf(iM326a));
                    try {
                        if (!f319e.tryAcquire(4L, TimeUnit.SECONDS)) {
                            C0915f.Log(5, "Camera2: Timeout waiting to lock camera for opening.");
                            return false;
                        }
                        try {
                            f317b.openCamera(m344c(context)[i], this.f321B, this.f327g);
                            if (!f319e.tryAcquire(4L, TimeUnit.SECONDS)) {
                                C0915f.Log(5, "Camera2: Timeout waiting to open camera.");
                                return false;
                            }
                            f319e.release();
                            this.f343w = i5;
                            m339b(cameraCharacteristics);
                            return this.f325d != null;
                        } catch (CameraAccessException e2) {
                            C0915f.Log(6, "Camera2: CameraAccessException " + e2);
                            f319e.release();
                            return false;
                        }
                    } catch (InterruptedException e3) {
                        C0915f.Log(6, "Camera2: Interrupted while trying to lock camera for opening " + e3);
                        return false;
                    }
                }
                C0915f.Log(6, "Camera2: target FPS ranges are not avialable.");
            }
            return false;
        } catch (CameraAccessException e4) {
            C0915f.Log(6, "Camera2: CameraAccessException " + e4);
            return false;
        }
    }

    /* JADX INFO: renamed from: b */
    public final void m362b() {
        if (this.f325d != null) {
            m365e();
            m356i();
            this.f320A = null;
            this.f345y = null;
            this.f344x = null;
            Image image = this.f339s;
            if (image != null) {
                image.close();
                this.f339s = null;
            }
            ImageReader imageReader = this.f338r;
            if (imageReader != null) {
                imageReader.close();
                this.f338r = null;
            }
        }
        m354h();
    }

    /* JADX INFO: renamed from: c */
    public final void m363c() {
        if (this.f338r == null) {
            ImageReader imageReaderNewInstance = ImageReader.newInstance(this.f328h.width(), this.f328h.height(), 35, 2);
            this.f338r = imageReaderNewInstance;
            imageReaderNewInstance.setOnImageAvailableListener(this.f322C, this.f327g);
            this.f339s = null;
            if (this.f343w != 0) {
                SurfaceTexture surfaceTexture = new SurfaceTexture(this.f343w);
                this.f344x = surfaceTexture;
                surfaceTexture.setDefaultBufferSize(this.f328h.width(), this.f328h.height());
                this.f344x.setOnFrameAvailableListener(this.f323D, this.f327g);
                this.f345y = new Surface(this.f344x);
            }
        }
        try {
            if (this.f341u == null) {
                this.f325d.createCaptureSession(this.f345y != null ? Arrays.asList(this.f345y, this.f338r.getSurface()) : Arrays.asList(this.f338r.getSurface()), new CameraCaptureSession.StateCallback() { // from class: com.unity3d.player.c.2
                    @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
                    public final void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                        C0915f.Log(6, "Camera2: CaptureSession configuration failed.");
                    }

                    @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
                    public final void onConfigured(CameraCaptureSession cameraCaptureSession) {
                        String str;
                        if (C0912c.this.f325d == null) {
                            return;
                        }
                        synchronized (C0912c.this.f342v) {
                            C0912c.this.f341u = cameraCaptureSession;
                            try {
                                C0912c.this.f340t = C0912c.this.f325d.createCaptureRequest(1);
                                if (C0912c.this.f345y != null) {
                                    C0912c.this.f340t.addTarget(C0912c.this.f345y);
                                }
                                C0912c.this.f340t.addTarget(C0912c.this.f338r.getSurface());
                                C0912c.this.f340t.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, C0912c.this.f337q);
                                C0912c.this.m357j();
                            } catch (CameraAccessException e) {
                                str = "Camera2: CameraAccessException " + e;
                                C0915f.Log(6, str);
                            } catch (IllegalStateException e2) {
                                str = "Camera2: IllegalStateException " + e2;
                                C0915f.Log(6, str);
                            }
                        }
                    }
                }, this.f327g);
            } else if (this.f346z == a.f353b) {
                this.f341u.setRepeatingRequest(this.f340t.build(), this.f320A, this.f327g);
            }
            this.f346z = a.f352a;
        } catch (CameraAccessException e) {
            C0915f.Log(6, "Camera2: CameraAccessException " + e);
        }
    }

    /* JADX INFO: renamed from: d */
    public final void m364d() {
        synchronized (this.f342v) {
            if (this.f341u != null) {
                try {
                    this.f341u.stopRepeating();
                    this.f346z = a.f353b;
                } catch (CameraAccessException e) {
                    C0915f.Log(6, "Camera2: CameraAccessException " + e);
                }
            }
        }
    }

    /* JADX INFO: renamed from: e */
    public final void m365e() {
        synchronized (this.f342v) {
            if (this.f341u != null) {
                try {
                    this.f341u.abortCaptures();
                } catch (CameraAccessException e) {
                    C0915f.Log(6, "Camera2: CameraAccessException " + e);
                }
                this.f341u.close();
                this.f341u = null;
                this.f346z = a.f354c;
            }
        }
    }
}
