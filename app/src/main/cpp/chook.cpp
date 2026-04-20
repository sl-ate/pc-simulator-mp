#include "chook.h"

#include <atomic>

#if __has_include(<dobby.h>)
#include <dobby.h>
#define CHOOK_HAS_DOBBY 1
#else
#define CHOOK_HAS_DOBBY 0
#endif

#include "common.h"
#include "memory_utils.h"

namespace {
std::atomic<const char*> g_lib_name{"libil2cpp.so"};

void LogPatchResult(const char* op, const char* lib_name, uintptr_t offset, bool ok) {
    if (ok) {
        MOD_LOGI("%s ok: %s + 0x%lx", op, lib_name, static_cast<unsigned long>(offset));
    } else {
        MOD_LOGW("%s failed: %s + 0x%lx", op, lib_name, static_cast<unsigned long>(offset));
    }
}
}  // namespace

void CHook::SetLibrary(const char* lib_name) {
    if (lib_name != nullptr && lib_name[0] != '\0') {
        g_lib_name.store(lib_name);
    }
}

const char* CHook::GetLibrary() {
    return g_lib_name.load();
}

uintptr_t CHook::Base() {
    return FindLibraryBase(GetLibrary());
}

void* CHook::Addr(uintptr_t offset) {
    return Addr(GetLibrary(), offset);
}

void* CHook::Addr(const char* lib_name, uintptr_t offset) {
    return ResolveAddress(lib_name, offset);
}

bool CHook::Patch(uintptr_t offset, const void* data, size_t size) {
    return Patch(GetLibrary(), offset, data, size);
}

bool CHook::Patch(const char* lib_name, uintptr_t offset, const void* data, size_t size) {
    void* target = Addr(lib_name, offset);
    if (target == nullptr) {
        MOD_LOGW("Patch target not found: %s + 0x%lx", lib_name, static_cast<unsigned long>(offset));
        return false;
    }
    const bool ok = WriteMemory(target, data, size);
    LogPatchResult("Patch", lib_name, offset, ok);
    return ok;
}

bool CHook::PatchAbsolute(uintptr_t absolute_address, const void* data, size_t size) {
    if (absolute_address == 0) {
        return false;
    }
    const bool ok = WriteMemory(reinterpret_cast<void*>(absolute_address), data, size);
    if (ok) {
        MOD_LOGI("PatchAbsolute ok: 0x%lx", static_cast<unsigned long>(absolute_address));
    } else {
        MOD_LOGW("PatchAbsolute failed: 0x%lx", static_cast<unsigned long>(absolute_address));
    }
    return ok;
}

bool CHook::RET(uintptr_t offset) {
    return RET(GetLibrary(), offset);
}

bool CHook::RET(const char* lib_name, uintptr_t offset) {
#if defined(__aarch64__)
    const uint8_t code[] = {0xC0, 0x03, 0x5F, 0xD6};  // ret
#elif defined(__arm__)
    const uint8_t code[] = {0x70, 0x47};  // bx lr (thumb)
#else
    return false;
#endif
    return Patch(lib_name, offset, code, sizeof(code));
}

bool CHook::RET_TRUE(uintptr_t offset) {
    return RET_TRUE(GetLibrary(), offset);
}

bool CHook::RET_TRUE(const char* lib_name, uintptr_t offset) {
#if defined(__aarch64__)
    const uint8_t code[] = {
        0x20, 0x00, 0x80, 0x52,  // mov w0, #1
        0xC0, 0x03, 0x5F, 0xD6   // ret
    };
#elif defined(__arm__)
    const uint8_t code[] = {
        0x01, 0x20,  // movs r0, #1
        0x70, 0x47   // bx lr
    };
#else
    return false;
#endif
    return Patch(lib_name, offset, code, sizeof(code));
}

bool CHook::RET_FALSE(uintptr_t offset) {
    return RET_FALSE(GetLibrary(), offset);
}

bool CHook::RET_FALSE(const char* lib_name, uintptr_t offset) {
#if defined(__aarch64__)
    const uint8_t code[] = {
        0x00, 0x00, 0x80, 0x52,  // mov w0, #0
        0xC0, 0x03, 0x5F, 0xD6   // ret
    };
#elif defined(__arm__)
    const uint8_t code[] = {
        0x00, 0x20,  // movs r0, #0
        0x70, 0x47   // bx lr
    };
#else
    return false;
#endif
    return Patch(lib_name, offset, code, sizeof(code));
}

bool CHook::NOP(uintptr_t offset, size_t instruction_count) {
    return NOP(GetLibrary(), offset, instruction_count);
}

bool CHook::NOP(const char* lib_name, uintptr_t offset, size_t instruction_count) {
    if (instruction_count == 0) {
        return false;
    }
#if defined(__aarch64__)
    const uint8_t nop[] = {0x1F, 0x20, 0x03, 0xD5};
    for (size_t i = 0; i < instruction_count; ++i) {
        if (!Patch(lib_name, offset + (i * sizeof(nop)), nop, sizeof(nop))) {
            return false;
        }
    }
    return true;
#elif defined(__arm__)
    const uint8_t nop[] = {0x00, 0xBF};  // thumb nop
    for (size_t i = 0; i < instruction_count; ++i) {
        if (!Patch(lib_name, offset + (i * sizeof(nop)), nop, sizeof(nop))) {
            return false;
        }
    }
    return true;
#else
    return false;
#endif
}

bool CHook::Hook(uintptr_t offset, void* replacement, void** original) {
    return Hook(GetLibrary(), offset, replacement, original);
}

bool CHook::Hook(const char* lib_name, uintptr_t offset, void* replacement, void** original) {
    void* target = Addr(lib_name, offset);
    if (target == nullptr) {
        MOD_LOGW("Hook target not found: %s + 0x%lx", lib_name, static_cast<unsigned long>(offset));
        return false;
    }
#if CHOOK_HAS_DOBBY
    const int rc = DobbyHook(target, replacement, original);
    if (rc != RS_SUCCESS) {
        MOD_LOGE("DobbyHook failed rc=%d target=%p", rc, target);
        return false;
    }
    MOD_LOGI("Hook ok: %s + 0x%lx -> %p", lib_name, static_cast<unsigned long>(offset), replacement);
    return true;
#else
    (void)replacement;
    (void)original;
    MOD_LOGW("Hook skipped (Dobby missing): %s + 0x%lx", lib_name, static_cast<unsigned long>(offset));
    return false;
#endif
}

bool CHook::HookAbsolute(uintptr_t absolute_address, void* replacement, void** original) {
    if (absolute_address == 0) {
        return false;
    }
#if CHOOK_HAS_DOBBY
    void* target = reinterpret_cast<void*>(absolute_address);
    const int rc = DobbyHook(target, replacement, original);
    if (rc != RS_SUCCESS) {
        MOD_LOGE("DobbyHook absolute failed rc=%d target=%p", rc, target);
        return false;
    }
    MOD_LOGI("HookAbsolute ok: 0x%lx -> %p", static_cast<unsigned long>(absolute_address), replacement);
    return true;
#else
    (void)replacement;
    (void)original;
    MOD_LOGW("HookAbsolute skipped (Dobby missing): 0x%lx", static_cast<unsigned long>(absolute_address));
    return false;
#endif
}
