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

static int module_get(const dt_graph_t *graph, dt_token_t name, dt_token_t inst) {
    int err = dt_module_get(graph, name, inst);
    if (err < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt",
                            "cannot find module %" PRItkn ":%" PRItkn,
                            dt_token_str(name), dt_token_str(inst));
    }
    return err;
}

static int module_get_param(dt_module_t *mod, long param) {
    int err = dt_module_get_param(mod->so, param);
    if (err < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt",
                            "cannot find module %" PRItkn " parameter %" PRItkn,
                            dt_token_str(mod->so->name), dt_token_str(param));
    }
    return err;
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

    VkResult res = qvk_init(nullptr, -1, 0);
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
    dt_graph_init(graph, s_queue_compute);
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
                                                                              jstring value,
                                                                              jlongArray run_flags) {
    auto *graph = (dt_graph_t *)native_graph;
    int err;
    err = module_get(graph, name, inst);
    if (err < 0) {
        return err;
    }
    int modid = err;
    const char *value_str = env->GetStringUTFChars(value, nullptr);
    err = dt_module_set_param_string(&graph->module[modid], param, value_str);
    env->ReleaseStringUTFChars(value, value_str);

    long *rf = env->GetLongArrayElements(run_flags, nullptr);
    rf[0] = s_graph_run_all;
    env->ReleaseLongArrayElements(run_flags, rf, 0);

    return err;
}

/*
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
    int mod_out_id = module_get(graph, dt_token("o-jpg"), dt_token("main"));
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
 */

typedef struct {
    JNIEnv *env;
    jobject vkdt_graph;
} ocback_param_t;

static void vkdt_android_output_callback(void *param, dt_token_t inst, int wd, int ht, const uint8_t *data) {
    __android_log_print(ANDROID_LOG_INFO, "vkdt", "callback %d %d", wd, ht);
    int err;
    auto *p = (ocback_param_t *)param;

    jclass cls = p->env->GetObjectClass(p->vkdt_graph);
    jmethodID mid = p->env->GetMethodID(cls, "getBitmapForOutput", "(JII)Landroid/graphics/Bitmap;");
    jobject bitmap = p->env->CallObjectMethod(p->vkdt_graph, mid, inst, wd, ht);

    AndroidBitmapInfo bitmap_info;
    err = AndroidBitmap_getInfo(p->env, bitmap, &bitmap_info);
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
    err = AndroidBitmap_lockPixels(p->env, bitmap, (void **)&pixels);
    if (err != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "Failed to lock pixels %d", err);
        return;
    }

    memcpy(pixels, data, wd * ht * 8);
    __android_log_print(ANDROID_LOG_INFO, "vkdt", "Copied pixels to bitmap");

    err = AndroidBitmap_unlockPixels(p->env, bitmap);
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
    const int mid = module_get(graph, dt_token("display"), dt_token("main"));
    if (mid < 0) {
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
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_runGraph(JNIEnv *env, jobject thiz,
                                                                        jlong native_graph,
                                                                        jlong run_flags
) {
    auto *graph = (dt_graph_t *)native_graph;
    int err;

    __android_log_print(ANDROID_LOG_INFO, "vkdt", "Run graph with flags %lx", run_flags);

    const int mid = module_get(graph, dt_token("o-cback"), dt_token("main"));
    if (mid < 0) {
        return 1;
    }
    ocback_param_t ocback_param;
    dt_ocback_data_t ocback_data;
    ocback_data.callback = vkdt_android_output_callback;
    ocback_data.param = &ocback_param;
    ocback_param.env = env;
    ocback_param.vkdt_graph = thiz;
    graph->module[mid].data = &ocback_data;

    VkResult res = dt_graph_run(graph, run_flags);
    __android_log_print(ANDROID_LOG_INFO, "vkdt", "graph run %d", res);

    graph->module[mid].data = nullptr;

    return res;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_github_paolodepetrillo_vkdtandroidtest_vkdt_VkdtGraph_setParamFloat(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong native_graph,
                                                                             jlong name, jlong inst,
                                                                             jlong param,
                                                                             jint num,
                                                                             jfloat new_value,
                                                                             jlongArray run_flags) {
    auto *graph = (dt_graph_t *) native_graph;
    int modid = module_get(graph, name, inst);
    if (modid < 0) {
        return modid;
    }
    dt_module_t *mod = &graph->module[modid];
    int parid = module_get_param(mod, param);
    if (parid < 0) {
        return parid;
    }
    dt_ui_param_t *ui = mod->so->param[parid];
    if (num >= ui->cnt) {
        __android_log_print(ANDROID_LOG_ERROR, "vkdt", "Index %d out of range %d", num, ui->cnt);
        return -1;
    }

    float *val = (float *)(mod->param + mod->so->param[parid]->offset) + num;
    float oldval = *val;
    *val = new_value;

    long *rf = env->GetLongArrayElements(run_flags, nullptr);
    rf[0] = s_graph_run_record_cmd_buf;
    if (mod->so->check_params) {
        rf[0] |= mod->so->check_params(mod, parid, num, &oldval);
    }
    env->ReleaseLongArrayElements(run_flags, rf, 0);

    return 0;
}
