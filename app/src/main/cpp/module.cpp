#include <jni.h>

#include <android/choreographer.h>
#include <arpa/inet.h>
#include <dirent.h>
#include <fcntl.h>
#include <netdb.h>
#include <signal.h>
#include <sys/stat.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <time.h>

#include <algorithm>
#include <atomic>
#include <cctype>
#include <cerrno>
#include <chrono>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <dlfcn.h>
#include <fstream>
#include <mutex>
#include <set>
#include <sstream>
#include <string>
#include <thread>
#include <unordered_map>
#include <vector>

#include "common.h"
#include "chook.h"
#include "hooks.h"
#include "mp_hooks.h"
#include "patches.h"

namespace {
constexpr int kViewVisible = 0;
constexpr int kViewGone = 8;
constexpr int kLinearVertical = 1;
constexpr int kMatchParent = -1;
constexpr int kWrapContent = -2;
constexpr int kGravityCenter = 0x11;
constexpr int kGravityTopStart = 0x33;
constexpr int kAndroidProgressHorizontalStyle = 16842872;
constexpr jint kConnectButtonId = 0x4D500101;
constexpr long long kBootDurationNs = 2500000000LL;
constexpr int kDefaultPort = 27015;
constexpr bool kEnablePauseMenuActionHooks = false;

enum class UiPhase : int {
    kBootLoading = 0,
    kConnectMenu = 1,
    kConnecting = 2,
    kConnected = 3,
    kJoiningWorld = 4,
};

struct UiEvent {
    enum class Type {
        kStatus,
        kChat,
        kConnected,
        kDisconnected,
    };

