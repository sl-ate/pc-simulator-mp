#include "mp_hooks.h"

#include <atomic>

#include "chook.h"
#include "common.h"

namespace {
std::atomic<bool> g_pipeline_initialized{false};
MpIl2CppOffsets g_offsets{0, 0, 0};

bool HasConfiguredOffsets(const MpIl2CppOffsets& offsets) {
    return offsets.network_tick != 0 && offsets.serialize_local_player != 0 && offsets.apply_remote_snapshot != 0;
}

bool InstallConfiguredHooks() {
    if (!HasConfiguredOffsets(g_offsets)) {
        MOD_LOGI("MP hooks: no IL2CPP offsets configured yet (pipeline ready for disassembly phase).");
        return true;
    }

    bool ok = true;

    // TODO(disassembly): Replace placeholders with concrete hooks after mapping method RVA offsets.
    // Example target functions:
    // - network_tick
    // - serialize_local_player
    // - apply_remote_snapshot

    if (ok) {
        MOD_LOGI("MP hooks: configured offsets accepted (network_tick=0x%lx serialize=0x%lx apply=0x%lx)",
                 static_cast<unsigned long>(g_offsets.network_tick),
                 static_cast<unsigned long>(g_offsets.serialize_local_player),
                 static_cast<unsigned long>(g_offsets.apply_remote_snapshot));
    }

    return ok;
}
}  // namespace

bool SetMpIl2CppOffsets(const MpIl2CppOffsets& offsets) {
    g_offsets = offsets;
    MOD_LOGI("MP hooks: offsets updated.");
    return true;
}

const MpIl2CppOffsets& GetMpIl2CppOffsets() {
    return g_offsets;
}

bool InitializeMpHookPipeline() {
    bool expected = false;
    if (!g_pipeline_initialized.compare_exchange_strong(expected, true)) {
        return true;
    }

    CHook::SetLibrary("libil2cpp.so");
    if (CHook::Base() == 0) {
        MOD_LOGW("MP hooks: libil2cpp base unavailable; initialization deferred.");
        g_pipeline_initialized.store(false);
        return false;
    }

    const bool installed = InstallConfiguredHooks();
    if (!installed) {
        g_pipeline_initialized.store(false);
    }
    return installed;
}
