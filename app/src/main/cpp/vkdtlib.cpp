#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>

extern "C" {
#include "core/log.h"
#include "core/threads.h"
#include "qvk/qvk.h"
#include "pipe/connector.h"
#include "pipe/global.h"
#include "pipe/graph.h"
#include "pipe/graph-export.h"
#include "pipe/graph-io.h"
#include "pipe/module.h"
#include "pipe/modules/api.h"
#include "pipe/modules/o-cback/callback.h"
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
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "replace display fail %d", err);
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

typedef struct {
    JNIEnv *env;
    jobject bitmap;
} ocback_param_t;

static void vkdt_android_output_callback(void *param, dt_token_t inst, int wd, int ht, const uint8_t *data) {
    __android_log_print(ANDROID_LOG_INFO, "vkdt", "callback %d %d", wd, ht);
    int err;
    auto *p = (ocback_param_t *)param;

    AndroidBitmapInfo bitmap_info;
    err = AndroidBitmap_getInfo(p->env, p->bitmap, &bitmap_info);
    if (err != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "Failed to get bitmap info %d", err);
        return;
    }

    if (bitmap_info.width != wd || bitmap_info.height != ht) {
        __android_log_print(ANDROID_LOG_INFO, "vkdt", "Bitmap size mismatch - is %dx%d but needs %dx%d",
                            bitmap_info.width, bitmap_info.height, wd, ht);
        return;
    }

    uint8_t *pixels;
    err = AndroidBitmap_lockPixels(p->env, p->bitmap, (void **)&pixels);
    if (err != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "Failed to lock pixels %d", err);
        return;
    }

    memcpy(pixels, data, wd * ht * 8);
    __android_log_print(ANDROID_LOG_INFO, "vkdt", "Copied pixels to bitmap");

    err = AndroidBitmap_unlockPixels(p->env, p->bitmap);
    if (err != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "Failed to unlock pixels %d", err);
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_replaceDisplayWithCback(JNIEnv *env,
                                                                                       jclass clazz,
                                                                                       jlong native_graph) {
    auto *graph = (dt_graph_t *)native_graph;
    const int mid = dt_module_get(graph, dt_token("display"), dt_token("main"));
    if (mid < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "cannot find display:main module");
        return 1;
    }
    const int cid = dt_module_get_connector(graph->module + mid, dt_token("input"));
    const int m0 = graph->module[mid].connector[cid].connected_mi;
    const int o0 = graph->module[mid].connector[cid].connected_mc;
    if (m0 < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "display input is not connected");
        return 2;
    }
    const int m1 = dt_module_add(graph, dt_token("o-cback"), dt_token("main"));
    const int i1 = dt_module_get_connector(graph->module + m1, dt_token("input"));
    CONN(dt_module_connect(graph, m0, o0, m1, i1));
    dt_graph_disconnect_display_modules(graph);
    graph->module[m1].data = (void *)vkdt_android_output_callback;
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_runGraph(JNIEnv *env, jclass clazz,
                                                                        jlong native_graph,
                                                                        jobject main_bitmap) {
    auto *graph = (dt_graph_t *)native_graph;
    int err;

    const int mid = dt_module_get(graph, dt_token("o-cback"), dt_token("main"));
    if (mid < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "cannot find o-cback:main module");
        return 1;
    }
    jobject bmp = env->NewLocalRef(main_bitmap);
    ocback_param_t ocback_param;
    dt_ocback_data_t ocback_data;
    ocback_data.callback = vkdt_android_output_callback;
    ocback_data.param = &ocback_param;
    if (bmp != nullptr) {
        ocback_param.env = env;
        ocback_param.bitmap = bmp;
        graph->module[mid].data = &ocback_data;
    } else {
        graph->module[mid].data = nullptr;
    }

    VkResult res = dt_graph_run(graph, s_graph_run_all);
    __android_log_print(ANDROID_LOG_INFO, "vkdt", "graph run %d", res);
    return res;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_runGraphForRoi(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jlong native_graph,
                                                                              jintArray size) {
    auto *graph = (dt_graph_t *)native_graph;
    VkResult res = dt_graph_run(graph, s_graph_run_roi);
    if (res != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "Failed to run graph for roi %d", res);
        return res;
    }

    const int mid = dt_module_get(graph, dt_token("o-cback"), dt_token("main"));
    if (mid < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "cannot find o-cback:main module");
        return 1;
    }

    int *wh = env->GetIntArrayElements(size, nullptr);
    wh[0] = (int)graph->module[mid].connector[0].roi.full_wd;
    wh[1] = (int)graph->module[mid].connector[0].roi.full_ht;
    env->ReleaseIntArrayElements(size, wh, 0);
    return 0;
}