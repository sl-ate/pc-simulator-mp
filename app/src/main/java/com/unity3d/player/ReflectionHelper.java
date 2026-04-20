package com.unity3d.player;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;

/* JADX INFO: loaded from: classes2.dex */
final class ReflectionHelper {
    protected static boolean LOG = false;
    protected static final boolean LOGV = false;

    /* JADX INFO: renamed from: a */
    private static C0878a[] f184a = new C0878a[4096];

    /* JADX INFO: renamed from: b */
    private static long f185b = 0;

    /* JADX INFO: renamed from: c */
    private static long f186c = 0;

    /* JADX INFO: renamed from: d */
    private static boolean f187d = false;

    /* JADX INFO: renamed from: com.unity3d.player.ReflectionHelper$a */
    private static class C0878a {

        /* JADX INFO: renamed from: a */
        public volatile Member f193a;

        /* JADX INFO: renamed from: b */
        private final Class f194b;

        /* JADX INFO: renamed from: c */
        private final String f195c;

        /* JADX INFO: renamed from: d */
        private final String f196d;

        /* JADX INFO: renamed from: e */
        private final int f197e;

        C0878a(Class cls, String str, String str2) {
            this.f194b = cls;
            this.f195c = str;
            this.f196d = str2;
            this.f197e = ((((cls.hashCode() + 527) * 31) + this.f195c.hashCode()) * 31) + this.f196d.hashCode();
        }

