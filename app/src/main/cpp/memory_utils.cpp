#include "memory_utils.h"

#include <sys/mman.h>
#include <unistd.h>

#include <cstdio>
#include <cstring>
#include <cerrno>
#include <cinttypes>

#include "common.h"

uintptr_t FindLibraryBase(const char* lib_name) {
    if (lib_name == nullptr) {
        return 0;
    }

    FILE* fp = fopen("/proc/self/maps", "r");
    if (fp == nullptr) {
        return 0;
    }

    uintptr_t base = 0;
    char line[512];
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (strstr(line, lib_name) == nullptr) {
            continue;
        }

        uintptr_t start = 0;
        if (sscanf(line, "%" SCNxPTR "-%*" SCNxPTR, &start) == 1) {
            base = start;
            break;
        }
    }

    fclose(fp);
    return base;
}

void* ResolveAddress(const char* lib_name, uintptr_t offset) {
    const uintptr_t base = FindLibraryBase(lib_name);
    if (base == 0) {
        return nullptr;
    }
    return reinterpret_cast<void*>(base + offset);
}

bool WriteMemory(void* address, const void* data, size_t size) {
    if (address == nullptr || data == nullptr || size == 0) {
        return false;
    }

    const long page_size = sysconf(_SC_PAGESIZE);
    if (page_size <= 0) {
        return false;
    }

    uintptr_t start = reinterpret_cast<uintptr_t>(address);
    uintptr_t page_start = start & ~(static_cast<uintptr_t>(page_size) - 1);
    uintptr_t page_end = (start + size + static_cast<uintptr_t>(page_size) - 1) &
                         ~(static_cast<uintptr_t>(page_size) - 1);
    size_t protect_size = page_end - page_start;

    if (mprotect(reinterpret_cast<void*>(page_start), protect_size, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        MOD_LOGE("mprotect RWX failed: %s", strerror(errno));
        return false;
    }

    memcpy(address, data, size);
    __builtin___clear_cache(reinterpret_cast<char*>(address), reinterpret_cast<char*>(address) + size);

    if (mprotect(reinterpret_cast<void*>(page_start), protect_size, PROT_READ | PROT_EXEC) != 0) {
        MOD_LOGW("mprotect RX restore failed: %s", strerror(errno));
    }
    return true;
}
