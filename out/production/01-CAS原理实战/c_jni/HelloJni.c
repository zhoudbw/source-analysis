#include "c_jni_HelloJni.h"
#include <stdio.h>

JNIEXPORT void JNICALL Java_c_1jni_HelloJni_sayHello (JNIEnv *env, jclass jc) {
    printf("HelloJni");
}