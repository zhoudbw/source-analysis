package c_jni;

/**
 * With Java code call file of c "HelloWorld"
 * @author zhoudbw
 */
public class HelloJni {
    static {
        System.loadLibrary("HelloJni");
    }
    public static native void sayHello();

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        new HelloJni().sayHello();
    }
}
