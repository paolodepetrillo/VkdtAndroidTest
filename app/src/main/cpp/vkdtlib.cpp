#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include "core/log.h"
#include "core/threads.h"
#include "qvk/qvk.h"
#include "pipe/global.h"
#include "pipe/graph.h"
#include "pipe/graph-export.h"
#include "pipe/graph-io.h"
#include "pipe/modules/api.h"
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

extern "C"
JNIEXPORT jint JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_setParamString(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jlong native_graph,
                                                                              jlong name,
                                                                              jlong inst,
                                                                              jlong param,
                                                                              jstring value) {
    auto *graph = (dt_graph_t *)native_graph;
    int err;
    err = dt_module_get(graph, name, inst);
    if (err < 0) {
        return err;
    }
    int modid = err;
    const char *value_str = env->GetStringUTFChars(value, nullptr);
    err = dt_module_set_param_string(&graph->module[modid], param, value_str);
    env->ReleaseStringUTFChars(value, value_str);
    return err;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_testExport(JNIEnv *env, jclass clazz,
                                                                          jlong native_graph,
                                                                          jstring out_path) {
    auto *graph = (dt_graph_t *)native_graph;
    int err;
    err = dt_graph_replace_display(graph, 0, 0, 0);
    if (err != 0) {
        __android_log_print(ANDROID_LOG_INFO, "vkdt", "replace display fail %d", err);
        return;
    }
    dt_graph_disconnect_display_modules(graph);
    int mod_out_id = dt_module_get(graph, dt_token("o-jpg"), dt_token("main"));
    dt_module_t *mod_out = &graph->module[mod_out_id];
    const char *out_path_str = env->GetStringUTFChars(out_path, nullptr);
    err = dt_module_set_param_string(mod_out, dt_token("filename"), out_path_str);
    if (err != 0) {
        __android_log_print(ANDROID_LOG_INFO, "vkdt", "set output filename fail %d", err);
        return;
    }
    env->ReleaseStringUTFChars(out_path, out_path_str);
    VkResult res = dt_graph_run(graph, s_graph_run_all);
    __android_log_print(ANDROID_LOG_INFO, "vkdt", "graph run %d", res);
}