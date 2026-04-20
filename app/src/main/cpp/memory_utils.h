#pragma once

#include <cstddef>
#include <cstdint>

uintptr_t FindLibraryBase(const char* lib_name);
void* ResolveAddress(const char* lib_name, uintptr_t offset);
bool WriteMemory(void* address, const void* data, size_t size);
