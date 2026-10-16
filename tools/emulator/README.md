# Emulator smoke test

`powershell -File tools/emulator/smoke.ps1 [-Apk path] [-Avd name] [-KeepAvd]`

Boots a headless emulator (`-no-window -no-audio -no-boot-anim`, hidden process, no window or console),
installs `app/build/outputs/apk/debug/app-debug.apk`, taps "Try the demo", opens the Tunio chat,
sends a unique message, checks it appears via `uiautomator dump`, opens AI-gile > chat name > hills picture full screen, swipes to the next (AII-92) and saves it to Pictures/AI-gile (AII-95) and opens the share menu (AII-96), then kills the emulator and deletes the temp AVD.
Exit code 0 = pass, 1 = fail.

Needs (SDK at `C:/Users/rich/AppData/Local/AndroidTools/sdk`): `emulator` package and a
`system-images;android-NN;google_apis;x86_64` image, about 6+ GB; build the APK first with `gradlew assembleDebug`.
First passed 2026-10-13 on android-34 (WHPX). It only talks to emulator-5584, never a plugged-in phone. On a fail the screen layout is saved to `%TEMP%\smoke-ui.xml`; on a pass a screenshot goes to `%TEMP%\smoke-pass.png`.

## Real board smoke (AII-318)

`powershell -File tools/emulator/smoke-board.ps1 [-Apk path] [-Avd name] [-KeepAvd]`

Starts its own throwaway test board (state `%TEMP%/aigile-testboard-emu.json`, never the one paired with Rich's phone),
pairs the app with it by link (`testboard.mjs emulator`), sends a message and checks it reached the board, then posts
a Lead answer on the board and checks it shows in the app. Needs the debug APK: only it allows plain http to 10.0.2.2.
Both scripts share `emu.ps1` (helpers, boot, install). On a pass a screenshot goes to `%TEMP%/smoke-board-pass.png`.
