#include <jni.h>
#include <string>

extern "C" {
#include "qvk/qvk.h"
#include "pipe/global.h"
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    VkResult res = qvk_init(0, -1);
    std::string hello = "Hello from C++ " + std::to_string(res);
    return env->NewStringUTF(hello.c_str());
}