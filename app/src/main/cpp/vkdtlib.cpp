#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include "core/log.h"
#include "core/threads.h"
#include "qvk/qvk.h"
#include "pipe/global.h"
#include "pipe/graph.h"
#include "pipe/graph-io.h"
}

static void vkdt_android_logger(dt_log_mask_t mask, const char *fmt, va_list ap) {
    __android_log_vprint((mask & s_log_err) ? ANDROID_LOG_ERROR : ANDROID_LOG_DEBUG,
                         "vkdt", fmt, ap);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtLib_initVkdtLib(JNIEnv *env, jclass thiz,
                                                                    jstring base_path) {
    int err;
    dt_log_init(s_log_all);
    dt_log_global.handler = vkdt_android_logger;
    const char *base_path_str = env->GetStringUTFChars(base_path, nullptr);
    err = dt_pipe_global_init_basedir(base_path_str);
    env->ReleaseStringUTFChars(base_path, base_path_str);
    if (err != 0) {
        return err;
    }
    threads_global_init();

    VkResult res = qvk_init(nullptr, -1);
    return res;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtLib_closeVkdtLib(JNIEnv *env, jclass thiz) {
    threads_shutdown();
    threads_global_cleanup();
    dt_pipe_global_cleanup();
    qvk_cleanup();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_initGraph(JNIEnv *env, jclass clazz
) {
    auto *graph = (dt_graph_t *)malloc(sizeof(dt_graph_t));
    dt_graph_init(graph);
    return (long)graph;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_cleanupGraph(JNIEnv *env, jclass clazz,
                                                                       jlong native_graph) {
    auto *graph = (dt_graph_t *)native_graph;
    dt_graph_cleanup(graph);
    free(graph);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_loadConfigLines(JNIEnv *env,
                                                                               jclass clazz,
                                                                               jlong native_graph,
                                                                               jobjectArray lines) {
    auto *graph = (dt_graph_t *)native_graph;
    int n = env->GetArrayLength(lines);
    for (int i = 0; i < n; i++) {
        auto jline = (jstring)env->GetObjectArrayElement(lines, i);
        const char *line = env->GetStringUTFChars(jline, nullptr);
        char buf[300000] = {0}; // workaround - need a char, not a const char
        strncpy(buf, line, 299999);
        int err = dt_graph_read_config_line(graph, buf);
        if (err < 0) {
            return err;
        }
        env->ReleaseStringUTFChars(jline, line);
    }
    return 0;
}