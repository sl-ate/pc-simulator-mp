package bitter.jnibridge;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/* JADX INFO: loaded from: classes.dex */
public class JNIBridge {

    /* JADX INFO: renamed from: bitter.jnibridge.JNIBridge$a */
    private static class C0343a implements InvocationHandler {

        /* JADX INFO: renamed from: a */
        private Object f24a = new Object[0];

        /* JADX INFO: renamed from: b */
        private long f25b;

        /* JADX INFO: renamed from: c */
        private Constructor f26c;

        public C0343a(long j) {
            this.f25b = j;
            try {
                Constructor declaredConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Integer.TYPE);
                this.f26c = declaredConstructor;
                declaredConstructor.setAccessible(true);
            } catch (NoClassDefFoundError unused) {
                this.f26c = null;
            } catch (NoSuchMethodException unused2) {
                this.f26c = null;
            }
        }

        /* JADX INFO: renamed from: a */
        private Object m31a(Object obj, Method method, Object[] objArr) {
            if (objArr == null) {
                objArr = new Object[0];
            }
            Class<?> declaringClass = method.getDeclaringClass();
            try {
                return ((MethodHandles.Lookup) this.f26c.newInstance(declaringClass, 2)).in(declaringClass).unreflectSpecial(method, declaringClass).bindTo(obj).invokeWithArguments(objArr);
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }
        }

        /* JADX INFO: renamed from: a */
        public final void m32a() {
            synchronized (this.f24a) {
                this.f25b = 0L;
            }
        }

        public final void finalize() {
            synchronized (this.f24a) {
                if (this.f25b == 0) {
                    return;
                }
                JNIBridge.delete(this.f25b);
            }
        }

        @Override // java.lang.reflect.InvocationHandler
        public final Object invoke(Object obj, Method method, Object[] objArr) {
            synchronized (this.f24a) {
                if (this.f25b == 0) {
                    return null;
                }
                try {
                    return JNIBridge.invoke(this.f25b, method.getDeclaringClass(), method, objArr);
                } catch (NoSuchMethodError e) {
                    if (this.f26c == null) {
                        System.err.println("JNIBridge error: Java interface default methods are only supported since Android Oreo");
                        throw e;
                    }
                    if ((method.getModifiers() & 1024) == 0) {
                        return m31a(obj, method, objArr);
                    }
                    throw e;
                }
            }
        }
    }

    static native void delete(long j);

    static void disableInterfaceProxy(Object obj) {
        if (obj != null) {
            ((C0343a) Proxy.getInvocationHandler(obj)).m32a();
        }
    }

    static native Object invoke(long j, Class cls, Method method, Object[] objArr);

    static Object newInterfaceProxy(long j, Class[] clsArr) {
        return Proxy.newProxyInstance(JNIBridge.class.getClassLoader(), clsArr, new C0343a(j));
    }
}
