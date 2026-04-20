# Decompiled Android Studio Project (JADX)

This folder is generated from:
- APK: `/home/kali/aredaavvsh/.tmp_apk_inspect/pc_simulator.apk`
- Tool: `jadx 1.5.5` with `--export-gradle --deobf --show-bad-code`

## Import
Open this folder directly in Android Studio:
- `reverse/jadx_androidstudio`

## Current status
- Gradle project files were generated (`build.gradle`, `app/build.gradle`, `settings.gradle`).
- Java sources and resources extracted.
- Native libs extracted under `app/src/main/lib/*`.
- Unity IL2CPP assets extracted under `app/src/main/assets/bin/Data/*`.

## Important caveat
This is a reverse-engineered project. Core gameplay is still native IL2CPP in:
- `app/src/main/lib/arm64-v8a/libil2cpp.so`
- `app/src/main/assets/bin/Data/Managed/Metadata/global-metadata.dat`

So this Java project is mainly Android shell / SDK glue code, not full game logic.