        public final boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof C0878a) {
                C0878a c0878a = (C0878a) obj;
                if (this.f197e == c0878a.f197e && this.f196d.equals(c0878a.f196d) && this.f195c.equals(c0878a.f195c) && this.f194b.equals(c0878a.f194b)) {
                    return true;
                }
            }
            return false;
        }

        public final int hashCode() {
            return this.f197e;
        }
    }

    /* JADX INFO: renamed from: com.unity3d.player.ReflectionHelper$b */
    protected interface InterfaceC0879b extends InvocationHandler {
        /* JADX INFO: renamed from: a */
        void mo287a(long j, boolean z);
    }

    ReflectionHelper() {
    }

    /* JADX INFO: renamed from: a */
    private static float m276a(Class cls, Class cls2) {
        if (cls.equals(cls2)) {
            return 1.0f;
        }
        if (cls.isPrimitive() || cls2.isPrimitive()) {
            return 0.0f;
        }
        try {
            if (cls.asSubclass(cls2) != null) {
                return 0.5f;
            }
        } catch (ClassCastException unused) {
        }
        try {
            return cls2.asSubclass(cls) != null ? 0.1f : 0.0f;
        } catch (ClassCastException unused2) {
            return 0.0f;
        }
    }

    /* JADX INFO: renamed from: a */
    private static float m277a(Class cls, Class[] clsArr, Class[] clsArr2) {
        if (clsArr2.length == 0) {
            return 0.1f;
        }
        int i = 0;
        if ((clsArr == null ? 0 : clsArr.length) + 1 != clsArr2.length) {
            return 0.0f;
        }
        float f = 1.0f;
        if (clsArr != null) {
            int length = clsArr.length;
            int i2 = 0;
            float fM276a = 1.0f;
            while (i < length) {
                fM276a *= m276a(clsArr[i], clsArr2[i2]);
                i++;
                i2++;
            }
            f = fM276a;
        }
        return f * m276a(cls, clsArr2[clsArr2.length - 1]);
    }

    /* JADX INFO: renamed from: a */
    private static Class m279a(String str, int[] iArr) {
        while (iArr[0] < str.length()) {
            int i = iArr[0];
            iArr[0] = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt != '(' && cCharAt != ')') {
                if (cCharAt == 'L') {
                    int iIndexOf = str.indexOf(59, iArr[0]);
                    if (iIndexOf == -1) {
                        return null;
                    }
                    String strSubstring = str.substring(iArr[0], iIndexOf);
                    iArr[0] = iIndexOf + 1;
                    try {
                        return Class.forName(strSubstring.replace('/', '.'));
                    } catch (ClassNotFoundException unused) {
                        return null;
                    }
                }
                if (cCharAt == 'Z') {
                    return Boolean.TYPE;
                }
                if (cCharAt == 'I') {
                    return Integer.TYPE;
                }
                if (cCharAt == 'F') {
                    return Float.TYPE;
                }
                if (cCharAt == 'V') {
                    return Void.TYPE;
                }
                if (cCharAt == 'B') {
                    return Byte.TYPE;
                }
                if (cCharAt == 'C') {
                    return Character.TYPE;
                }
                if (cCharAt == 'S') {
                    return Short.TYPE;
                }
                if (cCharAt == 'J') {
                    return Long.TYPE;
                }
                if (cCharAt == 'D') {
                    return Double.TYPE;
                }
                if (cCharAt == '[') {
                    return Array.newInstance((Class<?>) m279a(str, iArr), 0).getClass();
                }
                C0915f.Log(5, "! parseType; " + cCharAt + " is not known!");
                return null;
            }
        }
        return null;
    }

    /* JADX INFO: renamed from: a */
    private static synchronized void m282a(C0878a c0878a, Member member) {
        c0878a.f193a = member;
        f184a[c0878a.hashCode() & (f184a.length - 1)] = c0878a;
    }

    /* JADX INFO: renamed from: a */
    private static synchronized boolean m283a(C0878a c0878a) {
        C0878a c0878a2 = f184a[c0878a.hashCode() & (f184a.length - 1)];
        if (!c0878a.equals(c0878a2)) {
            return false;
        }
        c0878a.f193a = c0878a2.f193a;
        return true;
    }

    /* JADX INFO: renamed from: a */
    private static Class[] m284a(String str) {
        Class clsM279a;
        int i = 0;
        int[] iArr = {0};
        ArrayList arrayList = new ArrayList();
        while (iArr[0] < str.length() && (clsM279a = m279a(str, iArr)) != null) {
            arrayList.add(clsM279a);
        }
        Class[] clsArr = new Class[arrayList.size()];
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            clsArr[i] = (Class) it.next();
            i++;
        }
        return clsArr;
    }

    protected static synchronized boolean beginProxyCall(long j) {
        boolean z;
        if (j == f185b) {
            f186c++;
            z = true;
        } else {
            z = false;
        }
        return z;
    }

    protected static synchronized void endProxyCall() {
        long j = f186c - 1;
        f186c = j;
        if (0 == j && f187d) {
            ReflectionHelper.class.notifyAll();
        }
    }

    protected static synchronized void endUnityLaunch() {
        try {
            f185b++;
            f187d = true;
            while (f186c > 0) {
                ReflectionHelper.class.wait();
            }
        } catch (InterruptedException unused) {
            C0915f.Log(6, "Interrupted while waiting for all proxies to exit.");
        }
        f187d = false;
    }

    protected static Constructor getConstructorID(Class cls, String str) {
        Constructor<?> constructor;
        C0878a c0878a = new C0878a(cls, "", str);
        if (m283a(c0878a)) {
            constructor = (Constructor) c0878a.f193a;
        } else {
            Class[] clsArrM284a = m284a(str);
            float f = 0.0f;
            Constructor<?> constructor2 = null;
            for (Constructor<?> constructor3 : cls.getConstructors()) {
                float fM277a = m277a(Void.TYPE, constructor3.getParameterTypes(), clsArrM284a);
                if (fM277a > f) {
                    constructor2 = constructor3;
                    if (fM277a == 1.0f) {
                        break;
                    }
                    f = fM277a;
                }
            }
            m282a(c0878a, constructor2);
            constructor = constructor2;
        }
        if (constructor != null) {
            return constructor;
        }
        throw new NoSuchMethodError("<init>" + str + " in class " + cls.getName());
    }

    protected static Field getFieldID(Class cls, String str, String str2, boolean z) {
        Field field;
        Class superclass = cls;
        C0878a c0878a = new C0878a(superclass, str, str2);
        if (m283a(c0878a)) {
            field = (Field) c0878a.f193a;
        } else {
            Class[] clsArrM284a = m284a(str2);
            float f = 0.0f;
            Field field2 = null;
            while (superclass != null) {
                Field[] declaredFields = superclass.getDeclaredFields();
                int length = declaredFields.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    Field field3 = declaredFields[i];
                    if (z == Modifier.isStatic(field3.getModifiers()) && field3.getName().compareTo(str) == 0) {
                        float fM277a = m277a(field3.getType(), (Class[]) null, clsArrM284a);
                        if (fM277a > f) {
                            field2 = field3;
                            if (fM277a == 1.0f) {
                                f = fM277a;
                                break;
                            }
                            f = fM277a;
                        } else {
                            continue;
                        }
                    }
                    i++;
                }
                if (f == 1.0f || superclass.isPrimitive() || superclass.isInterface() || superclass.equals(Object.class) || superclass.equals(Void.TYPE)) {
                    break;
                }
                superclass = superclass.getSuperclass();
            }
            m282a(c0878a, field2);
            field = field2;
        }
        if (field != null) {
            return field;
        }
        Object[] objArr = new Object[4];
        objArr[0] = z ? "static" : "non-static";
        objArr[1] = str;
        objArr[2] = str2;
        objArr[3] = superclass.getName();
        throw new NoSuchFieldError(String.format("no %s field with name='%s' signature='%s' in class L%s;", objArr));
    }

    protected static String getFieldSignature(Field field) {
        Class<?> type = field.getType();
        if (type.isPrimitive()) {
            String name = type.getName();
            return "boolean".equals(name) ? "Z" : "byte".equals(name) ? "B" : "char".equals(name) ? "C" : "double".equals(name) ? "D" : "float".equals(name) ? "F" : "int".equals(name) ? "I" : "long".equals(name) ? "J" : "short".equals(name) ? "S" : name;
        }
        if (type.isArray()) {
            return type.getName().replace('.', '/');
        }
        return "L" + type.getName().replace('.', '/') + ";";
    }

    protected static Method getMethodID(Class cls, String str, String str2, boolean z) {
        Method method;
        C0878a c0878a = new C0878a(cls, str, str2);
        if (m283a(c0878a)) {
            method = (Method) c0878a.f193a;
        } else {
            Class[] clsArrM284a = m284a(str2);
            float f = 0.0f;
            Method method2 = null;
            while (cls != null) {
                Method[] declaredMethods = cls.getDeclaredMethods();
                int length = declaredMethods.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    Method method3 = declaredMethods[i];
                    if (z == Modifier.isStatic(method3.getModifiers()) && method3.getName().compareTo(str) == 0) {
                        float fM277a = m277a(method3.getReturnType(), method3.getParameterTypes(), clsArrM284a);
                        if (fM277a > f) {
                            method2 = method3;
                            if (fM277a == 1.0f) {
                                f = fM277a;
                                break;
                            }
                            f = fM277a;
                        } else {
                            continue;
                        }
                    }
                    i++;
                }
                if (f == 1.0f || cls.isPrimitive() || cls.isInterface() || cls.equals(Object.class) || cls.equals(Void.TYPE)) {
                    break;
                }
                cls = cls.getSuperclass();
            }
            m282a(c0878a, method2);
            method = method2;
        }
        if (method != null) {
            return method;
        }
        Object[] objArr = new Object[4];
        objArr[0] = z ? "static" : "non-static";
        objArr[1] = str;
        objArr[2] = str2;
        objArr[3] = cls.getName();
        throw new NoSuchMethodError(String.format("no %s method with name='%s' signature='%s' in class L%s;", objArr));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeProxyFinalize(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native Object nativeProxyInvoke(long j, String str, Object[] objArr);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeProxyLogJNIInvokeException(long j);

    protected static Object newProxyInstance(long j, Class cls) {
        return newProxyInstance(j, new Class[]{cls});
    }

    protected static Object newProxyInstance(final long j, final Class[] clsArr) {
        return Proxy.newProxyInstance(ReflectionHelper.class.getClassLoader(), clsArr, new InterfaceC0879b() { // from class: com.unity3d.player.ReflectionHelper.1

            /* JADX INFO: renamed from: c */
            private long f190c = ReflectionHelper.f185b;

            /* JADX INFO: renamed from: d */
            private long f191d;

            /* JADX INFO: renamed from: e */
            private boolean f192e;

            /* JADX INFO: renamed from: a */
            private Object m286a(Object obj, Method method, Object[] objArr) {
                if (objArr == null) {
                    objArr = new Object[0];
                }
                try {
                    Class<?> declaringClass = method.getDeclaringClass();
                    Constructor declaredConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Integer.TYPE);
                    declaredConstructor.setAccessible(true);
                    return ((MethodHandles.Lookup) declaredConstructor.newInstance(declaringClass, 2)).in(declaringClass).unreflectSpecial(method, declaringClass).bindTo(obj).invokeWithArguments(objArr);
                } catch (NoClassDefFoundError unused) {
                    C0915f.Log(6, String.format("Java interface default methods are only supported since Android Oreo", new Object[0]));
                    ReflectionHelper.nativeProxyLogJNIInvokeException(this.f191d);
                    return null;
                } catch (Throwable unused2) {
                    ReflectionHelper.nativeProxyLogJNIInvokeException(this.f191d);
                    return null;
                }
            }

            @Override // com.unity3d.player.ReflectionHelper.InterfaceC0879b
            /* JADX INFO: renamed from: a */
            public final void mo287a(long j2, boolean z) {
                this.f191d = j2;
                this.f192e = z;
            }

            protected final void finalize() throws Throwable {
                if (ReflectionHelper.beginProxyCall(this.f190c)) {
                    try {
                        ReflectionHelper.nativeProxyFinalize(j);
                    } finally {
                        ReflectionHelper.endProxyCall();
                        super.finalize();
                    }
                }
            }

            @Override // java.lang.reflect.InvocationHandler
            public final Object invoke(Object obj, Method method, Object[] objArr) {
                long j2;
                if (!ReflectionHelper.beginProxyCall(this.f190c)) {
                    C0915f.Log(6, "Scripting proxy object was destroyed, because Unity player was unloaded.");
                    return null;
                }
                try {
                    this.f191d = 0L;
                    this.f192e = false;
                    Object objNativeProxyInvoke = ReflectionHelper.nativeProxyInvoke(j, method.getName(), objArr);
                    if (!this.f192e) {
                        if (this.f191d != 0) {
                            j2 = this.f191d;
                        }
                        return objNativeProxyInvoke;
                    }
                    if ((method.getModifiers() & 1024) == 0) {
                        return m286a(obj, method, objArr);
                    }
                    j2 = this.f191d;
                    ReflectionHelper.nativeProxyLogJNIInvokeException(j2);
                    return objNativeProxyInvoke;
                } finally {
                    ReflectionHelper.endProxyCall();
                }
            }
        });
    }

    protected static void setNativeExceptionOnProxy(Object obj, long j, boolean z) {
        ((InterfaceC0879b) Proxy.getInvocationHandler(obj)).mo287a(j, z);
    }
}
