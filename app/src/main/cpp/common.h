#pragma once

#include <android/log.h>

#define MOD_TAG "ModHooks"
#define MOD_LOGI(...) __android_log_print(ANDROID_LOG_INFO, MOD_TAG, __VA_ARGS__)
#define MOD_LOGW(...) __android_log_print(ANDROID_LOG_WARN, MOD_TAG, __VA_ARGS__)
#define MOD_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, MOD_TAG, __VA_ARGS__)
