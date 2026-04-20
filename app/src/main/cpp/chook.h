#pragma once

#include <cstddef>
#include <cstdint>

class CHook {
public:
    static void SetLibrary(const char* lib_name);
    static const char* GetLibrary();

    static uintptr_t Base();
    static void* Addr(uintptr_t offset);
    static void* Addr(const char* lib_name, uintptr_t offset);

    static bool Patch(uintptr_t offset, const void* data, size_t size);
    static bool Patch(const char* lib_name, uintptr_t offset, const void* data, size_t size);
    static bool PatchAbsolute(uintptr_t absolute_address, const void* data, size_t size);

    static bool RET(uintptr_t offset);
    static bool RET(const char* lib_name, uintptr_t offset);

    static bool RET_TRUE(uintptr_t offset);
    static bool RET_TRUE(const char* lib_name, uintptr_t offset);

    static bool RET_FALSE(uintptr_t offset);
    static bool RET_FALSE(const char* lib_name, uintptr_t offset);

    static bool NOP(uintptr_t offset, size_t instruction_count = 1);
    static bool NOP(const char* lib_name, uintptr_t offset, size_t instruction_count = 1);

    static bool Hook(uintptr_t offset, void* replacement, void** original);
    static bool Hook(const char* lib_name, uintptr_t offset, void* replacement, void** original);
    static bool HookAbsolute(uintptr_t absolute_address, void* replacement, void** original);
};