    Type type = Type::kStatus;
    std::string text;
    int world_id = 1;
    double money = 0.0;
    double bitcoin = 0.0;
};

std::atomic<bool> g_module_initialized{false};
std::atomic<bool> g_ui_initialized{false};
std::atomic<bool> g_frame_loop_started{false};
std::atomic<int> g_ui_phase{static_cast<int>(UiPhase::kBootLoading)};
std::atomic<bool> g_connect_inflight{false};

JavaVM* g_vm = nullptr;

jobject g_activity_ref = nullptr;
jobject g_root_overlay = nullptr;
jobject g_loading_panel = nullptr;
jobject g_loading_progress = nullptr;
jobject g_loading_label = nullptr;
jobject g_connect_panel = nullptr;
jobject g_name_input = nullptr;
jobject g_ip_input = nullptr;
jobject g_connect_button = nullptr;
jobject g_status_label = nullptr;
jobject g_chat_label = nullptr;
jmethodID g_activity_pump_unity_tick = nullptr;

jclass g_unity_player_class = nullptr;
jmethodID g_unity_send_message = nullptr;

long long g_boot_started_ns = 0;
int g_last_boot_progress = -1;

std::mutex g_event_mutex;
std::vector<UiEvent> g_ui_events;
std::deque<std::string> g_chat_lines;

std::thread g_network_thread;
std::atomic<bool> g_network_stop{false};
std::atomic<int> g_network_socket{-1};
std::mutex g_network_mutex;
std::thread g_profile_sync_thread;
std::atomic<bool> g_profile_sync_stop{false};
std::mutex g_profile_sig_mutex;
std::string g_profile_last_signature;

std::string g_app_data_dir;
std::string g_external_files_dir;
std::string g_saves_dir;
std::string g_playerprefs_path;
std::string g_client_player_id;

std::string g_last_player_name = "Player";
std::string g_last_endpoint = "127.0.0.1:27015";

constexpr const char* kDefaultNativeCrashLogPath =
    "/sdcard/Android/data/com.Yiming.PC/files/mp_native_crash.log";
char g_native_crash_log_path[512] = {0};
std::atomic<bool> g_native_crash_handlers_installed{false};
volatile sig_atomic_t g_native_crash_in_handler = 0;

void* g_libandroid_handle = nullptr;
AChoreographer* g_choreographer = nullptr;
using ChoreoGetInstanceFn = AChoreographer* (*)();
using ChoreoPostFrameFn = void (*)(AChoreographer*, AChoreographer_frameCallback, void*);
ChoreoGetInstanceFn g_choreo_get_instance = nullptr;
ChoreoPostFrameFn g_choreo_post_frame = nullptr;

struct Il2CppDomain;
struct Il2CppAssembly;
struct Il2CppImage;
struct Il2CppClass;
struct Il2CppObject;
struct Il2CppException;
struct Il2CppType;
struct Il2CppString;
struct MethodInfo;

using Il2CppDomainGetFn = Il2CppDomain* (*)();
using Il2CppDomainGetAssembliesFn = const Il2CppAssembly** (*)(Il2CppDomain*, size_t*);
using Il2CppAssemblyGetImageFn = const Il2CppImage* (*)(const Il2CppAssembly*);
using Il2CppImageGetNameFn = const char* (*)(const Il2CppImage*);
using Il2CppImageGetClassCountFn = size_t (*)(const Il2CppImage*);
using Il2CppImageGetClassFn = const Il2CppClass* (*)(const Il2CppImage*, size_t);
using Il2CppClassFromNameFn = Il2CppClass* (*)(const Il2CppImage*, const char*, const char*);
using Il2CppClassGetMethodsFn = const MethodInfo* (*)(Il2CppClass*, void**);
using Il2CppClassGetNameFn = const char* (*)(Il2CppClass*);
using Il2CppClassGetNamespaceFn = const char* (*)(Il2CppClass*);
using Il2CppClassGetMethodFromNameFn = const MethodInfo* (*)(Il2CppClass*, const char*, int);
using Il2CppMethodGetNameFn = const char* (*)(const MethodInfo*);
using Il2CppMethodGetParamCountFn = uint32_t (*)(const MethodInfo*);
using Il2CppMethodGetFlagsFn = uint32_t (*)(const MethodInfo*, uint32_t*);
using Il2CppMethodGetParamNameFn = const char* (*)(const MethodInfo*, uint32_t);
using Il2CppRuntimeInvokeFn = Il2CppObject* (*)(const MethodInfo*, void*, void**, Il2CppException**);
using Il2CppObjectGetClassFn = Il2CppClass* (*)(Il2CppObject*);
using Il2CppObjectUnboxFn = void* (*)(Il2CppObject*);
using Il2CppObjectNewFn = Il2CppObject* (*)(Il2CppClass*);
using Il2CppStringNewFn = Il2CppString* (*)(const char*);
using Il2CppClassGetTypeFn = const Il2CppType* (*)(Il2CppClass*);
using Il2CppTypeGetObjectFn = Il2CppObject* (*)(const Il2CppType*);

constexpr uint32_t kMethodAttributeStatic = 0x0010;

void* g_libil2cpp_handle = nullptr;
Il2CppDomainGetFn g_il2cpp_domain_get = nullptr;
Il2CppDomainGetAssembliesFn g_il2cpp_domain_get_assemblies = nullptr;
Il2CppAssemblyGetImageFn g_il2cpp_assembly_get_image = nullptr;
Il2CppImageGetNameFn g_il2cpp_image_get_name = nullptr;
Il2CppImageGetClassCountFn g_il2cpp_image_get_class_count = nullptr;
Il2CppImageGetClassFn g_il2cpp_image_get_class = nullptr;
Il2CppClassFromNameFn g_il2cpp_class_from_name = nullptr;
Il2CppClassGetMethodsFn g_il2cpp_class_get_methods = nullptr;
Il2CppClassGetNameFn g_il2cpp_class_get_name = nullptr;
Il2CppClassGetNamespaceFn g_il2cpp_class_get_namespace = nullptr;
Il2CppClassGetMethodFromNameFn g_il2cpp_class_get_method_from_name = nullptr;
Il2CppMethodGetNameFn g_il2cpp_method_get_name = nullptr;
Il2CppMethodGetParamCountFn g_il2cpp_method_get_param_count = nullptr;
Il2CppMethodGetFlagsFn g_il2cpp_method_get_flags = nullptr;
Il2CppMethodGetParamNameFn g_il2cpp_method_get_param_name = nullptr;
Il2CppRuntimeInvokeFn g_il2cpp_runtime_invoke = nullptr;
Il2CppObjectGetClassFn g_il2cpp_object_get_class = nullptr;
Il2CppObjectUnboxFn g_il2cpp_object_unbox = nullptr;
Il2CppObjectNewFn g_il2cpp_object_new = nullptr;
Il2CppStringNewFn g_il2cpp_string_new = nullptr;
Il2CppClassGetTypeFn g_il2cpp_class_get_type = nullptr;
Il2CppTypeGetObjectFn g_il2cpp_type_get_object = nullptr;

const MethodInfo* g_scene_manager_load_scene_int = nullptr;
const MethodInfo* g_scene_manager_get_scene_count = nullptr;
const MethodInfo* g_scene_manager_get_active_scene = nullptr;
const MethodInfo* g_scene_get_build_index = nullptr;
const MethodInfo* g_game_object_create_primitive = nullptr;
const MethodInfo* g_game_object_find = nullptr;
const MethodInfo* g_game_object_ctor_name = nullptr;
const MethodInfo* g_game_object_get_transform = nullptr;
const MethodInfo* g_game_object_add_component_by_type = nullptr;
const MethodInfo* g_game_object_get_component_by_type = nullptr;
const MethodInfo* g_transform_set_position = nullptr;
const MethodInfo* g_transform_set_local_position = nullptr;
const MethodInfo* g_transform_set_local_scale = nullptr;
const MethodInfo* g_transform_set_parent = nullptr;
const MethodInfo* g_transform_set_parent_with_world_stays = nullptr;
const MethodInfo* g_behaviour_set_enabled = nullptr;
const MethodInfo* g_text_mesh_set_text = nullptr;
const MethodInfo* g_text_mesh_set_character_size = nullptr;
const MethodInfo* g_text_mesh_set_font_size = nullptr;
const MethodInfo* g_object_set_name = nullptr;
const MethodInfo* g_object_destroy = nullptr;
Il2CppClass* g_collider_class = nullptr;
Il2CppClass* g_game_object_class_cached = nullptr;
Il2CppClass* g_text_mesh_class = nullptr;
std::atomic<bool> g_scene_api_ready{false};
std::atomic<bool> g_scene_candidates_logged{false};
std::atomic<bool> g_remote_avatar_api_ready{false};

std::atomic<bool> g_waiting_world_scene{false};
std::atomic<int> g_pending_world_scene{-1};
std::atomic<long long> g_world_join_started_ns{0};
std::atomic<long long> g_menu_hook_last_attempt_ns{0};
std::atomic<bool> g_home_action_hooked{false};
std::atomic<bool> g_exit_without_save_hooked{false};
std::atomic<bool> g_menu_action_hooks_installed{false};
std::atomic<bool> g_exit_requested{false};
uintptr_t g_libil2cpp_exec_start = 0;
uintptr_t g_libil2cpp_exec_end = 0;
std::mutex g_menu_hook_mutex;
std::vector<uintptr_t> g_menu_hook_targets;

struct RemotePlayerState {
    std::string player_id;
    std::string name;
    int world_id = 1;
    bool has_transform = false;
    float x = 0.0f;
    float y = 1.0f;
    float z = 0.0f;
};

struct UnityVec3 {
    float x;
    float y;
    float z;
};

std::mutex g_remote_players_mutex;
std::unordered_map<std::string, RemotePlayerState> g_remote_players;
std::unordered_map<std::string, Il2CppObject*> g_remote_avatar_objects;
std::unordered_map<std::string, UnityVec3> g_remote_avatar_last_positions;
std::atomic<bool> g_main_update_hooked{false};
constexpr uintptr_t kRvaMainUpdate = 0x84CF3C;
using MainUpdateFn = void (*)(void*, const MethodInfo*);
MainUpdateFn g_main_update_original = nullptr;
std::atomic<bool> g_unity_tick_pending{false};
std::atomic<long long> g_last_unity_tick_request_ns{0};
std::atomic<long long> g_last_pause_buttons_hide_ns{0};
std::atomic<bool> g_name_tag_api_missing_logged{false};

bool ResolveIl2CppSceneApi();
void UpdateRemoteAvatarScene();
void TryHidePauseMenuButtons();
void StartProfileSyncThread();

void ClearException(JNIEnv* env) {
    if (env != nullptr && env->ExceptionCheck()) {
        env->ExceptionClear();
    }
}

bool GetJniEnv(JNIEnv** env, bool* attached) {
    if (env == nullptr || attached == nullptr || g_vm == nullptr) {
        return false;
    }

    *env = nullptr;
    *attached = false;

    const jint state = g_vm->GetEnv(reinterpret_cast<void**>(env), JNI_VERSION_1_6);
    if (state == JNI_OK) {
        return true;
    }

    if (state == JNI_EDETACHED) {
        if (g_vm->AttachCurrentThread(env, nullptr) == JNI_OK) {
            *attached = true;
            return true;
        }
    }

    return false;
}

struct CrashSignalSpec {
    int signo;
    const char* name;
};

constexpr CrashSignalSpec kCrashSignals[] = {
    {SIGSEGV, "SIGSEGV"},
    {SIGABRT, "SIGABRT"},
    {SIGBUS, "SIGBUS"},
    {SIGILL, "SIGILL"},
    {SIGFPE, "SIGFPE"},
    {SIGTRAP, "SIGTRAP"},
};

size_t SafeStrLen(const char* text) {
    if (text == nullptr) {
        return 0;
    }
    size_t len = 0;
    while (text[len] != '\0') {
        ++len;
    }
    return len;
}

void SafeWriteFd(int fd, const char* data, size_t size) {
    if (fd < 0 || data == nullptr || size == 0) {
        return;
    }

    size_t offset = 0;
    while (offset < size) {
        const ssize_t written = write(fd, data + offset, size - offset);
        if (written <= 0) {
            break;
        }
        offset += static_cast<size_t>(written);
    }
}

void SafeAppendLiteral(char* buffer, size_t capacity, size_t* inout_pos, const char* text) {
    if (buffer == nullptr || capacity == 0 || inout_pos == nullptr || text == nullptr) {
        return;
    }
    size_t pos = *inout_pos;
    for (size_t i = 0; text[i] != '\0' && (pos + 1) < capacity; ++i) {
        buffer[pos++] = text[i];
    }
    buffer[pos] = '\0';
    *inout_pos = pos;
}

void SafeAppendUnsigned(char* buffer, size_t capacity, size_t* inout_pos, unsigned long long value) {
    if (buffer == nullptr || capacity == 0 || inout_pos == nullptr) {
        return;
    }
    char tmp[32];
    int idx = 0;
    do {
        tmp[idx++] = static_cast<char>('0' + (value % 10ULL));
        value /= 10ULL;
    } while (value > 0ULL && idx < static_cast<int>(sizeof(tmp)));

    size_t pos = *inout_pos;
    for (int i = idx - 1; i >= 0 && (pos + 1) < capacity; --i) {
        buffer[pos++] = tmp[i];
    }
    buffer[pos] = '\0';
    *inout_pos = pos;
}

void SafeAppendSigned(char* buffer, size_t capacity, size_t* inout_pos, long long value) {
    if (value < 0) {
        SafeAppendLiteral(buffer, capacity, inout_pos, "-");
        const unsigned long long abs_value = static_cast<unsigned long long>(-(value + 1)) + 1ULL;
        SafeAppendUnsigned(buffer, capacity, inout_pos, abs_value);
        return;
    }
    SafeAppendUnsigned(buffer, capacity, inout_pos, static_cast<unsigned long long>(value));
}

void SafeAppendHex(char* buffer, size_t capacity, size_t* inout_pos, uintptr_t value) {
    if (buffer == nullptr || capacity == 0 || inout_pos == nullptr) {
        return;
    }
    SafeAppendLiteral(buffer, capacity, inout_pos, "0x");
    char tmp[sizeof(uintptr_t) * 2];
    int idx = 0;
    do {
        const unsigned nibble = static_cast<unsigned>(value & 0xFULL);
        tmp[idx++] = static_cast<char>((nibble < 10U) ? ('0' + nibble) : ('a' + (nibble - 10U)));
        value >>= 4U;
    } while (value != 0 && idx < static_cast<int>(sizeof(tmp)));

    size_t pos = *inout_pos;
    for (int i = idx - 1; i >= 0 && (pos + 1) < capacity; --i) {
        buffer[pos++] = tmp[i];
    }
    buffer[pos] = '\0';
    *inout_pos = pos;
}

const char* CrashSignalName(int signo) {
    for (const CrashSignalSpec& spec : kCrashSignals) {
        if (spec.signo == signo) {
            return spec.name;
        }
    }
    return "UNKNOWN";
}

void NativeCrashSignalHandler(int signo, siginfo_t* info, void* ucontext) {
    (void)ucontext;
    if (g_native_crash_in_handler != 0) {
        _exit(128 + signo);
    }
    g_native_crash_in_handler = 1;

    char path_local[sizeof(g_native_crash_log_path)] = {0};
    size_t path_len = 0;
    while ((path_len + 1) < sizeof(path_local) && g_native_crash_log_path[path_len] != '\0') {
        path_local[path_len] = g_native_crash_log_path[path_len];
        ++path_len;
    }
    path_local[path_len] = '\0';
    const char* path = (path_len > 0) ? path_local : kDefaultNativeCrashLogPath;

    int fd = open(path, O_CREAT | O_WRONLY | O_APPEND | O_CLOEXEC, 0644);
    if (fd >= 0) {
        char line[512] = {0};
        size_t pos = 0;
        SafeAppendLiteral(line, sizeof(line), &pos, "\n=== MP Native Crash ===\n");
        SafeAppendLiteral(line, sizeof(line), &pos, "signal=");
        SafeAppendSigned(line, sizeof(line), &pos, static_cast<long long>(signo));
        SafeAppendLiteral(line, sizeof(line), &pos, " (");
        SafeAppendLiteral(line, sizeof(line), &pos, CrashSignalName(signo));
        SafeAppendLiteral(line, sizeof(line), &pos, ")");
        SafeAppendLiteral(line, sizeof(line), &pos, " code=");
        SafeAppendSigned(line, sizeof(line), &pos, static_cast<long long>(info != nullptr ? info->si_code : 0));
        SafeAppendLiteral(line, sizeof(line), &pos, " addr=");
        SafeAppendHex(line, sizeof(line), &pos, reinterpret_cast<uintptr_t>(info != nullptr ? info->si_addr : nullptr));
        SafeAppendLiteral(line, sizeof(line), &pos, " pid=");
        SafeAppendUnsigned(line, sizeof(line), &pos, static_cast<unsigned long long>(getpid()));
        SafeAppendLiteral(line, sizeof(line), &pos, " tid=");
        SafeAppendSigned(line, sizeof(line), &pos, static_cast<long long>(syscall(SYS_gettid)));
        SafeAppendLiteral(line, sizeof(line), &pos, "\n");
        SafeWriteFd(fd, line, pos);
        close(fd);
    }

    struct sigaction sa;
    std::memset(&sa, 0, sizeof(sa));
    sa.sa_handler = SIG_DFL;
    sigemptyset(&sa.sa_mask);
    sigaction(signo, &sa, nullptr);
    syscall(SYS_tgkill, getpid(), static_cast<pid_t>(syscall(SYS_gettid)), signo);
    _exit(128 + signo);
}

void ConfigureNativeCrashLogPath(const std::string& external_dir) {
    const std::string path =
        external_dir.empty() ? kDefaultNativeCrashLogPath : (external_dir + "/mp_native_crash.log");
    std::memset(g_native_crash_log_path, 0, sizeof(g_native_crash_log_path));
    const size_t copy_len = std::min(path.size(), sizeof(g_native_crash_log_path) - 1);
    std::memcpy(g_native_crash_log_path, path.c_str(), copy_len);

    int fd = open(g_native_crash_log_path, O_CREAT | O_WRONLY | O_APPEND | O_CLOEXEC, 0644);
    if (fd >= 0) {
        const char* marker = "--- mp session start ---\n";
        SafeWriteFd(fd, marker, SafeStrLen(marker));
        close(fd);
    }
    MOD_LOGI("Native crash log path: %s", g_native_crash_log_path);
}

void InstallNativeCrashSignalHandlers() {
    bool expected = false;
    if (!g_native_crash_handlers_installed.compare_exchange_strong(expected, true)) {
        return;
    }

    for (const CrashSignalSpec& spec : kCrashSignals) {
        struct sigaction sa;
        std::memset(&sa, 0, sizeof(sa));
        sa.sa_sigaction = NativeCrashSignalHandler;
        sa.sa_flags = SA_SIGINFO | SA_RESETHAND;
        sigemptyset(&sa.sa_mask);
        if (sigaction(spec.signo, &sa, nullptr) != 0) {
            MOD_LOGW("InstallNativeCrashSignalHandlers: failed for signal=%d", spec.signo);
        }
    }
    MOD_LOGI("InstallNativeCrashSignalHandlers: armed.");
}

std::string TrimCopy(const std::string& input) {
    size_t start = 0;
    while (start < input.size() && std::isspace(static_cast<unsigned char>(input[start])) != 0) {
        ++start;
    }

    size_t end = input.size();
    while (end > start && std::isspace(static_cast<unsigned char>(input[end - 1])) != 0) {
        --end;
    }

    return input.substr(start, end - start);
}

std::string JsonEscape(const std::string& input) {
    std::string out;
    out.reserve(input.size() + 8);
    for (char ch : input) {
        switch (ch) {
            case '\\':
                out += "\\\\";
                break;
            case '"':
                out += "\\\"";
                break;
            case '\n':
                out += "\\n";
                break;
            case '\r':
                out += "\\r";
                break;
            case '\t':
                out += "\\t";
                break;
            default:
                out.push_back(ch);
                break;
        }
    }
    return out;
}

std::string JStringToUtf8(JNIEnv* env, jstring value) {
    if (env == nullptr || value == nullptr) {
        return "";
    }

    const char* raw = env->GetStringUTFChars(value, nullptr);
    if (raw == nullptr) {
        return "";
    }

    std::string output(raw);
    env->ReleaseStringUTFChars(value, raw);
    return output;
}

std::string QueryAppDataDir(JNIEnv* env, jobject context) {
    if (env == nullptr || context == nullptr) {
        return "";
    }

    jclass context_cls = env->GetObjectClass(context);
    if (context_cls == nullptr) {
        ClearException(env);
        return "";
    }

    jmethodID get_application_info = env->GetMethodID(
        context_cls, "getApplicationInfo", "()Landroid/content/pm/ApplicationInfo;");
    if (get_application_info == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jobject app_info = env->CallObjectMethod(context, get_application_info);
    if (app_info == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jclass app_info_cls = env->GetObjectClass(app_info);
    jfieldID data_dir_field = env->GetFieldID(app_info_cls, "dataDir", "Ljava/lang/String;");
    if (data_dir_field == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(app_info_cls);
        env->DeleteLocalRef(app_info);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jstring data_dir = reinterpret_cast<jstring>(env->GetObjectField(app_info, data_dir_field));
    const std::string out = JStringToUtf8(env, data_dir);

    if (data_dir != nullptr) env->DeleteLocalRef(data_dir);
    env->DeleteLocalRef(app_info_cls);
    env->DeleteLocalRef(app_info);
    env->DeleteLocalRef(context_cls);
    return out;
}

std::string QueryExternalFilesDir(JNIEnv* env, jobject context) {
    if (env == nullptr || context == nullptr) {
        return "";
    }

    jclass context_cls = env->GetObjectClass(context);
    if (context_cls == nullptr) {
        ClearException(env);
        return "";
    }

    jmethodID get_external_files_dir = env->GetMethodID(
        context_cls, "getExternalFilesDir", "(Ljava/lang/String;)Ljava/io/File;");
    if (get_external_files_dir == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jobject file_obj = env->CallObjectMethod(context, get_external_files_dir, nullptr);
    if (file_obj == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jclass file_cls = env->GetObjectClass(file_obj);
    jmethodID get_abs_path = env->GetMethodID(file_cls, "getAbsolutePath", "()Ljava/lang/String;");
    if (get_abs_path == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(file_cls);
        env->DeleteLocalRef(file_obj);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jstring abs_path = reinterpret_cast<jstring>(env->CallObjectMethod(file_obj, get_abs_path));
    const std::string out = JStringToUtf8(env, abs_path);

    if (abs_path != nullptr) env->DeleteLocalRef(abs_path);
    env->DeleteLocalRef(file_cls);
    env->DeleteLocalRef(file_obj);
    env->DeleteLocalRef(context_cls);
    return out;
}

std::string QueryAndroidModel(JNIEnv* env) {
    if (env == nullptr) {
        return "";
    }

    jclass build_cls = env->FindClass("android/os/Build");
    if (build_cls == nullptr) {
        ClearException(env);
        return "";
    }

    jfieldID model_field = env->GetStaticFieldID(build_cls, "MODEL", "Ljava/lang/String;");
    if (model_field == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(build_cls);
        return "";
    }

    jstring model = reinterpret_cast<jstring>(env->GetStaticObjectField(build_cls, model_field));
    const std::string out = JStringToUtf8(env, model);
    if (model != nullptr) env->DeleteLocalRef(model);
    env->DeleteLocalRef(build_cls);
    return out;
}

std::string QueryAndroidSecureId(JNIEnv* env, jobject context) {
    if (env == nullptr || context == nullptr) {
        return "";
    }

    jclass context_cls = env->GetObjectClass(context);
    if (context_cls == nullptr) {
        ClearException(env);
        return "";
    }

    jmethodID get_content_resolver = env->GetMethodID(
        context_cls, "getContentResolver", "()Landroid/content/ContentResolver;");
    if (get_content_resolver == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jobject resolver = env->CallObjectMethod(context, get_content_resolver);
    if (resolver == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jclass secure_cls = env->FindClass("android/provider/Settings$Secure");
    if (secure_cls == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(resolver);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jfieldID android_id_field = env->GetStaticFieldID(secure_cls, "ANDROID_ID", "Ljava/lang/String;");
    jmethodID get_string = env->GetStaticMethodID(
        secure_cls,
        "getString",
        "(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;");
    if (android_id_field == nullptr || get_string == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(secure_cls);
        env->DeleteLocalRef(resolver);
        env->DeleteLocalRef(context_cls);
        return "";
    }

    jstring android_id_key = reinterpret_cast<jstring>(env->GetStaticObjectField(secure_cls, android_id_field));
    jstring android_id = reinterpret_cast<jstring>(env->CallStaticObjectMethod(
        secure_cls,
        get_string,
        resolver,
        android_id_key));
    const std::string out = JStringToUtf8(env, android_id);

    if (android_id != nullptr) env->DeleteLocalRef(android_id);
    if (android_id_key != nullptr) env->DeleteLocalRef(android_id_key);
    env->DeleteLocalRef(secure_cls);
    env->DeleteLocalRef(resolver);
    env->DeleteLocalRef(context_cls);
    return out;
}

std::string SanitizePlayerId(const std::string& source, const std::string& fallback) {
    const std::string base = source.empty() ? fallback : source;
    if (base.empty()) {
        return "player_default";
    }

    std::string out;
    out.reserve(base.size());
    for (char ch : base) {
        const unsigned char uch = static_cast<unsigned char>(ch);
        if (std::isalnum(uch) != 0 || ch == '_' || ch == '-' || ch == '.') {
            out.push_back(ch);
        } else {
            out.push_back('_');
        }
    }

    if (out.empty()) {
        out = "player_default";
    }
    if (out.size() > 48) {
        out.resize(48);
    }
    return out;
}

bool ReadFileToString(const std::string& path, std::string* out) {
    if (out == nullptr || path.empty()) {
        return false;
    }

    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    std::ostringstream buffer;
    buffer << file.rdbuf();
    *out = buffer.str();
    return true;
}

bool ReadFileModifiedTime(const std::string& path, time_t* out_mtime) {
    if (path.empty() || out_mtime == nullptr) {
        return false;
    }

    struct stat st{};
    if (stat(path.c_str(), &st) != 0 || !S_ISREG(st.st_mode)) {
        return false;
    }

    *out_mtime = st.st_mtime;
    return true;
}

std::string UrlDecode(const std::string& input) {
    std::string out;
    out.reserve(input.size());

    for (size_t i = 0; i < input.size(); ++i) {
        const char ch = input[i];
        if (ch == '%' && i + 2 < input.size()) {
            const char hi = input[i + 1];
            const char lo = input[i + 2];
            auto hex = [](char c) -> int {
                if (c >= '0' && c <= '9') return c - '0';
                if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
                if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
                return -1;
            };
            const int h = hex(hi);
            const int l = hex(lo);
            if (h >= 0 && l >= 0) {
                out.push_back(static_cast<char>((h << 4) | l));
                i += 2;
                continue;
            }
        }

        if (ch == '+') {
            out.push_back(' ');
        } else {
            out.push_back(ch);
        }
    }

    return out;
}

bool ExtractXmlFloatValue(const std::string& xml, const std::string& name, double* out) {
    if (out == nullptr || name.empty()) {
        return false;
    }

    const std::string marker = "<float name=\"" + name + "\" value=\"";
    const size_t start = xml.find(marker);
    if (start == std::string::npos) {
        return false;
    }

    const size_t value_start = start + marker.size();
    const size_t value_end = xml.find('"', value_start);
    if (value_end == std::string::npos || value_end <= value_start) {
        return false;
    }

    const std::string value_text = xml.substr(value_start, value_end - value_start);
    char* end_ptr = nullptr;
    const double parsed = std::strtod(value_text.c_str(), &end_ptr);
    if (end_ptr == nullptr || end_ptr == value_text.c_str()) {
        return false;
    }

    *out = parsed;
    return true;
}

bool ExtractXmlStringValue(const std::string& xml, const std::string& name, std::string* out) {
    if (out == nullptr || name.empty()) {
        return false;
    }

    const std::string marker = "<string name=\"" + name + "\">";
    const size_t start = xml.find(marker);
    if (start == std::string::npos) {
        return false;
    }

    const size_t value_start = start + marker.size();
    const size_t value_end = xml.find("</string>", value_start);
    if (value_end == std::string::npos || value_end < value_start) {
        return false;
    }

    *out = xml.substr(value_start, value_end - value_start);
    return true;
}

std::string FindLatestPcSaveFile(const std::string& saves_dir) {
    if (saves_dir.empty()) {
        return "";
    }

    DIR* dir = opendir(saves_dir.c_str());
    if (dir == nullptr) {
        return "";
    }

    std::string best_path;
    time_t best_mtime = 0;

    while (dirent* ent = readdir(dir)) {
        const std::string name(ent->d_name);
        if (name == "." || name == "..") {
            continue;
        }
        if (name.size() < 4 || name.substr(name.size() - 3) != ".pc") {
            continue;
        }

        const std::string full_path = saves_dir + "/" + name;
        struct stat st{};
        if (stat(full_path.c_str(), &st) != 0) {
            continue;
        }
        if (!S_ISREG(st.st_mode)) {
            continue;
        }

        if (best_path.empty() || st.st_mtime > best_mtime) {
            best_path = full_path;
            best_mtime = st.st_mtime;
        }
    }

    closedir(dir);
    return best_path;
}

bool DecodePcSavePayload(const std::string& raw_utf8, std::string* decoded) {
    if (decoded == nullptr || raw_utf8.empty()) {
        return false;
    }

    decoded->clear();
    decoded->reserve(raw_utf8.size() / 2);

    const unsigned char* data = reinterpret_cast<const unsigned char*>(raw_utf8.data());
    const size_t size = raw_utf8.size();
    size_t i = 0;

    while (i < size) {
        uint32_t cp = 0;
        const unsigned char c0 = data[i];

        if (c0 < 0x80) {
            cp = c0;
            ++i;
        } else if ((c0 & 0xE0) == 0xC0 && i + 1 < size) {
            cp = static_cast<uint32_t>(c0 & 0x1F) << 6;
            cp |= static_cast<uint32_t>(data[i + 1] & 0x3F);
            i += 2;
        } else if ((c0 & 0xF0) == 0xE0 && i + 2 < size) {
            cp = static_cast<uint32_t>(c0 & 0x0F) << 12;
            cp |= static_cast<uint32_t>(data[i + 1] & 0x3F) << 6;
            cp |= static_cast<uint32_t>(data[i + 2] & 0x3F);
            i += 3;
        } else if ((c0 & 0xF8) == 0xF0 && i + 3 < size) {
            cp = static_cast<uint32_t>(c0 & 0x07) << 18;
            cp |= static_cast<uint32_t>(data[i + 1] & 0x3F) << 12;
            cp |= static_cast<uint32_t>(data[i + 2] & 0x3F) << 6;
            cp |= static_cast<uint32_t>(data[i + 3] & 0x3F);
            i += 4;
        } else {
            ++i;
            continue;
        }

        const uint8_t encoded_byte = static_cast<uint8_t>(cp & 0xFFu);
        const uint8_t plain_byte = static_cast<uint8_t>((encoded_byte - 128u) ^ 0x01u);
        decoded->push_back(static_cast<char>(plain_byte));
    }

    return !decoded->empty();
}

std::unordered_map<std::string, int> ExtractSpawnCounts(const std::string& decoded_text) {
    std::unordered_map<std::string, int> counts;
    const std::string marker = "\"spawnId\":\"";
    size_t pos = 0;

    while (true) {
        pos = decoded_text.find(marker, pos);
        if (pos == std::string::npos) {
            break;
        }
        const size_t start = pos + marker.size();
        const size_t end = decoded_text.find('"', start);
        if (end == std::string::npos) {
            break;
        }

        const std::string id = decoded_text.substr(start, end - start);
        if (!id.empty()) {
            counts[id] += 1;
        }
        pos = end + 1;
    }

    return counts;
}

bool ExtractJsonString(const std::string& json, const std::string& key, std::string* out) {
    if (out == nullptr) {
        return false;
    }

    const std::string marker = "\"" + key + "\":";
    size_t pos = json.find(marker);
    if (pos == std::string::npos) {
        return false;
    }

    pos += marker.size();
    while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t' || json[pos] == '\n' || json[pos] == '\r')) {
        ++pos;
    }

    if (pos >= json.size() || json[pos] != '"') {
        return false;
    }

    ++pos;
    std::string value;
    bool escaped = false;
    for (; pos < json.size(); ++pos) {
        const char ch = json[pos];
        if (escaped) {
            switch (ch) {
                case 'n':
                    value.push_back('\n');
                    break;
                case 'r':
                    value.push_back('\r');
                    break;
                case 't':
                    value.push_back('\t');
                    break;
                default:
                    value.push_back(ch);
                    break;
            }
            escaped = false;
            continue;
        }

        if (ch == '\\') {
            escaped = true;
            continue;
        }
        if (ch == '"') {
            *out = value;
            return true;
        }
        value.push_back(ch);
    }

    return false;
}

bool ExtractJsonDouble(const std::string& json, const std::string& key, double* out) {
    if (out == nullptr) {
        return false;
    }

    const std::string marker = "\"" + key + "\":";
    size_t pos = json.find(marker);
    if (pos == std::string::npos) {
        return false;
    }

    pos += marker.size();
    while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t' || json[pos] == '\n' || json[pos] == '\r')) {
        ++pos;
    }

    char* end_ptr = nullptr;
    const double value = std::strtod(json.c_str() + static_cast<long>(pos), &end_ptr);
    if (end_ptr == nullptr || end_ptr == json.c_str() + static_cast<long>(pos)) {
        return false;
    }

    *out = value;
    return true;
}

bool ExtractJsonInt(const std::string& json, const std::string& key, int* out) {
    if (out == nullptr) {
        return false;
    }

    double parsed = 0.0;
    if (!ExtractJsonDouble(json, key, &parsed)) {
        return false;
    }

    *out = static_cast<int>(parsed);
    return true;
}

bool ExtractJsonObject(const std::string& json, const std::string& key, std::string* out) {
    if (out == nullptr) {
        return false;
    }

    const std::string marker = "\"" + key + "\":";
    size_t pos = json.find(marker);
    if (pos == std::string::npos) {
        return false;
    }

    pos += marker.size();
    while (pos < json.size() && std::isspace(static_cast<unsigned char>(json[pos])) != 0) {
        ++pos;
    }
    if (pos >= json.size() || json[pos] != '{') {
        return false;
    }

    const size_t begin = pos;
    int depth = 0;
    bool in_string = false;
    bool escaped = false;
    for (; pos < json.size(); ++pos) {
        const char ch = json[pos];
        if (in_string) {
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                in_string = false;
            }
            continue;
        }

        if (ch == '"') {
            in_string = true;
            continue;
        }
        if (ch == '{') {
            ++depth;
            continue;
        }
        if (ch == '}') {
            --depth;
            if (depth == 0) {
                *out = json.substr(begin, pos - begin + 1);
                return true;
            }
        }
    }
    return false;
}

bool ExtractJsonArrayBounds(const std::string& json, const std::string& key, size_t* out_open, size_t* out_close) {
    if (out_open == nullptr || out_close == nullptr) {
        return false;
    }

    const std::string marker = "\"" + key + "\":";
    size_t pos = json.find(marker);
    if (pos == std::string::npos) {
        return false;
    }

    pos += marker.size();
    while (pos < json.size() && std::isspace(static_cast<unsigned char>(json[pos])) != 0) {
        ++pos;
    }
    if (pos >= json.size() || json[pos] != '[') {
        return false;
    }

    const size_t open = pos;
    int depth = 0;
    bool in_string = false;
    bool escaped = false;
    for (; pos < json.size(); ++pos) {
        const char ch = json[pos];
        if (in_string) {
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                in_string = false;
            }
            continue;
        }

        if (ch == '"') {
            in_string = true;
            continue;
        }
        if (ch == '[') {
            ++depth;
            continue;
        }
        if (ch == ']') {
            --depth;
            if (depth == 0) {
                *out_open = open;
                *out_close = pos;
                return true;
            }
        }
    }
    return false;
}

void SplitTopLevelJsonObjects(const std::string& json,
                              size_t array_open,
                              size_t array_close,
                              std::vector<std::string>* out_objects) {
    if (out_objects == nullptr || array_open >= array_close || array_close >= json.size()) {
        return;
    }
    out_objects->clear();

    bool in_string = false;
    bool escaped = false;
    int obj_depth = 0;
    size_t obj_begin = std::string::npos;

    for (size_t i = array_open + 1; i < array_close; ++i) {
        const char ch = json[i];
        if (in_string) {
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                in_string = false;
            }
            continue;
        }

        if (ch == '"') {
            in_string = true;
            continue;
        }
        if (ch == '{') {
            if (obj_depth == 0) {
                obj_begin = i;
            }
            ++obj_depth;
            continue;
        }
        if (ch == '}') {
            --obj_depth;
            if (obj_depth == 0 && obj_begin != std::string::npos) {
                out_objects->push_back(json.substr(obj_begin, i - obj_begin + 1));
                obj_begin = std::string::npos;
            }
        }
    }
}

struct ProfileSnapshot {
    bool valid = false;
    int world_id = 1;
    double money = 0.0;
    double bitcoin = 0.0;
    std::unordered_map<std::string, int> spawn_counts;
    std::vector<std::string> unlocked_items;
    std::string save_path;
};

ProfileSnapshot BuildProfileSnapshotFromLocalSave() {
    ProfileSnapshot snapshot;
    snapshot.valid = false;
    snapshot.world_id = 1;

    const std::string save_path = FindLatestPcSaveFile(g_saves_dir);
    if (save_path.empty()) {
        return snapshot;
    }

    std::string raw_save;
    if (!ReadFileToString(save_path, &raw_save) || raw_save.empty()) {
        return snapshot;
    }

    std::string decoded_save;
    if (!DecodePcSavePayload(raw_save, &decoded_save) || decoded_save.empty()) {
        return snapshot;
    }

    double coin = 0.0;
    int room = 1;
    ExtractJsonDouble(decoded_save, "coin", &coin);
    ExtractJsonInt(decoded_save, "room", &room);

    double bitcoin = 0.0;
    if (!g_playerprefs_path.empty()) {
        std::string prefs_xml;
        if (ReadFileToString(g_playerprefs_path, &prefs_xml)) {
            ExtractXmlFloatValue(prefs_xml, "Bitcoin", &bitcoin);

            std::string unlocked_encoded;
            if (ExtractXmlStringValue(prefs_xml, "Unlocked", &unlocked_encoded) && !unlocked_encoded.empty()) {
                const std::string unlocked_text = UrlDecode(unlocked_encoded);
                std::set<std::string> uniq;
                size_t start = 0;
                while (start <= unlocked_text.size()) {
                    size_t end = unlocked_text.find(',', start);
                    if (end == std::string::npos) {
                        end = unlocked_text.size();
                    }
                    std::string item = TrimCopy(unlocked_text.substr(start, end - start));
                    if (!item.empty()) {
                        uniq.insert(item);
                    }
                    if (end == unlocked_text.size()) {
                        break;
                    }
                    start = end + 1;
                }
                snapshot.unlocked_items.assign(uniq.begin(), uniq.end());
            }
        }
    }

    snapshot.world_id = std::max(1, room);
    snapshot.money = coin;
    snapshot.bitcoin = bitcoin;
    snapshot.spawn_counts = ExtractSpawnCounts(decoded_save);
    snapshot.save_path = save_path;
    snapshot.valid = true;
    return snapshot;
}

std::string BuildInventoryJsonObject(const ProfileSnapshot& snapshot) {
    const std::unordered_map<std::string, int>& spawn_counts = snapshot.spawn_counts;
    std::vector<std::pair<std::string, int>> entries(spawn_counts.begin(), spawn_counts.end());
    std::sort(entries.begin(), entries.end(), [](const auto& a, const auto& b) {
        return a.first < b.first;
    });

    std::string json = "{\"spawnCounts\":{";
    bool first = true;
    for (const auto& [key, value] : entries) {
        if (!first) {
            json += ",";
        }
        first = false;
        json += "\"";
        json += JsonEscape(key);
        json += "\":";
        json += std::to_string(value);
    }
    json += "},\"unlocked\":[";
    for (size_t i = 0; i < snapshot.unlocked_items.size(); ++i) {
        if (i > 0) {
            json += ",";
        }
        json += "\"";
        json += JsonEscape(snapshot.unlocked_items[i]);
        json += "\"";
    }
    json += "]}";
    return json;
}

std::string BuildProfileSignature(const ProfileSnapshot& snapshot) {
    if (!snapshot.valid) {
        return "invalid";
    }

    std::vector<std::pair<std::string, int>> entries(snapshot.spawn_counts.begin(), snapshot.spawn_counts.end());
    std::sort(entries.begin(), entries.end(), [](const auto& a, const auto& b) {
        return a.first < b.first;
    });

    std::ostringstream oss;
    oss.setf(std::ios::fixed);
    oss.precision(4);
    oss << snapshot.world_id << "|" << snapshot.money << "|" << snapshot.bitcoin << "|";
    for (const auto& [name, count] : entries) {
        oss << name << ":" << count << ";";
    }
    oss << "|u:";
    for (const std::string& item : snapshot.unlocked_items) {
        oss << item << ",";
    }
    return oss.str();
}

std::string BuildSetProfilePayload(const ProfileSnapshot& snapshot) {
    std::ostringstream oss;
    oss.setf(std::ios::fixed);
    oss.precision(2);
    oss << "{\"type\":\"set_profile\",\"money\":" << snapshot.money;
    oss.precision(6);
    oss << ",\"bitcoin\":" << snapshot.bitcoin
        << ",\"inventory\":" << BuildInventoryJsonObject(snapshot)
        << ",\"sourceSave\":\"" << JsonEscape(snapshot.save_path) << "\"}";
    return oss.str();
}

void PushUiEvent(const UiEvent& event) {
    std::lock_guard<std::mutex> lock(g_event_mutex);
    g_ui_events.push_back(event);
}

void PushStatusEvent(const std::string& text) {
    UiEvent event;
    event.type = UiEvent::Type::kStatus;
    event.text = text;
    PushUiEvent(event);
}

void PushChatEvent(const std::string& text) {
    UiEvent event;
    event.type = UiEvent::Type::kChat;
    event.text = text;
    PushUiEvent(event);
}

void PushConnectedEvent(int world_id, double money, double bitcoin) {
    UiEvent event;
    event.type = UiEvent::Type::kConnected;
    event.world_id = world_id;
    event.money = money;
    event.bitcoin = bitcoin;
    PushUiEvent(event);
}

void PushDisconnectedEvent(const std::string& reason) {
    UiEvent event;
    event.type = UiEvent::Type::kDisconnected;
    event.text = reason;
    PushUiEvent(event);
}

int DpToPx(JNIEnv* env, jobject context, int dp) {
    if (env == nullptr || context == nullptr) {
        return dp;
    }

    jclass context_cls = env->GetObjectClass(context);
    jmethodID get_resources = env->GetMethodID(context_cls, "getResources", "()Landroid/content/res/Resources;");
    env->DeleteLocalRef(context_cls);
    if (get_resources == nullptr) {
        ClearException(env);
        return dp;
    }

    jobject resources = env->CallObjectMethod(context, get_resources);
    if (resources == nullptr) {
        ClearException(env);
        return dp;
    }

    jclass resources_cls = env->GetObjectClass(resources);
    jmethodID get_display_metrics = env->GetMethodID(resources_cls, "getDisplayMetrics", "()Landroid/util/DisplayMetrics;");
    env->DeleteLocalRef(resources_cls);
    if (get_display_metrics == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(resources);
        return dp;
    }

    jobject metrics = env->CallObjectMethod(resources, get_display_metrics);
    env->DeleteLocalRef(resources);
    if (metrics == nullptr) {
        ClearException(env);
        return dp;
    }

    jclass metrics_cls = env->GetObjectClass(metrics);
    jfieldID density_field = env->GetFieldID(metrics_cls, "density", "F");
    float density = 1.0f;
    if (density_field != nullptr) {
        density = env->GetFloatField(metrics, density_field);
    } else {
        ClearException(env);
    }

    env->DeleteLocalRef(metrics_cls);
    env->DeleteLocalRef(metrics);

    return static_cast<int>(density * static_cast<float>(dp) + 0.5f);
}

void SetViewVisibility(JNIEnv* env, jobject view, int visibility) {
    if (env == nullptr || view == nullptr) {
        return;
    }

    jclass view_cls = env->GetObjectClass(view);
    jmethodID set_visibility = env->GetMethodID(view_cls, "setVisibility", "(I)V");
    if (set_visibility != nullptr) {
        env->CallVoidMethod(view, set_visibility, static_cast<jint>(visibility));
    } else {
        ClearException(env);
    }
    env->DeleteLocalRef(view_cls);
}

void SetViewEnabled(JNIEnv* env, jobject view, bool enabled) {
    if (env == nullptr || view == nullptr) {
        return;
    }

    jclass view_cls = env->GetObjectClass(view);
    jmethodID set_enabled = env->GetMethodID(view_cls, "setEnabled", "(Z)V");
    if (set_enabled != nullptr) {
        env->CallVoidMethod(view, set_enabled, static_cast<jboolean>(enabled));
    } else {
        ClearException(env);
    }
    env->DeleteLocalRef(view_cls);
}

void SetViewClickable(JNIEnv* env, jobject view, bool clickable) {
    if (env == nullptr || view == nullptr) {
        return;
    }

    jclass view_cls = env->GetObjectClass(view);
    jmethodID set_clickable = env->GetMethodID(view_cls, "setClickable", "(Z)V");
    if (set_clickable != nullptr) {
        env->CallVoidMethod(view, set_clickable, static_cast<jboolean>(clickable));
    } else {
        ClearException(env);
    }

    jmethodID set_focusable = env->GetMethodID(view_cls, "setFocusable", "(Z)V");
    if (set_focusable != nullptr) {
        env->CallVoidMethod(view, set_focusable, static_cast<jboolean>(clickable));
    } else {
        ClearException(env);
    }

    env->DeleteLocalRef(view_cls);
}

void SetViewBackgroundColor(JNIEnv* env, jobject view, jint color) {
    if (env == nullptr || view == nullptr) {
        return;
    }

    jclass view_cls = env->GetObjectClass(view);
    jmethodID set_background_color = env->GetMethodID(view_cls, "setBackgroundColor", "(I)V");
    if (set_background_color != nullptr) {
        env->CallVoidMethod(view, set_background_color, color);
    } else {
        ClearException(env);
    }
    env->DeleteLocalRef(view_cls);
}

void SetTextValue(JNIEnv* env, jobject text_view, const std::string& text) {
    if (env == nullptr || text_view == nullptr) {
        return;
    }

    jclass text_cls = env->GetObjectClass(text_view);
    jmethodID set_text = env->GetMethodID(text_cls, "setText", "(Ljava/lang/CharSequence;)V");
    if (set_text != nullptr) {
        jstring jtext = env->NewStringUTF(text.c_str());
        if (jtext != nullptr) {
            env->CallVoidMethod(text_view, set_text, jtext);
            env->DeleteLocalRef(jtext);
        }
    } else {
        ClearException(env);
    }
    env->DeleteLocalRef(text_cls);
}

void SetHintValue(JNIEnv* env, jobject text_view, const std::string& text) {
    if (env == nullptr || text_view == nullptr) {
        return;
    }

    jclass text_cls = env->GetObjectClass(text_view);
    jmethodID set_hint = env->GetMethodID(text_cls, "setHint", "(Ljava/lang/CharSequence;)V");
    if (set_hint != nullptr) {
        jstring jtext = env->NewStringUTF(text.c_str());
        if (jtext != nullptr) {
            env->CallVoidMethod(text_view, set_hint, jtext);
            env->DeleteLocalRef(jtext);
        }
    } else {
        ClearException(env);
    }
    env->DeleteLocalRef(text_cls);
}

void SetProgressValue(JNIEnv* env, jobject progress_bar, int value) {
    if (env == nullptr || progress_bar == nullptr) {
        return;
    }

    jclass progress_cls = env->GetObjectClass(progress_bar);
    jmethodID set_progress = env->GetMethodID(progress_cls, "setProgress", "(I)V");
    if (set_progress != nullptr) {
        env->CallVoidMethod(progress_bar, set_progress, static_cast<jint>(value));
    } else {
        ClearException(env);
    }
    env->DeleteLocalRef(progress_cls);
}

std::string ReadTextValue(JNIEnv* env, jobject text_view) {
    if (env == nullptr || text_view == nullptr) {
        return "";
    }

    std::string output;
    jclass text_cls = env->GetObjectClass(text_view);
    jmethodID get_text = env->GetMethodID(text_cls, "getText", "()Ljava/lang/CharSequence;");
    if (get_text == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(text_cls);
        return output;
    }

    jobject sequence = env->CallObjectMethod(text_view, get_text);
    if (sequence == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(text_cls);
        return output;
    }

    jclass object_cls = env->FindClass("java/lang/Object");
    jmethodID to_string = env->GetMethodID(object_cls, "toString", "()Ljava/lang/String;");
    if (to_string != nullptr) {
        jstring text = reinterpret_cast<jstring>(env->CallObjectMethod(sequence, to_string));
        if (text != nullptr) {
            const char* raw = env->GetStringUTFChars(text, nullptr);
            if (raw != nullptr) {
                output = raw;
                env->ReleaseStringUTFChars(text, raw);
            }
            env->DeleteLocalRef(text);
        }
    } else {
        ClearException(env);
    }

    env->DeleteLocalRef(object_cls);
    env->DeleteLocalRef(sequence);
    env->DeleteLocalRef(text_cls);
    return output;
}

jobject NewFrameLayoutParams(JNIEnv* env, int width, int height, int gravity, int left, int top, int right, int bottom) {
    jclass lp_cls = env->FindClass("android/widget/FrameLayout$LayoutParams");
    if (lp_cls == nullptr) {
        ClearException(env);
        return nullptr;
    }

    jmethodID ctor = env->GetMethodID(lp_cls, "<init>", "(II)V");
    if (ctor == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(lp_cls);
        return nullptr;
    }

    jobject params = env->NewObject(lp_cls, ctor, static_cast<jint>(width), static_cast<jint>(height));
    if (params == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(lp_cls);
        return nullptr;
    }

    jfieldID gravity_field = env->GetFieldID(lp_cls, "gravity", "I");
    if (gravity_field != nullptr) {
        env->SetIntField(params, gravity_field, static_cast<jint>(gravity));
    } else {
        ClearException(env);
    }

    jmethodID set_margins = env->GetMethodID(lp_cls, "setMargins", "(IIII)V");
    if (set_margins != nullptr) {
        env->CallVoidMethod(params, set_margins, static_cast<jint>(left), static_cast<jint>(top), static_cast<jint>(right), static_cast<jint>(bottom));
    } else {
        ClearException(env);
    }

    env->DeleteLocalRef(lp_cls);
    return params;
}

jobject NewLinearLayoutParams(JNIEnv* env, int width, int height, int left, int top, int right, int bottom) {
    jclass lp_cls = env->FindClass("android/widget/LinearLayout$LayoutParams");
    if (lp_cls == nullptr) {
        ClearException(env);
        return nullptr;
    }

    jmethodID ctor = env->GetMethodID(lp_cls, "<init>", "(II)V");
    if (ctor == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(lp_cls);
        return nullptr;
    }

    jobject params = env->NewObject(lp_cls, ctor, static_cast<jint>(width), static_cast<jint>(height));
    if (params == nullptr) {
        ClearException(env);
        env->DeleteLocalRef(lp_cls);
        return nullptr;
    }

    jmethodID set_margins = env->GetMethodID(lp_cls, "setMargins", "(IIII)V");
    if (set_margins != nullptr) {
        env->CallVoidMethod(params, set_margins, static_cast<jint>(left), static_cast<jint>(top), static_cast<jint>(right), static_cast<jint>(bottom));
    } else {
        ClearException(env);
    }

    env->DeleteLocalRef(lp_cls);
    return params;
}

void AddChildView(JNIEnv* env, jobject parent, jobject child, jobject params) {
    if (env == nullptr || parent == nullptr || child == nullptr || params == nullptr) {
        return;
    }

    jclass group_cls = env->FindClass("android/view/ViewGroup");
    if (group_cls == nullptr) {
        ClearException(env);
        return;
    }

    jmethodID add_view = env->GetMethodID(group_cls, "addView", "(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V");
    if (add_view != nullptr) {
        env->CallVoidMethod(parent, add_view, child, params);
    } else {
        ClearException(env);
    }

    env->DeleteLocalRef(group_cls);
}

jobject CreateTextView(JNIEnv* env, jobject context, const std::string& text, float text_size_sp, jint color, int gravity) {
    jclass text_cls = env->FindClass("android/widget/TextView");
    if (text_cls == nullptr) {
        ClearException(env);
        return nullptr;
    }

    jmethodID ctor = env->GetMethodID(text_cls, "<init>", "(Landroid/content/Context;)V");
    jobject text_view = nullptr;
    if (ctor != nullptr) {
        text_view = env->NewObject(text_cls, ctor, context);
    } else {
        ClearException(env);
    }

    if (text_view != nullptr) {
        jmethodID set_text_color = env->GetMethodID(text_cls, "setTextColor", "(I)V");
        if (set_text_color != nullptr) {
            env->CallVoidMethod(text_view, set_text_color, color);
        }

        jmethodID set_text_size = env->GetMethodID(text_cls, "setTextSize", "(F)V");
        if (set_text_size != nullptr) {
            env->CallVoidMethod(text_view, set_text_size, text_size_sp);
        }

        jmethodID set_gravity = env->GetMethodID(text_cls, "setGravity", "(I)V");
        if (set_gravity != nullptr) {
            env->CallVoidMethod(text_view, set_gravity, static_cast<jint>(gravity));
        }

        SetTextValue(env, text_view, text);
    }

    env->DeleteLocalRef(text_cls);
    return text_view;
}

jobject CreateEditText(JNIEnv* env, jobject context, const std::string& hint, const std::string& text) {
    jclass edit_cls = env->FindClass("android/widget/EditText");
    if (edit_cls == nullptr) {
        ClearException(env);
        return nullptr;
    }

    jmethodID ctor = env->GetMethodID(edit_cls, "<init>", "(Landroid/content/Context;)V");
    jobject edit = nullptr;
    if (ctor != nullptr) {
        edit = env->NewObject(edit_cls, ctor, context);
    } else {
        ClearException(env);
    }

    if (edit != nullptr) {
        SetHintValue(env, edit, hint);
        SetTextValue(env, edit, text);

        jmethodID set_single_line = env->GetMethodID(edit_cls, "setSingleLine", "(Z)V");
        if (set_single_line != nullptr) {
            env->CallVoidMethod(edit, set_single_line, static_cast<jboolean>(true));
        } else {
            ClearException(env);
        }

        jmethodID set_text_color = env->GetMethodID(edit_cls, "setTextColor", "(I)V");
        if (set_text_color != nullptr) {
            env->CallVoidMethod(edit, set_text_color, static_cast<jint>(0xFFFFFFFF));
        }

        jmethodID set_hint_color = env->GetMethodID(edit_cls, "setHintTextColor", "(I)V");
        if (set_hint_color != nullptr) {
            env->CallVoidMethod(edit, set_hint_color, static_cast<jint>(0xFFB0B0B0));
        }

        SetViewBackgroundColor(env, edit, static_cast<jint>(0xFF2C2C2C));

        jmethodID set_padding = env->GetMethodID(edit_cls, "setPadding", "(IIII)V");
        if (set_padding != nullptr) {
            const int pad_h = DpToPx(env, context, 12);
            const int pad_v = DpToPx(env, context, 9);
            env->CallVoidMethod(edit, set_padding, pad_h, pad_v, pad_h, pad_v);
        }
    }

    env->DeleteLocalRef(edit_cls);
    return edit;
}

jobject CreateButton(JNIEnv* env, jobject context, const std::string& text, jint view_id) {
    jclass button_cls = env->FindClass("android/widget/Button");
    if (button_cls == nullptr) {
        ClearException(env);
        return nullptr;
    }

    jmethodID ctor = env->GetMethodID(button_cls, "<init>", "(Landroid/content/Context;)V");
    jobject button = nullptr;
    if (ctor != nullptr) {
        button = env->NewObject(button_cls, ctor, context);
    } else {
        ClearException(env);
    }

    if (button != nullptr) {
        SetTextValue(env, button, text);

        jclass view_cls = env->FindClass("android/view/View");
        if (view_cls != nullptr) {
            jmethodID set_id = env->GetMethodID(view_cls, "setId", "(I)V");
            if (set_id != nullptr) {
                env->CallVoidMethod(button, set_id, view_id);
            } else {
                ClearException(env);
            }

            env->DeleteLocalRef(view_cls);
        } else {
            ClearException(env);
        }

        SetViewBackgroundColor(env, button, static_cast<jint>(0xFF257A3E));
    }

    env->DeleteLocalRef(button_cls);
    return button;
}

void SendUnityBridgeMessage(JNIEnv* env, const std::string& method, const std::string& payload) {
    if (env == nullptr || g_unity_player_class == nullptr || g_unity_send_message == nullptr) {
        return;
    }

    jstring object_name = env->NewStringUTF("MPBridge");
    jstring method_name = env->NewStringUTF(method.c_str());
    jstring body = env->NewStringUTF(payload.c_str());
    if (object_name != nullptr && method_name != nullptr && body != nullptr) {
        env->CallStaticVoidMethod(g_unity_player_class, g_unity_send_message, object_name, method_name, body);
        ClearException(env);
    }

    if (object_name != nullptr) env->DeleteLocalRef(object_name);
    if (method_name != nullptr) env->DeleteLocalRef(method_name);
    if (body != nullptr) env->DeleteLocalRef(body);
}

const Il2CppImage* FindImageByNameContains(const std::string& needle) {
    if (g_il2cpp_domain_get == nullptr || g_il2cpp_domain_get_assemblies == nullptr ||
        g_il2cpp_assembly_get_image == nullptr || g_il2cpp_image_get_name == nullptr) {
        return nullptr;
    }

    Il2CppDomain* domain = g_il2cpp_domain_get();
    if (domain == nullptr) {
        return nullptr;
    }

    size_t assembly_count = 0;
    const Il2CppAssembly** assemblies = g_il2cpp_domain_get_assemblies(domain, &assembly_count);
    if (assemblies == nullptr || assembly_count == 0) {
        return nullptr;
    }

    for (size_t i = 0; i < assembly_count; ++i) {
        const Il2CppImage* image = g_il2cpp_assembly_get_image(assemblies[i]);
        if (image == nullptr) {
            continue;
        }
        const char* image_name = g_il2cpp_image_get_name(image);
        if (image_name == nullptr) {
            continue;
        }
        if (std::strstr(image_name, needle.c_str()) != nullptr) {
            return image;
        }
    }

    return nullptr;
}

const MethodInfo* FindStaticMethod(Il2CppClass* klass, const char* method_name, uint32_t param_count, const char* first_param_name_hint) {
    if (klass == nullptr || g_il2cpp_class_get_methods == nullptr || g_il2cpp_method_get_name == nullptr ||
        g_il2cpp_method_get_param_count == nullptr || g_il2cpp_method_get_flags == nullptr) {
        return nullptr;
    }

    void* iterator = nullptr;
    while (const MethodInfo* method = g_il2cpp_class_get_methods(klass, &iterator)) {
        const char* current_name = g_il2cpp_method_get_name(method);
        if (current_name == nullptr || std::strcmp(current_name, method_name) != 0) {
            continue;
        }

        if (g_il2cpp_method_get_param_count(method) != param_count) {
            continue;
        }

        uint32_t iflags = 0;
        const uint32_t flags = g_il2cpp_method_get_flags(method, &iflags);
        if ((flags & kMethodAttributeStatic) == 0) {
            continue;
        }

        if (param_count > 0 && first_param_name_hint != nullptr && g_il2cpp_method_get_param_name != nullptr) {
            const char* param_name = g_il2cpp_method_get_param_name(method, 0);
            if (param_name == nullptr || std::strstr(param_name, first_param_name_hint) == nullptr) {
                continue;
            }
        }

        return method;
    }

    return nullptr;
}

const MethodInfo* FindInstanceMethod(Il2CppClass* klass, const char* method_name, uint32_t param_count) {
    if (klass == nullptr || method_name == nullptr || g_il2cpp_class_get_methods == nullptr ||
        g_il2cpp_method_get_name == nullptr || g_il2cpp_method_get_param_count == nullptr ||
        g_il2cpp_method_get_flags == nullptr) {
        return nullptr;
    }

    void* iterator = nullptr;
    while (const MethodInfo* method = g_il2cpp_class_get_methods(klass, &iterator)) {
        const char* current_name = g_il2cpp_method_get_name(method);
        if (current_name == nullptr || std::strcmp(current_name, method_name) != 0) {
            continue;
        }
        if (g_il2cpp_method_get_param_count(method) != param_count) {
            continue;
        }

        uint32_t iflags = 0;
        const uint32_t flags = g_il2cpp_method_get_flags(method, &iflags);
        if ((flags & kMethodAttributeStatic) != 0) {
            continue;
        }
        return method;
    }
    return nullptr;
}

const MethodInfo* FindInstanceMethodWithFirstParamNameHint(Il2CppClass* klass,
                                                           const char* method_name,
                                                           uint32_t param_count,
                                                           const char* first_param_name_hint) {
    if (klass == nullptr || method_name == nullptr || g_il2cpp_class_get_methods == nullptr ||
        g_il2cpp_method_get_name == nullptr || g_il2cpp_method_get_param_count == nullptr ||
        g_il2cpp_method_get_flags == nullptr) {
        return nullptr;
    }

    void* iterator = nullptr;
    while (const MethodInfo* method = g_il2cpp_class_get_methods(klass, &iterator)) {
        const char* current_name = g_il2cpp_method_get_name(method);
        if (current_name == nullptr || std::strcmp(current_name, method_name) != 0) {
            continue;
        }
        if (g_il2cpp_method_get_param_count(method) != param_count) {
            continue;
        }

        uint32_t iflags = 0;
        const uint32_t flags = g_il2cpp_method_get_flags(method, &iflags);
        if ((flags & kMethodAttributeStatic) != 0) {
            continue;
        }

        if (param_count > 0 && first_param_name_hint != nullptr && g_il2cpp_method_get_param_name != nullptr) {
            const char* param_name = g_il2cpp_method_get_param_name(method, 0);
            if (param_name == nullptr || std::strstr(param_name, first_param_name_hint) == nullptr) {
                continue;
            }
        }

        return method;
    }
    return nullptr;
}

long long MonotonicNowNs() {
    timespec ts{};
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return static_cast<long long>(ts.tv_sec) * 1000000000LL + static_cast<long long>(ts.tv_nsec);
}

bool RefreshIl2CppExecRange() {
    if (g_libil2cpp_exec_start != 0 && g_libil2cpp_exec_end > g_libil2cpp_exec_start) {
        return true;
    }

    FILE* maps = std::fopen("/proc/self/maps", "r");
    if (maps == nullptr) {
        return false;
    }

    char line[768];
    while (std::fgets(line, sizeof(line), maps) != nullptr) {
        if (std::strstr(line, "libil2cpp.so") == nullptr) {
            continue;
        }
        if (std::strstr(line, "r-xp") == nullptr) {
            continue;
        }

        unsigned long long start = 0;
        unsigned long long end = 0;
        if (std::sscanf(line, "%llx-%llx", &start, &end) == 2 && start != 0 && end > start) {
            g_libil2cpp_exec_start = static_cast<uintptr_t>(start);
            g_libil2cpp_exec_end = static_cast<uintptr_t>(end);
            break;
        }
    }

    std::fclose(maps);
    return g_libil2cpp_exec_start != 0 && g_libil2cpp_exec_end > g_libil2cpp_exec_start;
}

bool IsAddressInsideIl2CppText(uintptr_t address) {
    if (!RefreshIl2CppExecRange()) {
        return false;
    }
    return address >= g_libil2cpp_exec_start && address < g_libil2cpp_exec_end;
}

uintptr_t ResolveMethodCodeAddress(const MethodInfo* method) {
    if (method == nullptr) {
        return 0;
    }

    const uintptr_t* words = reinterpret_cast<const uintptr_t*>(method);
    if (words == nullptr) {
        return 0;
    }

    // MethodInfo layout changes across Unity/IL2CPP versions; probe first words and pick one
    // that points into libil2cpp executable range.
    for (int i = 0; i < 8; ++i) {
        const uintptr_t candidate = words[i];
        if (candidate != 0 && IsAddressInsideIl2CppText(candidate)) {
            return candidate;
        }
    }

    return words[0];
}

void RequestImmediateExit(const char* reason) {
    if (g_exit_requested.exchange(true)) {
        return;
    }

    MOD_LOGI("RequestImmediateExit: reason=%s", reason != nullptr ? reason : "unknown");

    JNIEnv* env = nullptr;
    bool attached = false;
    if (GetJniEnv(&env, &attached) && env != nullptr && g_activity_ref != nullptr) {
        jobject activity = g_activity_ref;
        jclass activity_cls = env->GetObjectClass(activity);
        if (activity_cls != nullptr) {
            jmethodID finish_affinity = env->GetMethodID(activity_cls, "finishAffinity", "()V");
            if (finish_affinity != nullptr) {
                env->CallVoidMethod(activity, finish_affinity);
                ClearException(env);
            }

            jmethodID finish = env->GetMethodID(activity_cls, "finish", "()V");
            if (finish != nullptr) {
                env->CallVoidMethod(activity, finish);
                ClearException(env);
            }

            env->DeleteLocalRef(activity_cls);
        } else {
            ClearException(env);
        }

        if (attached) {
            g_vm->DetachCurrentThread();
        }
    }
}

void HookedHomeAction(void* self, const MethodInfo* method) {
    (void)self;
    (void)method;
    MOD_LOGI("HookedHomeAction: blocked Home action.");
}

void HookedExitWithoutSaveAction(void* self, const MethodInfo* method) {
    (void)self;
    (void)method;
    const UiPhase phase = static_cast<UiPhase>(g_ui_phase.load());
    if (phase != UiPhase::kConnected) {
        MOD_LOGI("HookedExitWithoutSaveAction: ignored in phase=%d", static_cast<int>(phase));
        return;
    }
    MOD_LOGI("HookedExitWithoutSaveAction: redirected to immediate app exit.");
    RequestImmediateExit("pause_exit");
}

void HookedMainUpdate(void* self, const MethodInfo* method) {
    if (g_main_update_original != nullptr) {
        g_main_update_original(self, method);
    }
    UpdateRemoteAvatarScene();
}

void TryInstallMainUpdateHook() {
    if (g_main_update_hooked.load()) {
        return;
    }

    CHook::SetLibrary("libil2cpp.so");
    if (CHook::Base() == 0) {
        return;
    }

    void* original = nullptr;
    if (!CHook::Hook(kRvaMainUpdate, reinterpret_cast<void*>(HookedMainUpdate), &original)) {
        MOD_LOGW("TryInstallMainUpdateHook: failed @ 0x%lx", static_cast<unsigned long>(kRvaMainUpdate));
        return;
    }

    g_main_update_original = reinterpret_cast<MainUpdateFn>(original);
    g_main_update_hooked.store(true);
    MOD_LOGI("TryInstallMainUpdateHook: installed @ 0x%lx", static_cast<unsigned long>(kRvaMainUpdate));
}

bool ShouldInspectForPauseActions(const char* klass_name) {
    if (klass_name == nullptr) {
        return false;
    }

    if (std::strcmp(klass_name, "PauseMenu") == 0 ||
        std::strcmp(klass_name, "MenuManager") == 0) {
        return true;
    }

    return std::strstr(klass_name, "PauseMenu") != nullptr;
}

bool HookMethodIfNeeded(const char* klass_name,
                        const MethodInfo* method,
                        void* replacement,
                        std::atomic<bool>* state_flag) {
    if (klass_name == nullptr || method == nullptr || replacement == nullptr || state_flag == nullptr) {
        return false;
    }

    const uintptr_t code = ResolveMethodCodeAddress(method);
    if (code == 0) {
        MOD_LOGW("HookMethodIfNeeded: method code unresolved for %s", klass_name);
        return false;
    }

    {
        std::lock_guard<std::mutex> lock(g_menu_hook_mutex);
        if (std::find(g_menu_hook_targets.begin(), g_menu_hook_targets.end(), code) != g_menu_hook_targets.end()) {
            state_flag->store(true);
            return true;
        }
    }

    void* original = nullptr;
    if (!CHook::HookAbsolute(code, replacement, &original)) {
        MOD_LOGW("HookMethodIfNeeded: Dobby hook failed for %s @ %p", klass_name, reinterpret_cast<void*>(code));
        return false;
    }

    {
        std::lock_guard<std::mutex> lock(g_menu_hook_mutex);
        g_menu_hook_targets.push_back(code);
    }
    state_flag->store(true);
    MOD_LOGI("HookMethodIfNeeded: hooked %s @ %p", klass_name, reinterpret_cast<void*>(code));
    return true;
}

void TryInstallPauseMenuActionHooks() {
    if (!kEnablePauseMenuActionHooks) {
        return;
    }

    if (g_menu_action_hooks_installed.load()) {
        return;
    }

    const long long now_ns = MonotonicNowNs();
    const long long last_ns = g_menu_hook_last_attempt_ns.load();
    if (last_ns > 0 && (now_ns - last_ns) < 1000000000LL) {
        return;
    }
    g_menu_hook_last_attempt_ns.store(now_ns);

    if (!ResolveIl2CppSceneApi()) {
        return;
    }

    const Il2CppImage* image = FindImageByNameContains("Assembly-CSharp");
    if (image == nullptr || g_il2cpp_image_get_class_count == nullptr || g_il2cpp_image_get_class == nullptr ||
        g_il2cpp_class_get_name == nullptr || g_il2cpp_class_get_methods == nullptr ||
        g_il2cpp_method_get_name == nullptr || g_il2cpp_method_get_param_count == nullptr) {
        return;
    }

    int found_home = 0;
    int found_exit_without_save = 0;
    int hooked_home = 0;
    int hooked_exit_without_save = 0;

    const size_t class_count = g_il2cpp_image_get_class_count(image);
    for (size_t i = 0; i < class_count; ++i) {
        const Il2CppClass* klass_const = g_il2cpp_image_get_class(image, i);
        if (klass_const == nullptr) {
            continue;
        }

        Il2CppClass* klass = const_cast<Il2CppClass*>(klass_const);
        const char* klass_name = g_il2cpp_class_get_name(klass);
        if (!ShouldInspectForPauseActions(klass_name)) {
            continue;
        }

        void* iterator = nullptr;
        while (const MethodInfo* method = g_il2cpp_class_get_methods(klass, &iterator)) {
            const char* method_name = g_il2cpp_method_get_name(method);
            if (method_name == nullptr || g_il2cpp_method_get_param_count(method) != 0) {
                continue;
            }

            if (std::strcmp(method_name, "Home") == 0) {
                ++found_home;
                if (HookMethodIfNeeded(klass_name, method, reinterpret_cast<void*>(HookedHomeAction), &g_home_action_hooked)) {
                    ++hooked_home;
                }
                continue;
            }

            if (std::strcmp(method_name, "ExitWithoutSave") == 0) {
                ++found_exit_without_save;
                if (HookMethodIfNeeded(klass_name,
                                       method,
                                       reinterpret_cast<void*>(HookedExitWithoutSaveAction),
                                       &g_exit_without_save_hooked)) {
                    ++hooked_exit_without_save;
                }
                continue;
            }
        }
    }

    MOD_LOGI("TryInstallPauseMenuActionHooks: found(home=%d exitNoSave=%d) hooked(home=%d exitNoSave=%d)",
             found_home,
             found_exit_without_save,
             hooked_home,
             hooked_exit_without_save);

    if (g_home_action_hooked.load() && g_exit_without_save_hooked.load()) {
        g_menu_action_hooks_installed.store(true);
        MOD_LOGI("TryInstallPauseMenuActionHooks: hooks installed.");
    }
}

bool ResolveIl2CppSceneApi() {
    if (g_scene_api_ready.load()) {
        return true;
    }

    if (g_libil2cpp_handle == nullptr) {
        g_libil2cpp_handle = dlopen("libil2cpp.so", RTLD_NOW | RTLD_NOLOAD);
        if (g_libil2cpp_handle == nullptr) {
            g_libil2cpp_handle = dlopen("libil2cpp.so", RTLD_NOW);
        }
    }

    if (g_libil2cpp_handle == nullptr) {
        MOD_LOGW("ResolveIl2CppSceneApi: libil2cpp handle unavailable.");
        return false;
    }

#define RESOLVE_SYM(var_name, symbol_name)                                                     \
    do {                                                                                        \
        var_name = reinterpret_cast<decltype(var_name)>(dlsym(g_libil2cpp_handle, symbol_name)); \
        if (var_name == nullptr) {                                                              \
            MOD_LOGW("ResolveIl2CppSceneApi: missing symbol %s", symbol_name);                 \
            return false;                                                                       \
        }                                                                                       \
    } while (0)

    RESOLVE_SYM(g_il2cpp_domain_get, "il2cpp_domain_get");
    RESOLVE_SYM(g_il2cpp_domain_get_assemblies, "il2cpp_domain_get_assemblies");
    RESOLVE_SYM(g_il2cpp_assembly_get_image, "il2cpp_assembly_get_image");
    RESOLVE_SYM(g_il2cpp_image_get_name, "il2cpp_image_get_name");
    RESOLVE_SYM(g_il2cpp_image_get_class_count, "il2cpp_image_get_class_count");
    RESOLVE_SYM(g_il2cpp_image_get_class, "il2cpp_image_get_class");
    RESOLVE_SYM(g_il2cpp_class_from_name, "il2cpp_class_from_name");
    RESOLVE_SYM(g_il2cpp_class_get_methods, "il2cpp_class_get_methods");
    RESOLVE_SYM(g_il2cpp_class_get_name, "il2cpp_class_get_name");
    RESOLVE_SYM(g_il2cpp_class_get_namespace, "il2cpp_class_get_namespace");
    RESOLVE_SYM(g_il2cpp_class_get_method_from_name, "il2cpp_class_get_method_from_name");
    RESOLVE_SYM(g_il2cpp_method_get_name, "il2cpp_method_get_name");
    RESOLVE_SYM(g_il2cpp_method_get_param_count, "il2cpp_method_get_param_count");
    RESOLVE_SYM(g_il2cpp_method_get_flags, "il2cpp_method_get_flags");
    RESOLVE_SYM(g_il2cpp_method_get_param_name, "il2cpp_method_get_param_name");
    RESOLVE_SYM(g_il2cpp_runtime_invoke, "il2cpp_runtime_invoke");
    RESOLVE_SYM(g_il2cpp_object_get_class, "il2cpp_object_get_class");
    RESOLVE_SYM(g_il2cpp_object_unbox, "il2cpp_object_unbox");
    RESOLVE_SYM(g_il2cpp_object_new, "il2cpp_object_new");

#undef RESOLVE_SYM

    const Il2CppImage* core_image = FindImageByNameContains("UnityEngine.CoreModule");
    if (core_image == nullptr) {
        MOD_LOGW("ResolveIl2CppSceneApi: UnityEngine.CoreModule image not found.");
        return false;
    }

    Il2CppClass* scene_manager_class = g_il2cpp_class_from_name(
        core_image, "UnityEngine.SceneManagement", "SceneManager");
    if (scene_manager_class == nullptr) {
        MOD_LOGW("ResolveIl2CppSceneApi: SceneManager class not found.");
        return false;
    }

    g_scene_manager_load_scene_int = FindStaticMethod(
        scene_manager_class, "LoadScene", 1, "sceneBuildIndex");
    if (g_scene_manager_load_scene_int == nullptr) {
        g_scene_manager_load_scene_int = FindStaticMethod(
            scene_manager_class, "LoadScene", 1, nullptr);
    }
    g_scene_manager_get_scene_count = FindStaticMethod(
        scene_manager_class, "get_sceneCountInBuildSettings", 0, nullptr);
    g_scene_manager_get_active_scene = FindStaticMethod(
        scene_manager_class, "GetActiveScene", 0, nullptr);

    Il2CppClass* scene_struct_class = g_il2cpp_class_from_name(
        core_image, "UnityEngine.SceneManagement", "Scene");
    if (scene_struct_class != nullptr && g_il2cpp_class_get_method_from_name != nullptr) {
        g_scene_get_build_index = g_il2cpp_class_get_method_from_name(
            scene_struct_class, "get_buildIndex", 0);
    }

    if (g_scene_manager_load_scene_int == nullptr) {
        MOD_LOGW("ResolveIl2CppSceneApi: LoadScene(int) method not found.");
        return false;
    }

    g_scene_api_ready.store(true);
    MOD_LOGI("ResolveIl2CppSceneApi: SceneManager methods resolved.");
    return true;
}

int GetSceneCountInBuildSettings() {
    if (!ResolveIl2CppSceneApi() || g_scene_manager_get_scene_count == nullptr ||
        g_il2cpp_runtime_invoke == nullptr || g_il2cpp_object_unbox == nullptr) {
        return -1;
    }

    Il2CppException* exception = nullptr;
    Il2CppObject* boxed = g_il2cpp_runtime_invoke(
        g_scene_manager_get_scene_count, nullptr, nullptr, &exception);
    if (exception != nullptr || boxed == nullptr) {
        return -1;
    }

    void* raw = g_il2cpp_object_unbox(boxed);
    if (raw == nullptr) {
        return -1;
    }

    return *reinterpret_cast<int*>(raw);
}

int GetActiveSceneBuildIndex() {
    if (!ResolveIl2CppSceneApi() || g_scene_manager_get_active_scene == nullptr ||
        g_scene_get_build_index == nullptr || g_il2cpp_runtime_invoke == nullptr ||
        g_il2cpp_object_unbox == nullptr) {
        return -1;
    }

    Il2CppException* exception = nullptr;
    Il2CppObject* scene_box = g_il2cpp_runtime_invoke(
        g_scene_manager_get_active_scene, nullptr, nullptr, &exception);
    if (exception != nullptr || scene_box == nullptr) {
        return -1;
    }

    exception = nullptr;
    Il2CppObject* build_idx_box = g_il2cpp_runtime_invoke(
        g_scene_get_build_index, scene_box, nullptr, &exception);
    if (exception != nullptr || build_idx_box == nullptr) {
        return -1;
    }

    void* raw = g_il2cpp_object_unbox(build_idx_box);
    if (raw == nullptr) {
        return -1;
    }
    return *reinterpret_cast<int*>(raw);
}

void LogAssemblyCSharpMenuCandidates() {
    if (g_scene_candidates_logged.exchange(true)) {
        return;
    }
    if (!ResolveIl2CppSceneApi()) {
        return;
    }

    const Il2CppImage* image = FindImageByNameContains("Assembly-CSharp");
    if (image == nullptr || g_il2cpp_image_get_class_count == nullptr ||
        g_il2cpp_image_get_class == nullptr || g_il2cpp_class_get_name == nullptr ||
        g_il2cpp_class_get_namespace == nullptr) {
        return;
    }

    const size_t class_count = g_il2cpp_image_get_class_count(image);
    int logged = 0;
    for (size_t i = 0; i < class_count; ++i) {
        const Il2CppClass* klass_const = g_il2cpp_image_get_class(image, i);
        if (klass_const == nullptr) {
            continue;
        }

        Il2CppClass* klass = const_cast<Il2CppClass*>(klass_const);
        const char* klass_name = g_il2cpp_class_get_name(klass);
        const char* ns_name = g_il2cpp_class_get_namespace(klass);
        if (klass_name == nullptr) {
            continue;
        }

        if (std::strstr(klass_name, "Menu") == nullptr &&
            std::strstr(klass_name, "Start") == nullptr &&
            std::strstr(klass_name, "Game") == nullptr) {
            continue;
        }

        MOD_LOGI("Assembly-CSharp candidate class: %s.%s",
                 ns_name != nullptr ? ns_name : "",
                 klass_name);
        ++logged;
        if (logged >= 80) {
            break;
        }
    }

    MOD_LOGI("Assembly-CSharp candidate scan done. logged=%d", logged);
}

bool ForceLoadWorldScene(int requested_world_id) {
    if (!ResolveIl2CppSceneApi()) {
        return false;
    }
    if (g_scene_manager_load_scene_int == nullptr || g_il2cpp_runtime_invoke == nullptr) {
        return false;
    }

    const int scene_count = GetSceneCountInBuildSettings();
    int target_scene = requested_world_id;
    if (target_scene < 0) {
        target_scene = 0;
    }
    if (scene_count > 0 && target_scene >= scene_count) {
        target_scene = (scene_count > 1) ? 1 : 0;
    }

    void* args[1];
    args[0] = &target_scene;
    Il2CppException* exception = nullptr;
    g_il2cpp_runtime_invoke(g_scene_manager_load_scene_int, nullptr, args, &exception);
    if (exception != nullptr) {
        MOD_LOGW("ForceLoadWorldScene: runtime exception while calling LoadScene(%d).", target_scene);
        return false;
    }

    MOD_LOGI("ForceLoadWorldScene: requested=%d target=%d sceneCount=%d",
             requested_world_id,
             target_scene,
             scene_count);
    return true;
}

bool ParseRemotePlayerStateObject(const std::string& json, RemotePlayerState* out_state) {
    if (out_state == nullptr) {
        return false;
    }

    RemotePlayerState state;
    if (!ExtractJsonString(json, "playerId", &state.player_id) || state.player_id.empty()) {
        return false;
    }
    if (!ExtractJsonString(json, "name", &state.name) || state.name.empty()) {
        state.name = state.player_id;
    }
    ExtractJsonInt(json, "worldId", &state.world_id);

    const auto parse_transform = [&](const std::string& owner_json) {
        std::string transform_json;
        if (!ExtractJsonObject(owner_json, "transform", &transform_json)) {
            return;
        }

        double x = 0.0;
        double y = 1.0;
        double z = 0.0;
        const bool has_x = ExtractJsonDouble(transform_json, "x", &x);
        const bool has_y = ExtractJsonDouble(transform_json, "y", &y);
        const bool has_z = ExtractJsonDouble(transform_json, "z", &z);
        if (has_x && has_y && has_z) {
            state.has_transform = true;
            state.x = static_cast<float>(x);
            state.y = static_cast<float>(y);
            state.z = static_cast<float>(z);
        }
    };

    // Accept both legacy world_state.transform and current world_state.state.transform layouts.
    parse_transform(json);
    if (!state.has_transform) {
        std::string state_json;
        if (ExtractJsonObject(json, "state", &state_json)) {
            parse_transform(state_json);
        }
    }

    *out_state = state;
    return true;
}

void HandleWorldStatePacket(const std::string& line) {
    size_t array_open = std::string::npos;
    size_t array_close = std::string::npos;
    if (!ExtractJsonArrayBounds(line, "players", &array_open, &array_close)) {
        return;
    }

    std::vector<std::string> player_objects;
    SplitTopLevelJsonObjects(line, array_open, array_close, &player_objects);
    if (player_objects.empty()) {
        return;
    }

    std::unordered_map<std::string, RemotePlayerState> next;
    next.reserve(player_objects.size());
    for (const std::string& obj : player_objects) {
        RemotePlayerState state;
        if (!ParseRemotePlayerStateObject(obj, &state)) {
            continue;
        }
        if (state.player_id == g_client_player_id) {
            continue;
        }
        next[state.player_id] = state;
    }

    {
        std::lock_guard<std::mutex> lock(g_remote_players_mutex);
        g_remote_players.swap(next);
    }
}

bool ResolveRemoteAvatarApi() {
    if (g_remote_avatar_api_ready.load()) {
        return true;
    }
    if (!ResolveIl2CppSceneApi()) {
        return false;
    }

    if (g_il2cpp_string_new == nullptr) {
        g_il2cpp_string_new = reinterpret_cast<Il2CppStringNewFn>(dlsym(g_libil2cpp_handle, "il2cpp_string_new"));
    }
    if (g_il2cpp_class_get_type == nullptr) {
        g_il2cpp_class_get_type =
            reinterpret_cast<Il2CppClassGetTypeFn>(dlsym(g_libil2cpp_handle, "il2cpp_class_get_type"));
    }
    if (g_il2cpp_type_get_object == nullptr) {
        g_il2cpp_type_get_object =
            reinterpret_cast<Il2CppTypeGetObjectFn>(dlsym(g_libil2cpp_handle, "il2cpp_type_get_object"));
    }

    const Il2CppImage* core_image = FindImageByNameContains("UnityEngine.CoreModule");
    if (core_image == nullptr || g_il2cpp_class_from_name == nullptr) {
        return false;
    }

    Il2CppClass* game_object_class = g_il2cpp_class_from_name(core_image, "UnityEngine", "GameObject");
    Il2CppClass* transform_class = g_il2cpp_class_from_name(core_image, "UnityEngine", "Transform");
    Il2CppClass* behaviour_class = g_il2cpp_class_from_name(core_image, "UnityEngine", "Behaviour");
    Il2CppClass* object_class = g_il2cpp_class_from_name(core_image, "UnityEngine", "Object");
    g_collider_class = g_il2cpp_class_from_name(core_image, "UnityEngine", "Collider");
    g_text_mesh_class = g_il2cpp_class_from_name(core_image, "UnityEngine", "TextMesh");
    if (g_text_mesh_class == nullptr) {
        const Il2CppImage* text_mesh_pro_image = FindImageByNameContains("Unity.TextMeshPro");
        if (text_mesh_pro_image != nullptr) {
            g_text_mesh_class = g_il2cpp_class_from_name(text_mesh_pro_image, "TMPro", "TextMeshPro");
        }
    }
    if (game_object_class == nullptr || transform_class == nullptr || object_class == nullptr) {
        return false;
    }
    g_game_object_class_cached = game_object_class;

    g_game_object_create_primitive = FindStaticMethod(game_object_class, "CreatePrimitive", 1, nullptr);
    g_game_object_find = FindStaticMethod(game_object_class, "Find", 1, nullptr);
    g_game_object_ctor_name = FindInstanceMethod(game_object_class, ".ctor", 1);
    g_game_object_add_component_by_type = FindInstanceMethod(game_object_class, "AddComponent", 1);
    g_game_object_get_transform = FindInstanceMethod(game_object_class, "get_transform", 0);
    g_game_object_get_component_by_type = FindInstanceMethod(game_object_class, "GetComponent", 1);
    g_transform_set_position = FindInstanceMethod(transform_class, "set_position", 1);
    g_transform_set_local_position = FindInstanceMethod(transform_class, "set_localPosition", 1);
    g_transform_set_local_scale = FindInstanceMethod(transform_class, "set_localScale", 1);
    g_transform_set_parent_with_world_stays = FindInstanceMethod(transform_class, "SetParent", 2);
    g_transform_set_parent = FindInstanceMethod(transform_class, "SetParent", 1);
    g_behaviour_set_enabled = FindInstanceMethod(behaviour_class, "set_enabled", 1);
    if (g_text_mesh_class != nullptr) {
        g_text_mesh_set_text = FindInstanceMethod(g_text_mesh_class, "set_text", 1);
        g_text_mesh_set_character_size = FindInstanceMethod(g_text_mesh_class, "set_characterSize", 1);
        g_text_mesh_set_font_size = FindInstanceMethod(g_text_mesh_class, "set_fontSize", 1);
    }
    g_object_set_name = FindInstanceMethod(object_class, "set_name", 1);
    g_object_destroy = FindStaticMethod(object_class, "Destroy", 1, nullptr);

    if (g_game_object_create_primitive == nullptr || g_game_object_get_transform == nullptr ||
        g_transform_set_position == nullptr || g_transform_set_local_scale == nullptr ||
        g_object_destroy == nullptr || g_il2cpp_runtime_invoke == nullptr) {
        MOD_LOGW("ResolveRemoteAvatarApi: missing required methods.");
        return false;
    }

    g_remote_avatar_api_ready.store(true);
    MOD_LOGI("ResolveRemoteAvatarApi: ready.");
    return true;
}

void DestroyRemoteAvatarObject(Il2CppObject* avatar) {
    if (avatar == nullptr || g_object_destroy == nullptr || g_il2cpp_runtime_invoke == nullptr) {
        return;
    }
    void* args[1];
    args[0] = avatar;
    Il2CppException* exception = nullptr;
    g_il2cpp_runtime_invoke(g_object_destroy, nullptr, args, &exception);
    (void)exception;
}

Il2CppObject* GetTransform(Il2CppObject* game_object) {
    if (game_object == nullptr || g_game_object_get_transform == nullptr || g_il2cpp_runtime_invoke == nullptr) {
        return nullptr;
    }
    Il2CppException* exception = nullptr;
    Il2CppObject* transform = g_il2cpp_runtime_invoke(g_game_object_get_transform, game_object, nullptr, &exception);
    if (exception != nullptr) {
        return nullptr;
    }
    return transform;
}

void SetTransformPosition(Il2CppObject* transform, float x, float y, float z) {
    if (transform == nullptr || g_transform_set_position == nullptr || g_il2cpp_runtime_invoke == nullptr) {
        return;
    }
    UnityVec3 pos{x, y, z};
    void* args[1];
    args[0] = &pos;
    Il2CppException* exception = nullptr;
    g_il2cpp_runtime_invoke(g_transform_set_position, transform, args, &exception);
    (void)exception;
}

void SetTransformLocalPosition(Il2CppObject* transform, float x, float y, float z) {
    if (transform == nullptr || g_transform_set_local_position == nullptr || g_il2cpp_runtime_invoke == nullptr) {
        return;
    }
    UnityVec3 pos{x, y, z};
    void* args[1];
    args[0] = &pos;
    Il2CppException* exception = nullptr;
    g_il2cpp_runtime_invoke(g_transform_set_local_position, transform, args, &exception);
    (void)exception;
}

void SetTransformScale(Il2CppObject* transform, float x, float y, float z) {
    if (transform == nullptr || g_transform_set_local_scale == nullptr || g_il2cpp_runtime_invoke == nullptr) {
        return;
    }
    UnityVec3 scale{x, y, z};
    void* args[1];
    args[0] = &scale;
    Il2CppException* exception = nullptr;
    g_il2cpp_runtime_invoke(g_transform_set_local_scale, transform, args, &exception);
    (void)exception;
}

void SetTransformParent(Il2CppObject* child_transform, Il2CppObject* parent_transform) {
    if (child_transform == nullptr || parent_transform == nullptr || g_il2cpp_runtime_invoke == nullptr) {
        return;
    }

    Il2CppException* exception = nullptr;
    if (g_transform_set_parent_with_world_stays != nullptr) {
        bool world_position_stays = false;
        void* args[2];
        args[0] = parent_transform;
        args[1] = &world_position_stays;
        g_il2cpp_runtime_invoke(g_transform_set_parent_with_world_stays, child_transform, args, &exception);
        return;
    }

    if (g_transform_set_parent != nullptr) {
        void* args[1];
        args[0] = parent_transform;
        g_il2cpp_runtime_invoke(g_transform_set_parent, child_transform, args, &exception);
    }
}

Il2CppObject* CreatePrimitive(int primitive_type) {
    if (!ResolveRemoteAvatarApi()) {
        return nullptr;
    }
    void* args[1];
    args[0] = &primitive_type;
    Il2CppException* exception = nullptr;
    Il2CppObject* created = g_il2cpp_runtime_invoke(g_game_object_create_primitive, nullptr, args, &exception);
    if (exception != nullptr) {
        return nullptr;
    }
    return created;
}

void DisableColliderOnGameObject(Il2CppObject* game_object) {
    if (game_object == nullptr || g_game_object_get_component_by_type == nullptr ||
        g_behaviour_set_enabled == nullptr || g_il2cpp_class_get_type == nullptr ||
        g_il2cpp_type_get_object == nullptr || g_collider_class == nullptr ||
        g_il2cpp_runtime_invoke == nullptr) {
        return;
    }

    const Il2CppType* collider_type = g_il2cpp_class_get_type(g_collider_class);
    if (collider_type == nullptr) {
        return;
    }
    Il2CppObject* type_object = g_il2cpp_type_get_object(collider_type);
    if (type_object == nullptr) {
        return;
    }

    void* get_args[1];
    get_args[0] = type_object;
    Il2CppException* exception = nullptr;
    Il2CppObject* collider_component =
        g_il2cpp_runtime_invoke(g_game_object_get_component_by_type, game_object, get_args, &exception);
    if (exception != nullptr || collider_component == nullptr) {
        return;
    }

    bool enabled = false;
    void* set_args[1];
    set_args[0] = &enabled;
    exception = nullptr;
    g_il2cpp_runtime_invoke(g_behaviour_set_enabled, collider_component, set_args, &exception);
    (void)exception;
}

Il2CppObject* CreateNamedGameObject(const std::string& object_name) {
    if (!ResolveRemoteAvatarApi() || g_game_object_class_cached == nullptr ||
        g_il2cpp_object_new == nullptr || g_game_object_ctor_name == nullptr ||
        g_il2cpp_runtime_invoke == nullptr || g_il2cpp_string_new == nullptr) {
        return nullptr;
    }

    Il2CppObject* game_object = g_il2cpp_object_new(g_game_object_class_cached);
    if (game_object == nullptr) {
        return nullptr;
    }

    Il2CppString* name_str = g_il2cpp_string_new(object_name.c_str());
    if (name_str == nullptr) {
        return nullptr;
    }

    void* args[1];
    args[0] = name_str;
    Il2CppException* exception = nullptr;
    g_il2cpp_runtime_invoke(g_game_object_ctor_name, game_object, args, &exception);
    if (exception != nullptr) {
        return nullptr;
    }
    return game_object;
}

void AttachPlayerNameTag(Il2CppObject* root_transform, const std::string& player_name) {
    if (root_transform == nullptr || player_name.empty() ||
        g_game_object_add_component_by_type == nullptr || g_text_mesh_class == nullptr ||
        g_il2cpp_class_get_type == nullptr || g_il2cpp_type_get_object == nullptr ||
        g_il2cpp_runtime_invoke == nullptr) {
        if (!g_name_tag_api_missing_logged.exchange(true)) {
            MOD_LOGW("AttachPlayerNameTag: api missing add=%d textClass=%d classType=%d typeObj=%d invoke=%d",
                     g_game_object_add_component_by_type != nullptr ? 1 : 0,
                     g_text_mesh_class != nullptr ? 1 : 0,
                     g_il2cpp_class_get_type != nullptr ? 1 : 0,
                     g_il2cpp_type_get_object != nullptr ? 1 : 0,
                     g_il2cpp_runtime_invoke != nullptr ? 1 : 0);
        }
        return;
    }

    std::string tag_name = "MPTag_" + player_name;
    if (tag_name.size() > 60) {
        tag_name.resize(60);
    }
    Il2CppObject* label_go = CreateNamedGameObject(tag_name);
    if (label_go == nullptr) {
        return;
    }

    Il2CppObject* label_transform = GetTransform(label_go);
    if (label_transform == nullptr) {
        return;
    }

    SetTransformParent(label_transform, root_transform);
    SetTransformLocalPosition(label_transform, 0.0f, 3.10f, 0.0f);
    SetTransformScale(label_transform, 1.0f, 1.0f, 1.0f);

    const Il2CppType* text_mesh_type = g_il2cpp_class_get_type(g_text_mesh_class);
    if (text_mesh_type == nullptr) {
        return;
    }
    Il2CppObject* text_mesh_type_object = g_il2cpp_type_get_object(text_mesh_type);
    if (text_mesh_type_object == nullptr) {
        return;
    }

    void* add_args[1];
    add_args[0] = text_mesh_type_object;
    Il2CppException* exception = nullptr;
    Il2CppObject* text_mesh_component =
        g_il2cpp_runtime_invoke(g_game_object_add_component_by_type, label_go, add_args, &exception);
    if (exception != nullptr || text_mesh_component == nullptr) {
        return;
    }

    if (g_text_mesh_set_text != nullptr && g_il2cpp_string_new != nullptr) {
        Il2CppString* text_value = g_il2cpp_string_new(player_name.c_str());
        if (text_value != nullptr) {
            void* args[1];
            args[0] = text_value;
            exception = nullptr;
            g_il2cpp_runtime_invoke(g_text_mesh_set_text, text_mesh_component, args, &exception);
        }
    }

    if (g_text_mesh_set_character_size != nullptr) {
        float char_size = 0.22f;
        void* args[1];
        args[0] = &char_size;
        exception = nullptr;
        g_il2cpp_runtime_invoke(g_text_mesh_set_character_size, text_mesh_component, args, &exception);
    }

    if (g_text_mesh_set_font_size != nullptr) {
        int font_size = 48;
        void* args[1];
        args[0] = &font_size;
        exception = nullptr;
        g_il2cpp_runtime_invoke(g_text_mesh_set_font_size, text_mesh_component, args, &exception);
    }

    MOD_LOGI("AttachPlayerNameTag: %s", player_name.c_str());
}

void AttachHumanoidPart(Il2CppObject* root_transform,
                        int primitive_type,
                        float local_x,
                        float local_y,
                        float local_z,
                        float scale_x,
                        float scale_y,
                        float scale_z) {
    if (root_transform == nullptr) {
        return;
    }
    if (g_transform_set_local_position == nullptr ||
        (g_transform_set_parent_with_world_stays == nullptr && g_transform_set_parent == nullptr)) {
        return;
    }

    Il2CppObject* part_go = CreatePrimitive(primitive_type);
    if (part_go == nullptr) {
        return;
    }

    Il2CppObject* part_transform = GetTransform(part_go);
    if (part_transform == nullptr) {
        return;
    }

    DisableColliderOnGameObject(part_go);
    SetTransformParent(part_transform, root_transform);
    SetTransformLocalPosition(part_transform, local_x, local_y, local_z);
    SetTransformScale(part_transform, scale_x, scale_y, scale_z);
}

Il2CppObject* CreateRemoteAvatarObject(const RemotePlayerState& state) {
    if (!ResolveRemoteAvatarApi()) {
        return nullptr;
    }

    constexpr int kPrimitiveSphere = 0;   // UnityEngine.PrimitiveType.Sphere
    constexpr int kPrimitiveCapsule = 1;  // UnityEngine.PrimitiveType.Capsule

    Il2CppObject* avatar = CreatePrimitive(kPrimitiveCapsule);
    Il2CppException* exception = nullptr;
    if (exception != nullptr || avatar == nullptr) {
        MOD_LOGW("CreateRemoteAvatarObject: CreatePrimitive failed for %s", state.player_id.c_str());
        return nullptr;
    }
    DisableColliderOnGameObject(avatar);

    if (g_object_set_name != nullptr && g_il2cpp_string_new != nullptr) {
        std::string obj_name = "MP_" + state.name;
        if (obj_name.size() > 60) {
            obj_name.resize(60);
        }
        Il2CppString* name_str = g_il2cpp_string_new(obj_name.c_str());
        if (name_str != nullptr) {
            void* name_args[1];
            name_args[0] = name_str;
            exception = nullptr;
            g_il2cpp_runtime_invoke(g_object_set_name, avatar, name_args, &exception);
            (void)exception;
        }
    }

    Il2CppObject* root_transform = GetTransform(avatar);
    constexpr float kDownscale = (1.0f / 1.5f);
    constexpr float kBodyScaleX = 2.90f * kDownscale;
    constexpr float kBodyScaleY = 3.30f * kDownscale;
    constexpr float kBodyScaleZ = 1.90f * kDownscale;
    SetTransformScale(root_transform, kBodyScaleX, kBodyScaleY, kBodyScaleZ);

    const auto attach_world_part = [&](int primitive_type,
                                       float world_x,
                                       float world_y,
                                       float world_z,
                                       float world_scale_x,
                                       float world_scale_y,
                                       float world_scale_z) {
        if (kBodyScaleX <= 0.0f || kBodyScaleY <= 0.0f || kBodyScaleZ <= 0.0f) {
            return;
        }
        AttachHumanoidPart(root_transform,
                           primitive_type,
                           world_x / kBodyScaleX,
                           world_y / kBodyScaleY,
                           world_z / kBodyScaleZ,
                           world_scale_x / kBodyScaleX,
                           world_scale_y / kBodyScaleY,
                           world_scale_z / kBodyScaleZ);
    };

    // Keep the head sphere tangent to the body top.
    constexpr float kHeadDiameter = 1.45f * kDownscale;
    const float head_center_y = kBodyScaleY + (kHeadDiameter * 0.5f);
    attach_world_part(kPrimitiveSphere, 0.0f, head_center_y, 0.0f, kHeadDiameter, kHeadDiameter, kHeadDiameter);

    // Arms
    attach_world_part(kPrimitiveCapsule, -1.95f * kDownscale, 0.92f * kDownscale, 0.0f, 0.70f * kDownscale, 1.75f * kDownscale, 0.70f * kDownscale);
    attach_world_part(kPrimitiveCapsule, 1.95f * kDownscale, 0.92f * kDownscale, 0.0f, 0.70f * kDownscale, 1.75f * kDownscale, 0.70f * kDownscale);

    // Legs (3x+ larger than previous setup).
    attach_world_part(kPrimitiveCapsule, -0.86f * kDownscale, -3.45f * kDownscale, 0.0f, 0.95f * kDownscale, 2.70f * kDownscale, 0.95f * kDownscale);
    attach_world_part(kPrimitiveCapsule, 0.86f * kDownscale, -3.45f * kDownscale, 0.0f, 0.95f * kDownscale, 2.70f * kDownscale, 0.95f * kDownscale);
    AttachPlayerNameTag(root_transform, state.name);

    MOD_LOGI("Remote avatar created: %s", state.player_id.c_str());
    return avatar;
}

void ApplyRemoteAvatarTransform(Il2CppObject* avatar, const RemotePlayerState& state) {
    if (avatar == nullptr || g_game_object_get_transform == nullptr || g_transform_set_position == nullptr ||
        g_il2cpp_runtime_invoke == nullptr) {
        return;
    }

    Il2CppObject* transform = GetTransform(avatar);
    if (transform == nullptr) {
        return;
    }

    constexpr float kAvatarGroundYOffset = -2.50f;
    SetTransformPosition(transform, state.x, state.y + kAvatarGroundYOffset, state.z);
}

void UpdateRemoteAvatarScene() {
    const UiPhase phase = static_cast<UiPhase>(g_ui_phase.load());
    if (phase != UiPhase::kConnected) {
        if (!g_remote_avatar_objects.empty() && ResolveRemoteAvatarApi()) {
            for (auto& it : g_remote_avatar_objects) {
                DestroyRemoteAvatarObject(it.second);
            }
            g_remote_avatar_objects.clear();
            g_remote_avatar_last_positions.clear();
        }
        return;
    }

    if (!ResolveRemoteAvatarApi()) {
        return;
    }

    std::unordered_map<std::string, RemotePlayerState> snapshot;
    {
        std::lock_guard<std::mutex> lock(g_remote_players_mutex);
        snapshot = g_remote_players;
    }

    std::set<std::string> active_ids;
    for (auto& [player_id, state] : snapshot) {
        if (player_id.empty() || player_id == g_client_player_id) {
            continue;
        }

        if (!state.has_transform) {
            const size_t h = std::hash<std::string>{}(player_id);
            const int idx = static_cast<int>(h % 6U);
            static const float fallback_pos[6][2] = {
                {2.0f, 2.0f}, {-2.0f, 2.0f}, {2.0f, -2.0f},
                {-2.0f, -2.0f}, {3.0f, 0.0f}, {0.0f, 3.0f},
            };
            state.has_transform = true;
            state.x = fallback_pos[idx][0];
            state.y = 1.0f;
            state.z = fallback_pos[idx][1];
        }
        if (!state.has_transform) {
            continue;
        }

        active_ids.insert(player_id);
        Il2CppObject* avatar = nullptr;
        auto found = g_remote_avatar_objects.find(player_id);
        if (found == g_remote_avatar_objects.end()) {
            avatar = CreateRemoteAvatarObject(state);
            if (avatar == nullptr) {
                continue;
            }
            g_remote_avatar_objects[player_id] = avatar;
            PushChatEvent(state.name + " avatar spawned.");
        } else {
            avatar = found->second;
        }

        bool should_apply = true;
        auto last_it = g_remote_avatar_last_positions.find(player_id);
        if (last_it != g_remote_avatar_last_positions.end()) {
            const float dx = state.x - last_it->second.x;
            const float dy = state.y - last_it->second.y;
            const float dz = state.z - last_it->second.z;
            const float dist_sq = (dx * dx) + (dy * dy) + (dz * dz);
            if (dist_sq < 0.0064f) {  // ~8cm, skip tiny jitter updates
                should_apply = false;
            }
        }

        if (should_apply) {
            ApplyRemoteAvatarTransform(avatar, state);
            g_remote_avatar_last_positions[player_id] = UnityVec3{state.x, state.y, state.z};
        }
    }

    for (auto it = g_remote_avatar_objects.begin(); it != g_remote_avatar_objects.end();) {
        if (active_ids.find(it->first) == active_ids.end()) {
            DestroyRemoteAvatarObject(it->second);
            g_remote_avatar_last_positions.erase(it->first);
            it = g_remote_avatar_objects.erase(it);
        } else {
            ++it;
        }
    }
}

void TryHidePauseMenuButtons() {
    const UiPhase phase = static_cast<UiPhase>(g_ui_phase.load());
    if (phase != UiPhase::kConnected && phase != UiPhase::kJoiningWorld) {
        return;
    }

    const long long now_ns = MonotonicNowNs();
    const long long last_ns = g_last_pause_buttons_hide_ns.load();
    if (last_ns > 0 && (now_ns - last_ns) < 5000000000LL) {
        return;
    }
    g_last_pause_buttons_hide_ns.store(now_ns);

    if (!ResolveRemoteAvatarApi() || g_game_object_find == nullptr ||
        g_il2cpp_string_new == nullptr || g_object_destroy == nullptr ||
        g_il2cpp_runtime_invoke == nullptr) {
        return;
    }

    static const char* kButtonNames[] = {
        "Home",
        "ExitWithoutSave",
        "Exit Without Save",
    };

    int removed = 0;
    for (const char* button_name : kButtonNames) {
        Il2CppString* name_str = g_il2cpp_string_new(button_name);
        if (name_str == nullptr) {
            continue;
        }

        void* args[1];
        args[0] = name_str;
        Il2CppException* exception = nullptr;
        Il2CppObject* found = g_il2cpp_runtime_invoke(g_game_object_find, nullptr, args, &exception);
        if (exception != nullptr || found == nullptr) {
            continue;
        }

        DestroyRemoteAvatarObject(found);
        ++removed;
    }

    if (removed > 0) {
        MOD_LOGI("TryHidePauseMenuButtons: removed=%d", removed);
    }
}

void RequestUnityThreadAvatarTick(JNIEnv* env) {
    if (env == nullptr || g_activity_ref == nullptr || g_activity_pump_unity_tick == nullptr) {
        return;
    }
    if (static_cast<UiPhase>(g_ui_phase.load()) != UiPhase::kConnected) {
        return;
    }

    const long long now_ns = MonotonicNowNs();
    const long long last_ns = g_last_unity_tick_request_ns.load();
    if (last_ns > 0 && (now_ns - last_ns) < 220000000LL) {
        return;
    }

    bool expected = false;
    if (!g_unity_tick_pending.compare_exchange_strong(expected, true)) {
        return;
    }
    g_last_unity_tick_request_ns.store(now_ns);

    env->CallVoidMethod(g_activity_ref, g_activity_pump_unity_tick);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        g_unity_tick_pending.store(false);
    }
}

void RenderChatLines(JNIEnv* env) {
    if (env == nullptr || g_chat_label == nullptr) {
        return;
    }

    std::string combined = "Chat\n";
    for (const std::string& line : g_chat_lines) {
        combined += line;
        combined += "\n";
    }

    SetTextValue(env, g_chat_label, combined);
}

void AddChatLineLocal(JNIEnv* env, const std::string& line) {
    if (line.empty()) {
        return;
    }

    g_chat_lines.push_back(line);
    while (g_chat_lines.size() > 10) {
        g_chat_lines.pop_front();
    }

    RenderChatLines(env);
}

void ShowConnectPanel(JNIEnv* env, const std::string& status) {
    g_ui_phase.store(static_cast<int>(UiPhase::kConnectMenu));
    SetViewVisibility(env, g_loading_panel, kViewGone);
    SetViewVisibility(env, g_connect_panel, kViewVisible);
    SetViewVisibility(env, g_chat_label, kViewVisible);
    SetViewEnabled(env, g_connect_button, true);
    SetViewClickable(env, g_root_overlay, true);
    SetViewBackgroundColor(env, g_root_overlay, static_cast<jint>(0xFF000000));
    SetTextValue(env, g_status_label, status);
    g_connect_inflight.store(false);
}

void ApplyConnectedUiState(JNIEnv* env, int world_id, double money, double bitcoin) {
    g_ui_phase.store(static_cast<int>(UiPhase::kJoiningWorld));
    SetViewVisibility(env, g_loading_panel, kViewVisible);
    SetProgressValue(env, g_loading_progress, 100);
    SetTextValue(env, g_loading_label, "Joining world...");
    SetViewVisibility(env, g_connect_panel, kViewGone);
    SetViewVisibility(env, g_chat_label, kViewVisible);
    SetViewBackgroundColor(env, g_root_overlay, static_cast<jint>(0xFF000000));
    SetViewClickable(env, g_root_overlay, true);
    SetViewEnabled(env, g_connect_button, true);
    g_connect_inflight.store(false);

    char summary[192];
    std::snprintf(summary,
                  sizeof(summary),
                  "Connected. World %d | Money %.0f | BTC %.6f",
                  world_id,
                  money,
                  bitcoin);
    AddChatLineLocal(env, summary);

    const bool scene_load_ok = ForceLoadWorldScene(world_id);
    if (scene_load_ok) {
        g_pending_world_scene.store(world_id);
        g_world_join_started_ns.store(MonotonicNowNs());
        g_waiting_world_scene.store(true);
        AddChatLineLocal(env, "World scene load invoked. Waiting for scene...");
    } else {
        g_waiting_world_scene.store(false);
        AddChatLineLocal(env, "Scene load failed; keeping offline menu hidden.");
        SetTextValue(env, g_status_label, "Connected, but world load failed.");
        SetViewVisibility(env, g_connect_panel, kViewVisible);
        SetViewVisibility(env, g_loading_panel, kViewGone);
        g_ui_phase.store(static_cast<int>(UiPhase::kConnectMenu));
        SetViewClickable(env, g_root_overlay, true);
    }

    char payload[320];
    std::snprintf(payload,
                  sizeof(payload),
                  "{\"worldId\":%d,\"money\":%.2f,\"bitcoin\":%.6f,\"player\":\"%s\"}",
                  world_id,
                  money,
                  bitcoin,
                  JsonEscape(g_last_player_name).c_str());
    SendUnityBridgeMessage(env, "OnMpSpawnConfig", payload);
}

void DrainUiEvents(JNIEnv* env) {
    std::vector<UiEvent> events;
    {
        std::lock_guard<std::mutex> lock(g_event_mutex);
        if (g_ui_events.empty()) {
            return;
        }
        events.swap(g_ui_events);
    }

    for (const UiEvent& event : events) {
        switch (event.type) {
            case UiEvent::Type::kStatus:
                SetTextValue(env, g_status_label, event.text);
                break;
            case UiEvent::Type::kChat:
                AddChatLineLocal(env, event.text);
                break;
            case UiEvent::Type::kConnected:
                ApplyConnectedUiState(env, event.world_id, event.money, event.bitcoin);
                break;
            case UiEvent::Type::kDisconnected:
                ShowConnectPanel(env, event.text);
                break;
        }
    }
}

void UpdateBootLoading(JNIEnv* env, long long frame_time_ns) {
    if (static_cast<UiPhase>(g_ui_phase.load()) != UiPhase::kBootLoading) {
        return;
    }

    if (g_boot_started_ns == 0) {
        g_boot_started_ns = frame_time_ns;
        g_last_boot_progress = -1;
    }

    const long long elapsed = std::max<long long>(0, frame_time_ns - g_boot_started_ns);
    int progress = static_cast<int>((elapsed * 100LL) / kBootDurationNs);
    progress = std::clamp(progress, 0, 100);

    if (progress != g_last_boot_progress) {
        g_last_boot_progress = progress;
        SetProgressValue(env, g_loading_progress, progress);

        char label[96];
        std::snprintf(label, sizeof(label), "Loading online runtime... %d%%", progress);
        SetTextValue(env, g_loading_label, label);
    }

    if (progress >= 100) {
        ShowConnectPanel(env, "Name and IP را وارد کن و Connect بزن.");
        SendUnityBridgeMessage(env, "OnMpBootReady", "{\"phase\":\"connect_menu\"}");
    }
}

void UpdateWorldJoinTransition(JNIEnv* env, long long frame_time_ns) {
    (void)frame_time_ns;
    if (env == nullptr || !g_waiting_world_scene.load()) {
        return;
    }

    const int target_scene = g_pending_world_scene.load();
    const int active_scene = GetActiveSceneBuildIndex();
    const long long started_ns = g_world_join_started_ns.load();
    const long long elapsed_ms =
        (started_ns > 0) ? ((MonotonicNowNs() - started_ns) / 1000000LL) : 0LL;

    bool ready = false;
    if (active_scene >= 0 && target_scene >= 0 && active_scene == target_scene) {
        ready = true;
    }
    if (!ready && elapsed_ms >= 7000LL) {
        // Fallback gate: keep menu hidden for a short transition window even if scene index probing fails.
        ready = true;
    }

    if (!ready) {
        char join_label[128];
        if (active_scene >= 0) {
            std::snprintf(join_label, sizeof(join_label), "Joining world... active scene %d", active_scene);
        } else {
            std::snprintf(join_label, sizeof(join_label), "Joining world... (%lld ms)", elapsed_ms);
        }
        SetTextValue(env, g_loading_label, join_label);
        return;
    }

    g_waiting_world_scene.store(false);
    g_ui_phase.store(static_cast<int>(UiPhase::kConnected));
    SetViewVisibility(env, g_loading_panel, kViewGone);
    SetViewVisibility(env, g_chat_label, kViewVisible);
    SetViewBackgroundColor(env, g_root_overlay, static_cast<jint>(0x00000000));
    SetViewClickable(env, g_root_overlay, false);
    AddChatLineLocal(env, "World scene ready.");
    MOD_LOGI("UpdateWorldJoinTransition: world ready. active=%d target=%d elapsedMs=%lld",
             active_scene,
             target_scene,
             elapsed_ms);
}

void UiFrameCallback(long frame_time_ns, void* data) {
    (void)data;

    if (!g_ui_initialized.load()) {
        return;
    }

    JNIEnv* env = nullptr;
    bool attached = false;
    if (GetJniEnv(&env, &attached) && env != nullptr) {
        UpdateBootLoading(env, static_cast<long long>(frame_time_ns));
        DrainUiEvents(env);
        TryInstallPauseMenuActionHooks();
        UpdateWorldJoinTransition(env, static_cast<long long>(frame_time_ns));
        TryHidePauseMenuButtons();
        RequestUnityThreadAvatarTick(env);
        if (attached) {
            g_vm->DetachCurrentThread();
        }
    }

    if (g_choreographer != nullptr && g_choreo_post_frame != nullptr) {
        g_choreo_post_frame(g_choreographer, UiFrameCallback, nullptr);
    }
}

void StartUiFrameLoop() {
    bool expected = false;
    if (!g_frame_loop_started.compare_exchange_strong(expected, true)) {
        return;
    }

    if (g_libandroid_handle == nullptr) {
        g_libandroid_handle = dlopen("libandroid.so", RTLD_NOW);
    }
    if (g_libandroid_handle == nullptr) {
        MOD_LOGW("StartUiFrameLoop: libandroid.so unavailable.");
        return;
    }

    if (g_choreo_get_instance == nullptr) {
        g_choreo_get_instance = reinterpret_cast<ChoreoGetInstanceFn>(dlsym(g_libandroid_handle, "AChoreographer_getInstance"));
    }
    if (g_choreo_post_frame == nullptr) {
        g_choreo_post_frame = reinterpret_cast<ChoreoPostFrameFn>(dlsym(g_libandroid_handle, "AChoreographer_postFrameCallback"));
    }

    if (g_choreo_get_instance == nullptr || g_choreo_post_frame == nullptr) {
        MOD_LOGW("StartUiFrameLoop: choreographer symbols unavailable.");
        return;
    }

    g_choreographer = g_choreo_get_instance();
    if (g_choreographer == nullptr) {
        MOD_LOGW("StartUiFrameLoop: choreographer instance unavailable.");
        return;
    }

    g_choreo_post_frame(g_choreographer, UiFrameCallback, nullptr);
}

bool ParseEndpoint(const std::string& endpoint, std::string* host, int* port) {
    if (host == nullptr || port == nullptr) {
        return false;
    }

    std::string text = TrimCopy(endpoint);
    if (text.empty()) {
        return false;
    }

    std::string parsed_host = text;
    int parsed_port = kDefaultPort;

    const size_t first_colon = text.find(':');
    const size_t last_colon = text.rfind(':');

    if (first_colon != std::string::npos && first_colon == last_colon) {
        parsed_host = TrimCopy(text.substr(0, first_colon));
        const std::string port_text = TrimCopy(text.substr(first_colon + 1));
        if (!port_text.empty()) {
            const int value = std::atoi(port_text.c_str());
            if (value > 0 && value <= 65535) {
                parsed_port = value;
            }
        }
    }

    if (parsed_host.empty()) {
        return false;
    }

    *host = parsed_host;
    *port = parsed_port;
    return true;
}

bool ConnectWithTimeout(int fd, const sockaddr* addr, socklen_t addr_len, int timeout_ms) {
    const int original_flags = fcntl(fd, F_GETFL, 0);
    if (original_flags < 0) {
        return false;
    }

    if (fcntl(fd, F_SETFL, original_flags | O_NONBLOCK) != 0) {
        return false;
    }

    int rc = connect(fd, addr, addr_len);
    if (rc == 0) {
        fcntl(fd, F_SETFL, original_flags);
        return true;
    }

    if (errno != EINPROGRESS) {
        fcntl(fd, F_SETFL, original_flags);
        return false;
    }

    fd_set write_set;
    FD_ZERO(&write_set);
    FD_SET(fd, &write_set);

    timeval tv;
    tv.tv_sec = timeout_ms / 1000;
    tv.tv_usec = (timeout_ms % 1000) * 1000;

    rc = select(fd + 1, nullptr, &write_set, nullptr, &tv);
    if (rc <= 0) {
        fcntl(fd, F_SETFL, original_flags);
        return false;
    }

    int socket_error = 0;
    socklen_t error_len = sizeof(socket_error);
    if (getsockopt(fd, SOL_SOCKET, SO_ERROR, &socket_error, &error_len) != 0 || socket_error != 0) {
        fcntl(fd, F_SETFL, original_flags);
        return false;
    }

    fcntl(fd, F_SETFL, original_flags);
    return true;
}

bool SendLine(int fd, const std::string& line) {
    std::string payload = line;
    payload.push_back('\n');

    size_t sent = 0;
    while (sent < payload.size()) {
        const ssize_t written = send(fd, payload.data() + sent, payload.size() - sent, 0);
        if (written <= 0) {
            if (errno == EINTR) {
                continue;
            }
            return false;
        }
        sent += static_cast<size_t>(written);
    }

    return true;
}

void CloseNetworkSocket() {
    const int fd = g_network_socket.exchange(-1);
    if (fd >= 0) {
        shutdown(fd, SHUT_RDWR);
        close(fd);
    }
}

void HandleServerPacket(const std::string& line) {
    std::string type;
    if (!ExtractJsonString(line, "type", &type)) {
        return;
    }

    if (type == "server_hello") {
        PushChatEvent("Server hello received.");
        return;
    }

    if (type == "hello_ack") {
        std::string name;
        if (ExtractJsonString(line, "name", &name) && !name.empty()) {
            PushStatusEvent("Connected as " + name + ". Waiting for world...");
        } else {
            PushStatusEvent("Hello ACK received. Waiting for world...");
        }
        return;
    }

    if (type == "session_config") {
        int world_id = 1;
        double money = 0.0;
        double bitcoin = 0.0;
        ExtractJsonInt(line, "worldId", &world_id);
        ExtractJsonDouble(line, "money", &money);
        ExtractJsonDouble(line, "bitcoin", &bitcoin);

        char status[160];
        std::snprintf(status,
                      sizeof(status),
                      "Session config: world %d | money %.0f | btc %.6f",
                      world_id,
                      money,
                      bitcoin);
        PushStatusEvent(status);
        return;
    }

    if (type == "world_entered") {
        int world_id = 1;
        double money = 0.0;
        double bitcoin = 0.0;
        ExtractJsonInt(line, "worldId", &world_id);
        ExtractJsonDouble(line, "money", &money);
        ExtractJsonDouble(line, "bitcoin", &bitcoin);
        StartProfileSyncThread();
        PushConnectedEvent(world_id, money, bitcoin);
        return;
    }

    if (type == "world_state") {
        HandleWorldStatePacket(line);
        return;
    }

    if (type == "player_joined") {
        std::string name;
        if (!ExtractJsonString(line, "name", &name)) {
            ExtractJsonString(line, "playerId", &name);
        }
        if (!name.empty()) {
            PushChatEvent(name + " joined.");
        }
        return;
    }

    if (type == "player_left") {
        std::string player_id;
        if (ExtractJsonString(line, "playerId", &player_id) && !player_id.empty()) {
            PushChatEvent(player_id + " left.");
        }
        return;
    }

    if (type == "error") {
        std::string message;
        ExtractJsonString(line, "message", &message);
        if (message.empty()) {
            message = "server_error";
        }
        PushStatusEvent("Server error: " + message);
        return;
    }
}

void StopProfileSyncThread() {
    g_profile_sync_stop.store(true);
    if (g_profile_sync_thread.joinable()) {
        g_profile_sync_thread.join();
    }
    g_profile_sync_stop.store(false);
}

void SetProfileLastSignature(const std::string& signature) {
    std::lock_guard<std::mutex> lock(g_profile_sig_mutex);
    g_profile_last_signature = signature;
}

std::string GetProfileLastSignature() {
    std::lock_guard<std::mutex> lock(g_profile_sig_mutex);
    return g_profile_last_signature;
}

bool SendProfileSnapshotNow(int fd, std::string* inout_last_signature) {
    if (fd < 0) {
        return false;
    }

    const ProfileSnapshot snapshot = BuildProfileSnapshotFromLocalSave();
    if (!snapshot.valid) {
        return false;
    }

    const std::string signature = BuildProfileSignature(snapshot);
    if (inout_last_signature != nullptr && *inout_last_signature == signature) {
        return false;
    }

    const std::string payload = BuildSetProfilePayload(snapshot);
    if (!SendLine(fd, payload)) {
        return false;
    }

    if (inout_last_signature != nullptr) {
        *inout_last_signature = signature;
    }
    SetProfileLastSignature(signature);
    return true;
}

void StartProfileSyncThread() {
    StopProfileSyncThread();

    g_profile_sync_thread = std::thread([]() {
        std::string last_signature;
        int loop_counter = 0;

        // Baseline local profile at connect time to avoid pushing stale data immediately.
        const ProfileSnapshot initial_snapshot = BuildProfileSnapshotFromLocalSave();
        if (initial_snapshot.valid) {
            last_signature = BuildProfileSignature(initial_snapshot);
            SetProfileLastSignature(last_signature);
            MOD_LOGI("ProfileSync: baseline loaded from %s", initial_snapshot.save_path.c_str());
        } else {
            SetProfileLastSignature("");
            MOD_LOGW("ProfileSync: baseline snapshot unavailable.");
        }

        while (!g_profile_sync_stop.load()) {
            for (int i = 0; i < 12 && !g_profile_sync_stop.load(); ++i) {
                std::this_thread::sleep_for(std::chrono::milliseconds(100));
            }
            if (g_profile_sync_stop.load()) {
                break;
            }

            const int fd = g_network_socket.load();
            if (fd < 0) {
                continue;
            }

            if (!SendProfileSnapshotNow(fd, &last_signature)) {
                continue;
            }

            ++loop_counter;
            if (loop_counter <= 3 || (loop_counter % 8) == 0) {
                const std::string save_path = FindLatestPcSaveFile(g_saves_dir);
                MOD_LOGI("ProfileSync: pushed set_profile from %s", save_path.c_str());
            }
        }
    });
}

void StopNetworkClient() {
    {
        std::lock_guard<std::mutex> lock(g_remote_players_mutex);
        g_remote_players.clear();
    }

    {
        std::lock_guard<std::mutex> lock(g_network_mutex);
        g_network_stop.store(true);
        const int fd = g_network_socket.load();
        std::string last_sig_local = GetProfileLastSignature();
        SendProfileSnapshotNow(fd, &last_sig_local);
        StopProfileSyncThread();
        CloseNetworkSocket();
    }

    if (g_network_thread.joinable()) {
        g_network_thread.join();
    }

    g_network_stop.store(false);
}

void StartNetworkClient(const std::string& player_id,
                        const std::string& player_name,
                        const std::string& endpoint) {
    StopNetworkClient();

    g_network_thread = std::thread([player_id, player_name, endpoint]() {
        MOD_LOGI("StartNetworkClient: thread begin endpoint=%s playerId=%s",
                 endpoint.c_str(),
                 player_id.c_str());
        std::string host;
        int port = kDefaultPort;
        if (!ParseEndpoint(endpoint, &host, &port)) {
            PushDisconnectedEvent("IP معتبر نیست.");
            MOD_LOGW("StartNetworkClient: invalid endpoint.");
            return;
        }

        char pre_status[160];
        std::snprintf(pre_status, sizeof(pre_status), "Connecting to %s:%d ...", host.c_str(), port);
        PushStatusEvent(pre_status);

        addrinfo hints;
        std::memset(&hints, 0, sizeof(hints));
        hints.ai_family = AF_UNSPEC;
        hints.ai_socktype = SOCK_STREAM;

        addrinfo* result = nullptr;
        const std::string port_text = std::to_string(port);
        const int gai = getaddrinfo(host.c_str(), port_text.c_str(), &hints, &result);
        if (gai != 0 || result == nullptr) {
            PushDisconnectedEvent("Resolve host failed.");
            MOD_LOGW("StartNetworkClient: getaddrinfo failed for %s", host.c_str());
            return;
        }

        int sock = -1;
        for (addrinfo* it = result; it != nullptr && !g_network_stop.load(); it = it->ai_next) {
            sock = socket(it->ai_family, it->ai_socktype, it->ai_protocol);
            if (sock < 0) {
                continue;
            }

            if (ConnectWithTimeout(sock, it->ai_addr, static_cast<socklen_t>(it->ai_addrlen), 4500)) {
                break;
            }

            close(sock);
            sock = -1;
        }
        freeaddrinfo(result);

        if (sock < 0 || g_network_stop.load()) {
            if (sock >= 0) {
                close(sock);
            }
            PushDisconnectedEvent("Connect timeout / failed.");
            MOD_LOGW("StartNetworkClient: connect failed.");
            return;
        }

        g_network_socket.store(sock);

        const std::string safe_name = TrimCopy(player_name).empty() ? "Player" : TrimCopy(player_name);
        const std::string safe_id = TrimCopy(player_id).empty() ? safe_name : TrimCopy(player_id);
        const std::string hello =
            "{\"type\":\"hello\",\"playerId\":\"" + JsonEscape(safe_id) +
            "\",\"name\":\"" + JsonEscape(safe_name) +
            "\",\"platform\":\"android-mod\"}";

        if (!SendLine(sock, hello)) {
            PushDisconnectedEvent("Failed to send hello.");
            CloseNetworkSocket();
            MOD_LOGW("StartNetworkClient: hello send failed.");
            return;
        }

        PushStatusEvent("TCP connected. Waiting for server packets...");
        PushChatEvent("Connected to server.");

        std::string pending;
        char buffer[2048];

        while (!g_network_stop.load()) {
            const ssize_t received = recv(sock, buffer, sizeof(buffer), 0);
            if (received == 0) {
                break;
            }

            if (received < 0) {
                if (errno == EINTR) {
                    continue;
                }
                break;
            }

            pending.append(buffer, static_cast<size_t>(received));

            size_t pos = 0;
            while (true) {
                const size_t newline = pending.find('\n', pos);
                if (newline == std::string::npos) {
                    pending.erase(0, pos);
                    break;
                }

                std::string line = pending.substr(pos, newline - pos);
                pos = newline + 1;
                line = TrimCopy(line);
                if (!line.empty()) {
                    HandleServerPacket(line);
                }
            }
        }

        CloseNetworkSocket();
        StopProfileSyncThread();

        if (!g_network_stop.load()) {
            PushDisconnectedEvent("Connection lost.");
            PushChatEvent("Disconnected from server.");
            MOD_LOGW("StartNetworkClient: connection lost.");
        }
    });
}

void InitializeModule() {
    bool expected = false;
    if (!g_module_initialized.compare_exchange_strong(expected, true)) {
        return;
    }

    MOD_LOGI("Initializing native module...");
    ConfigureNativeCrashLogPath("");
    InstallNativeCrashSignalHandlers();
    const bool patch_ok = ApplyPatches();
    const bool hook_ok = InstallHooks();
    const bool mp_hook_ok = InitializeMpHookPipeline();
    MOD_LOGI("Native module initialized. patches=%d hooks=%d mp_hooks=%d",
             patch_ok ? 1 : 0,
             hook_ok ? 1 : 0,
             mp_hook_ok ? 1 : 0);
}

void CacheUnitySendMessage(JNIEnv* env) {
    if (env == nullptr || g_unity_send_message != nullptr) {
        return;
    }

    jclass unity_cls_local = env->FindClass("com/unity3d/player/UnityPlayer");
    if (unity_cls_local == nullptr) {
        ClearException(env);
        MOD_LOGW("UnityPlayer class not found for UnitySendMessage.");
        return;
    }

    g_unity_player_class = reinterpret_cast<jclass>(env->NewGlobalRef(unity_cls_local));
    g_unity_send_message = env->GetStaticMethodID(
        unity_cls_local,
        "UnitySendMessage",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (g_unity_send_message == nullptr) {
        ClearException(env);
        MOD_LOGW("UnitySendMessage method not found.");
    }

    env->DeleteLocalRef(unity_cls_local);
}

bool CreateNativeOnlineUi(JNIEnv* env, jobject activity) {
    MOD_LOGI("CreateNativeOnlineUi: begin");
    if (env == nullptr || activity == nullptr) {
        MOD_LOGW("CreateNativeOnlineUi: env/activity null.");
        return false;
    }

    bool expected = false;
    if (!g_ui_initialized.compare_exchange_strong(expected, true)) {
        MOD_LOGI("CreateNativeOnlineUi: already initialized.");
        return true;
    }

    MOD_LOGI("CreateNativeOnlineUi: capture refs");
    g_activity_ref = env->NewGlobalRef(activity);
    CacheUnitySendMessage(env);
    jclass activity_cls_tick = env->GetObjectClass(activity);
    if (activity_cls_tick != nullptr) {
        g_activity_pump_unity_tick =
            env->GetMethodID(activity_cls_tick, "pumpNativeOnUnityThread", "()V");
        if (g_activity_pump_unity_tick == nullptr) {
            ClearException(env);
            MOD_LOGW("CreateNativeOnlineUi: pumpNativeOnUnityThread() not found.");
        }
        env->DeleteLocalRef(activity_cls_tick);
    } else {
        ClearException(env);
    }

    g_app_data_dir = QueryAppDataDir(env, activity);
    g_external_files_dir = QueryExternalFilesDir(env, activity);
    g_saves_dir = g_external_files_dir.empty() ? "" : (g_external_files_dir + "/saves");
    ConfigureNativeCrashLogPath(g_external_files_dir);
    g_playerprefs_path = g_app_data_dir.empty()
                             ? ""
                             : (g_app_data_dir + "/shared_prefs/com.Yiming.PC.v2.playerprefs.xml");
    const std::string android_id = QueryAndroidSecureId(env, activity);
    const std::string android_model = QueryAndroidModel(env);
    g_client_player_id = SanitizePlayerId(
        android_id.empty() ? android_model : ("android_" + android_id),
        "player_default");
    MOD_LOGI("CreateNativeOnlineUi: dataDir=%s extDir=%s savesDir=%s",
             g_app_data_dir.c_str(),
             g_external_files_dir.c_str(),
             g_saves_dir.c_str());
    MOD_LOGI("CreateNativeOnlineUi: stablePlayerId=%s", g_client_player_id.c_str());

    const int pad = DpToPx(env, activity, 16);
    const int panel_margin = DpToPx(env, activity, 20);

    jclass frame_cls = env->FindClass("android/widget/FrameLayout");
    if (frame_cls == nullptr) {
        MOD_LOGW("CreateNativeOnlineUi: FrameLayout class not found.");
        ClearException(env);
        g_ui_initialized.store(false);
        return false;
    }
    jmethodID frame_ctor = env->GetMethodID(frame_cls, "<init>", "(Landroid/content/Context;)V");
    if (frame_ctor == nullptr) {
        MOD_LOGW("CreateNativeOnlineUi: FrameLayout ctor not found.");
        ClearException(env);
        env->DeleteLocalRef(frame_cls);
        g_ui_initialized.store(false);
        return false;
    }
    jobject root = env->NewObject(frame_cls, frame_ctor, activity);
    if (root == nullptr) {
        MOD_LOGW("CreateNativeOnlineUi: root overlay alloc failed.");
        ClearException(env);
        env->DeleteLocalRef(frame_cls);
        g_ui_initialized.store(false);
        return false;
    }
    SetViewBackgroundColor(env, root, static_cast<jint>(0xFF000000));
    SetViewClickable(env, root, true);

    jclass linear_cls = env->FindClass("android/widget/LinearLayout");
    jmethodID linear_ctor = env->GetMethodID(linear_cls, "<init>", "(Landroid/content/Context;)V");
    jmethodID set_orientation = env->GetMethodID(linear_cls, "setOrientation", "(I)V");
    jmethodID set_padding = env->GetMethodID(linear_cls, "setPadding", "(IIII)V");

    jobject loading_panel = env->NewObject(linear_cls, linear_ctor, activity);
    env->CallVoidMethod(loading_panel, set_orientation, static_cast<jint>(kLinearVertical));
    env->CallVoidMethod(loading_panel, set_padding, pad, pad, pad, pad);
    SetViewBackgroundColor(env, loading_panel, static_cast<jint>(0xDD1B1B1B));

    jobject loading_title = CreateTextView(env, activity, "Initializing MP runtime", 18.0f, static_cast<jint>(0xFFFFFFFF), kGravityCenter);
    jobject progress_bar = nullptr;
    {
        jclass progress_cls = env->FindClass("android/widget/ProgressBar");
        jmethodID progress_ctor = env->GetMethodID(progress_cls, "<init>", "(Landroid/content/Context;Landroid/util/AttributeSet;I)V");
        if (progress_ctor != nullptr) {
            progress_bar = env->NewObject(progress_cls, progress_ctor, activity, nullptr, static_cast<jint>(kAndroidProgressHorizontalStyle));
        }
        if (progress_bar == nullptr) {
            ClearException(env);
            jmethodID fallback_ctor = env->GetMethodID(progress_cls, "<init>", "(Landroid/content/Context;)V");
            if (fallback_ctor != nullptr) {
                progress_bar = env->NewObject(progress_cls, fallback_ctor, activity);
            }
        }

        if (progress_bar != nullptr) {
            jmethodID set_max = env->GetMethodID(progress_cls, "setMax", "(I)V");
            if (set_max != nullptr) {
                env->CallVoidMethod(progress_bar, set_max, static_cast<jint>(100));
            }
            SetProgressValue(env, progress_bar, 0);
        }
        env->DeleteLocalRef(progress_cls);
    }

    jobject loading_label = CreateTextView(env, activity, "Loading online runtime... 0%", 14.0f, static_cast<jint>(0xFFD0D0D0), kGravityCenter);

    jobject lp_title = NewLinearLayoutParams(env, kMatchParent, kWrapContent, 0, 0, 0, DpToPx(env, activity, 12));
    AddChildView(env, loading_panel, loading_title, lp_title);

    jobject lp_bar = NewLinearLayoutParams(env, kMatchParent, DpToPx(env, activity, 12), 0, 0, 0, DpToPx(env, activity, 10));
    AddChildView(env, loading_panel, progress_bar, lp_bar);

    jobject lp_progress_label = NewLinearLayoutParams(env, kMatchParent, kWrapContent, 0, 0, 0, 0);
    AddChildView(env, loading_panel, loading_label, lp_progress_label);

    jobject lp_loading = NewFrameLayoutParams(env,
                                              std::max(DpToPx(env, activity, 300), DpToPx(env, activity, 320)),
                                              kWrapContent,
                                              kGravityCenter,
                                              panel_margin,
                                              panel_margin,
                                              panel_margin,
                                              panel_margin);
    AddChildView(env, root, loading_panel, lp_loading);

    jobject connect_panel = env->NewObject(linear_cls, linear_ctor, activity);
    env->CallVoidMethod(connect_panel, set_orientation, static_cast<jint>(kLinearVertical));
    env->CallVoidMethod(connect_panel, set_padding, pad, pad, pad, pad);
    SetViewBackgroundColor(env, connect_panel, static_cast<jint>(0xFF141414));
    SetViewVisibility(env, connect_panel, kViewGone);

    jobject connect_title = CreateTextView(env, activity, "PCMP v0.1", 18.0f, static_cast<jint>(0xFFFFFFFF), kGravityCenter);
    jobject name_input = CreateEditText(env, activity, "Player Name", "Player");
    jobject ip_input = CreateEditText(env, activity, "IP:Port", g_last_endpoint);
    jobject connect_button = CreateButton(env, activity, "Connect", kConnectButtonId);
    jobject status_label = CreateTextView(env, activity, "Waiting...", 13.0f, static_cast<jint>(0xFFD0D0D0), kGravityCenter);

    jclass view_cls = env->FindClass("android/view/View");
    jmethodID set_on_click_listener = env->GetMethodID(view_cls, "setOnClickListener", "(Landroid/view/View$OnClickListener;)V");
    if (set_on_click_listener != nullptr && connect_button != nullptr) {
        env->CallVoidMethod(connect_button, set_on_click_listener, activity);
    } else {
        ClearException(env);
    }

    jobject lp_connect_title = NewLinearLayoutParams(env, kMatchParent, kWrapContent, 0, 0, 0, DpToPx(env, activity, 10));
    AddChildView(env, connect_panel, connect_title, lp_connect_title);

    jobject lp_name = NewLinearLayoutParams(env, kMatchParent, kWrapContent, 0, 0, 0, DpToPx(env, activity, 8));
    AddChildView(env, connect_panel, name_input, lp_name);

    jobject lp_ip = NewLinearLayoutParams(env, kMatchParent, kWrapContent, 0, 0, 0, DpToPx(env, activity, 10));
    AddChildView(env, connect_panel, ip_input, lp_ip);

    jobject lp_connect_button = NewLinearLayoutParams(env, kMatchParent, kWrapContent, 0, 0, 0, DpToPx(env, activity, 8));
    AddChildView(env, connect_panel, connect_button, lp_connect_button);

    jobject lp_status = NewLinearLayoutParams(env, kMatchParent, kWrapContent, 0, 0, 0, 0);
    AddChildView(env, connect_panel, status_label, lp_status);

    jobject lp_connect_panel = NewFrameLayoutParams(env,
                                                    std::max(DpToPx(env, activity, 300), DpToPx(env, activity, 320)),
                                                    kWrapContent,
                                                    kGravityCenter,
                                                    panel_margin,
                                                    panel_margin,
                                                    panel_margin,
                                                    panel_margin);
    AddChildView(env, root, connect_panel, lp_connect_panel);

    jobject chat_label = CreateTextView(env,
                                        activity,
                                        "Chat\n",
                                        13.0f,
                                        static_cast<jint>(0xFFFFFFFF),
                                        kGravityTopStart);
    SetViewBackgroundColor(env, chat_label, static_cast<jint>(0x86232323));

    jclass text_cls = env->FindClass("android/widget/TextView");
    jmethodID set_max_lines = env->GetMethodID(text_cls, "setMaxLines", "(I)V");
    if (set_max_lines != nullptr) {
        env->CallVoidMethod(chat_label, set_max_lines, static_cast<jint>(12));
    } else {
        ClearException(env);
    }

    jmethodID set_text_padding = env->GetMethodID(text_cls, "setPadding", "(IIII)V");
    if (set_text_padding != nullptr) {
        const int chat_pad = DpToPx(env, activity, 8);
        env->CallVoidMethod(chat_label, set_text_padding, chat_pad, chat_pad, chat_pad, chat_pad);
    }

    jobject lp_chat = NewFrameLayoutParams(env,
                                           DpToPx(env, activity, 250),
                                           kWrapContent,
                                           kGravityTopStart,
                                           DpToPx(env, activity, 10),
                                           DpToPx(env, activity, 10),
                                           0,
                                           0);
    AddChildView(env, root, chat_label, lp_chat);

    jobject root_params = NewFrameLayoutParams(env, kMatchParent, kMatchParent, kGravityTopStart, 0, 0, 0, 0);
    jclass activity_cls = env->GetObjectClass(activity);
    jmethodID add_content_view = env->GetMethodID(activity_cls, "addContentView", "(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V");
    if (add_content_view != nullptr) {
        env->CallVoidMethod(activity, add_content_view, root, root_params);
    } else {
        ClearException(env);
    }

    g_root_overlay = env->NewGlobalRef(root);
    g_loading_panel = env->NewGlobalRef(loading_panel);
    g_loading_progress = env->NewGlobalRef(progress_bar);
    g_loading_label = env->NewGlobalRef(loading_label);
    g_connect_panel = env->NewGlobalRef(connect_panel);
    g_name_input = env->NewGlobalRef(name_input);
    g_ip_input = env->NewGlobalRef(ip_input);
    g_connect_button = env->NewGlobalRef(connect_button);
    g_status_label = env->NewGlobalRef(status_label);
    g_chat_label = env->NewGlobalRef(chat_label);

    g_boot_started_ns = 0;
    g_last_boot_progress = -1;
    g_ui_phase.store(static_cast<int>(UiPhase::kBootLoading));

    env->DeleteLocalRef(frame_cls);
    env->DeleteLocalRef(linear_cls);
    env->DeleteLocalRef(view_cls);
    env->DeleteLocalRef(text_cls);
    env->DeleteLocalRef(activity_cls);

    env->DeleteLocalRef(root);
    env->DeleteLocalRef(loading_panel);
    env->DeleteLocalRef(loading_title);
    env->DeleteLocalRef(progress_bar);
    env->DeleteLocalRef(loading_label);
    env->DeleteLocalRef(lp_title);
    env->DeleteLocalRef(lp_bar);
    env->DeleteLocalRef(lp_progress_label);
    env->DeleteLocalRef(lp_loading);

    env->DeleteLocalRef(connect_panel);
    env->DeleteLocalRef(connect_title);
    env->DeleteLocalRef(name_input);
    env->DeleteLocalRef(ip_input);
    env->DeleteLocalRef(connect_button);
    env->DeleteLocalRef(status_label);
    env->DeleteLocalRef(lp_connect_title);
    env->DeleteLocalRef(lp_name);
    env->DeleteLocalRef(lp_ip);
    env->DeleteLocalRef(lp_connect_button);
    env->DeleteLocalRef(lp_status);
    env->DeleteLocalRef(lp_connect_panel);

    env->DeleteLocalRef(chat_label);
    env->DeleteLocalRef(lp_chat);
    env->DeleteLocalRef(root_params);

    AddChatLineLocal(env, "MP runtime loaded.");
    StartUiFrameLoop();

    MOD_LOGI("CreateNativeOnlineUi: Native MP UI created.");
    return true;
}

void HandleConnectButtonPressed(JNIEnv* env) {
    if (env == nullptr) {
        return;
    }

    if (!g_ui_initialized.load()) {
        return;
    }

    const std::string raw_name = TrimCopy(ReadTextValue(env, g_name_input));
    const std::string raw_endpoint = TrimCopy(ReadTextValue(env, g_ip_input));
    MOD_LOGI("HandleConnectButtonPressed: raw_name='%s' raw_endpoint='%s'",
             raw_name.c_str(),
             raw_endpoint.c_str());

    std::string player_name = raw_name;
    if (player_name.empty()) {
        player_name = "Player";
        SetTextValue(env, g_name_input, player_name);
    }

    std::string endpoint = raw_endpoint;
    if (endpoint.empty()) {
        endpoint = g_last_endpoint;
        SetTextValue(env, g_ip_input, endpoint);
    }

    if (g_connect_inflight.exchange(true)) {
        SetTextValue(env, g_status_label, "Connection in progress...");
        return;
    }

    g_last_player_name = player_name;
    g_last_endpoint = endpoint;
    const std::string player_id = g_client_player_id.empty()
                                      ? SanitizePlayerId(player_name, "player_default")
                                      : g_client_player_id;
    MOD_LOGI("HandleConnectButtonPressed: playerId=%s name=%s endpoint=%s",
             player_id.c_str(),
             player_name.c_str(),
             endpoint.c_str());

    g_ui_phase.store(static_cast<int>(UiPhase::kConnecting));
    SetTextValue(env, g_status_label, "Connecting...");
    SetViewEnabled(env, g_connect_button, false);
    SetViewClickable(env, g_root_overlay, true);

    AddChatLineLocal(env, "Trying " + endpoint + " ...");

    SendUnityBridgeMessage(env,
                           "OnMpConnectAttempt",
                           "{\"player\":\"" + JsonEscape(player_name) +
                               "\",\"endpoint\":\"" + JsonEscape(endpoint) + "\"}");

    StartNetworkClient(player_id, player_name, endpoint);
}
}  // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_unity3d_player_UnityPlayerActivity_nativeInitMod(JNIEnv* env, jclass clazz) {
    (void)env;
    (void)clazz;
    InitializeModule();
}

extern "C" JNIEXPORT void JNICALL
Java_com_unity3d_player_UnityPlayerActivity_nativeOnUnityReady(JNIEnv* env, jobject thiz) {
    InitializeModule();
    ApplyPatches();             // Retry after Unity player creation, when libil2cpp is typically loaded.
    InitializeMpHookPipeline(); // Retry MP hook bootstrap once Unity native runtime is active.
    MOD_LOGI("nativeOnUnityReady: entering CreateNativeOnlineUi");
    const bool ui_ok = CreateNativeOnlineUi(env, thiz);
    MOD_LOGI("nativeOnUnityReady: CreateNativeOnlineUi=%d", ui_ok ? 1 : 0);
    TryInstallPauseMenuActionHooks();
}

extern "C" JNIEXPORT void JNICALL
Java_com_unity3d_player_UnityPlayerActivity_nativeOnUiButtonClick(JNIEnv* env, jobject thiz, jint view_id) {
    (void)thiz;
    MOD_LOGI("nativeOnUiButtonClick: id=0x%x", static_cast<unsigned int>(view_id));
    if (view_id == kConnectButtonId) {
        HandleConnectButtonPressed(env);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_unity3d_player_UnityPlayerActivity_nativeOnUnityTick(JNIEnv* env, jobject thiz) {
    (void)env;
    (void)thiz;
    g_unity_tick_pending.store(false);
    UpdateRemoteAvatarScene();
}

extern "C" JNIEXPORT void JNICALL
Java_com_unity3d_player_UnityPlayerActivity_nativeStartPerfOverlay(JNIEnv* env, jobject thiz) {
    // Kept for compatibility with previous JNI signature.
    Java_com_unity3d_player_UnityPlayerActivity_nativeOnUnityReady(env, thiz);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    (void)reserved;
    g_vm = vm;
    InitializeModule();
    return JNI_VERSION_1_6;
}
