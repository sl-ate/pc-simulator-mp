#include "hooks.h"

#include "chook.h"
#include "common.h"

bool InstallHooks() {
    CHook::SetLibrary("libil2cpp.so");
    MOD_LOGI("InstallHooks: no active function hooks (CHook core ready).");
    return true;
}
