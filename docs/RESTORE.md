# Restore after a PC reinstall

## Code
- Everything is on GitHub: https://github.com/richjhewitt/Ai-gileAndroid (main plus the unfinished branches)
- Clone it back into `Bots 'n' Code Stuff/AI-gile Android`

## App signing key (not on GitHub, on purpose)
- Backup copy: **R:\messenger.jks** (the Backups drive), copied 2026-10-17
- Put it back as `keystore/messenger.jks` in the project folder
- Without it, phones that have the app installed can't take updates from new builds, and the watch app can't pair with the phone app

## Not needed
- `local.properties` is made again by the Android tools (one line: `sdk.dir=...`)
- Build folders, `.gradle`, `.kotlin`: made again on the first build
