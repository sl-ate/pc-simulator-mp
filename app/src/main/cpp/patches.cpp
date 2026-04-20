#include "patches.h"

#include <atomic>

#include "chook.h"
#include "common.h"

namespace {
std::atomic<bool> g_patches_done{false};
constexpr bool kEnableStaticBinaryPatches = false;
constexpr bool kEnablePauseMenuBinaryPatches = true;

// RVAs from current dump of this game build (TypeDefIndex 4703):
// PauseMenu.ExitWithoutSave, PauseMenu.Home
constexpr uintptr_t kRvaPauseExitWithoutSave = 0x856CC4;
constexpr uintptr_t kRvaPauseHome = 0x856D4C;
}  // namespace

bool ApplyPatches() {
    if (g_patches_done.load()) {
        return true;
    }

    CHook::SetLibrary("libil2cpp.so");
    if (CHook::Base() == 0) {
        return false;
    }

    bool ok = true;

    if (kEnableStaticBinaryPatches) {
        ok &= CHook::RET_TRUE(0x83C1FC);
        ok &= CHook::RET(0x83C204);
    } else {
        MOD_LOGI("ApplyPatches: legacy static patches disabled for stability.");
    }

    if (kEnablePauseMenuBinaryPatches) {
        ok &= CHook::RET(kRvaPauseExitWithoutSave);
        ok &= CHook::RET(kRvaPauseHome);
        MOD_LOGI("ApplyPatches: pause menu patches requested.");
    }

    if (ok) {
        g_patches_done.store(true);
        MOD_LOGI("ApplyPatches: all patches applied via CHook.");
    } else {
        MOD_LOGW("ApplyPatches: one or more patches failed.");
    }

    return ok;
}
