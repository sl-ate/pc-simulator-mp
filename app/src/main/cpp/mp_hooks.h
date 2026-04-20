#pragma once

#include <cstdint>

struct MpIl2CppOffsets {
    uintptr_t network_tick;
    uintptr_t serialize_local_player;
    uintptr_t apply_remote_snapshot;
};

bool InitializeMpHookPipeline();
bool SetMpIl2CppOffsets(const MpIl2CppOffsets& offsets);
const MpIl2CppOffsets& GetMpIl2CppOffsets();
