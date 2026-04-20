# IL2CPP Address Notes (arm64)

Binary:
- `app/src/main/lib/arm64-v8a/libil2cpp.so`
- BuildID: `6730f373b0dcc3429aded73cc934a7eb40b18391`

## Base sections

- Entry: `0x005F54A0`
- `.text` start: `0x005F54A0`
- `il2cpp` section start: `0x0083C008`
- `.rodata` start: `0x01C2D090`

## Candidate patch offsets (RVA)

From `r2` around existing patch area:

- `0x0083C1F0` (small function, 5 instructions)
- `0x0083C1FC` (2-instruction getter)
  - `ldrb w0, [x0, 0x60]`
  - `ret`
- `0x0083C204` (3-instruction setter)
  - `and w8, w1, 1`
  - `strb w8, [x0, 0x60]`
  - `ret`

These 3 RVAs are suitable for boolean-force patches via C++ (`RET_TRUE`, `RET_FALSE`, or hook).

## Commands used

```bash
objdump -h app/src/main/lib/arm64-v8a/libil2cpp.so
objdump -T app/src/main/lib/arm64-v8a/libil2cpp.so
r2 -q -c "e asm.arch=arm; e asm.bits=64; s 0x83c1d0; pd 24" app/src/main/lib/arm64-v8a/libil2cpp.so
r2 -q -c "aa; af @ 0x83c1f0; afi @ 0x83c1f0; af @ 0x83c204; afi @ 0x83c204" app/src/main/lib/arm64-v8a/libil2cpp.so
```
