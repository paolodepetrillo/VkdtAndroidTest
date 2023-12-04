#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include "core/log.h"
#include "qvk/qvk.h"
#include "pipe/global.h"
}

static void vkdt_android_logger(dt_log_mask_t mask, const char *fmt, va_list ap) {
    __android_log_vprint((mask & s_log_err) ? ANDROID_LOG_ERROR : ANDROID_LOG_DEBUG,
                         "vkdt", fmt, ap);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */, jstring base_path) {
    dt_log_init(s_log_all);
    dt_log_global.handler = vkdt_android_logger;
    const char *base_path_str = env->GetStringUTFChars(base_path, nullptr);
    strcpy(dt_pipe.basedir, base_path_str);
    env->ReleaseStringUTFChars(base_path, base_path_str);
    dt_pipe_global_init();

    VkResult res = qvk_init(0, -1);
    std::string hello = "Hello from C++ " + std::to_string(res);
    return env->NewStringUTF(hello.c_str());
}