# Emulator smoke test

`powershell -File tools/emulator/smoke.ps1 [-Apk path] [-Avd name] [-KeepAvd]`

Boots a headless emulator (`-no-window -no-audio -no-boot-anim`, hidden process, no window or console),
installs `app/build/outputs/apk/debug/app-debug.apk`, taps "Try the demo", opens the Tunio chat,
sends a unique message, checks it appears via `uiautomator dump`, then kills the emulator and deletes the temp AVD.
Exit code 0 = pass, 1 = fail.

Needs (SDK at `C:/Users/rich/AppData/Local/AndroidTools/sdk`): `emulator` package and a
`system-images;android-NN;google_apis;x86_64` image, about 6+ GB; build the APK first with `gradlew assembleDebug`.
First passed 2026-10-13 on android-34 (WHPX). It only talks to emulator-5584, never a plugged-in phone. On a fail the screen layout is saved to `%TEMP%\smoke-ui.xml`; on a pass a screenshot goes to `%TEMP%\smoke-pass.png`.
