<#
 Shared by smoke.ps1 and smoke-board.ps1 (dot-sourced): helpers, then boot the headless emulator, install the APK
 and open the app on its first screen. Uses the caller's $Apk, $Avd, $Sdk, $Jdk, $BootSeconds, $KeepAvd;
 $script:onCleanup (a script block) runs on the way out, pass or fail.
#>
$ErrorActionPreference = 'Continue'   # adb talks on stderr; failures are caught by Fail below
$env:ANDROID_HOME = $Sdk; $env:ANDROID_SDK_ROOT = $Sdk; $env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$Sdk\cmdline-tools\latest\bin;$Sdk\platform-tools;$env:PATH"
$adb = "$Sdk/platform-tools/adb.exe"
$emuExe = "$Sdk/emulator/emulator.exe"
$pkg = 'com.smizflark.aigile.messenger'
$script:emu = $null

function Fail($m) { Write-Host "FAIL: $m"; try { (Dump).OuterXml | Out-File (Join-Path $env:TEMP 'smoke-ui.xml') -Encoding utf8; Write-Host "screen saved to TEMP\smoke-ui.xml" } catch {}; try { & $adb logcat -d -b crash,main -t 400 | Out-File (Join-Path $env:TEMP 'smoke-crash.txt') -Encoding utf8; Write-Host "crash log saved to TEMP\smoke-crash.txt" } catch {}; Cleanup; exit 1 }
function Cleanup {
  if ($script:onCleanup) { try { & $script:onCleanup } catch {} }
  try { & $adb -s emulator-5584 emu kill 2>$null | Out-Null } catch {}
  Start-Sleep 3
  if ($script:emu -and -not $script:emu.HasExited) { Stop-Process -Id $script:emu.Id -Force -ErrorAction SilentlyContinue }
  if (-not $KeepAvd) { try { & avdmanager delete avd -n $Avd 2>$null | Out-Null } catch {} }
}
function Dump {
  # adb writes "null root node" to stderr while the screen is still drawing; that is a retry, not a failure
  $ErrorActionPreference = 'Continue'
  & $adb shell uiautomator dump /sdcard/ui.xml 2>&1 | Out-Null
  try { [xml](& $adb exec-out cat /sdcard/ui.xml 2>$null) } catch { [xml]'<hierarchy/>' }
}
# find a node whose text/desc/resource-id matches, return centre point
function Find($pattern) {
  $x = Dump
  foreach ($n in $x.SelectNodes('//node')) {
    if (@($n.text, $n.'content-desc', $n.'resource-id') -match $pattern -and $n.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
      return @([int](([int]$Matches[1]+[int]$Matches[3])/2), [int](([int]$Matches[2]+[int]$Matches[4])/2))
    }
  }
  $null
}
# a slow first boot can show "X isn't responding": tap Wait and carry on
function DismissPopup {
  if ((Dump).OuterXml -match "isn.t responding") { $w = Find '^Wait$'; if ($w) { & $adb shell input tap $w[0] $w[1]; Write-Host "dismissed a not-responding popup" } }
}
function TapWhenFound($pattern, $what, $tries = 20) {
  for ($i = 0; $i -lt $tries; $i++) {
    DismissPopup
    $p = Find $pattern
    if ($p) { & $adb shell input tap $p[0] $p[1]; Write-Host "tapped $what"; return }
    Start-Sleep 1
  }
  Fail "could not find '$what'"
}
# wait until the screen shows text matching $pattern (scrolling nothing), or fail
function WaitFor($pattern, $what, $tries = 10) {
  for ($i = 0; $i -lt $tries; $i++) { DismissPopup; if ((Dump).OuterXml -match $pattern) { return }; Start-Sleep 2 }
  Fail "$what not shown"
}

if (-not (Test-Path $Apk)) { Fail "APK not found: $Apk (run gradlew assembleDebug first)" }
if (-not (Test-Path $emuExe)) { Fail "emulator package missing: sdkmanager 'emulator'" }
$img = Get-ChildItem "$Sdk/system-images/*/google_apis/x86_64" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $img) { Fail "no google_apis x86_64 system image: sdkmanager 'system-images;android-34;google_apis;x86_64'" }
$api = $img.Parent.Parent.Name

if (-not (Test-Path "$env:USERPROFILE/.android/avd/$Avd.avd")) {
  'no' | & avdmanager create avd -n $Avd -k "system-images;$api;google_apis;x86_64" -d pixel_6 --force | Out-Null
}
Write-Host "starting headless emulator ($Avd, $api)"
$script:emu = Start-Process $emuExe -ArgumentList @('-avd', $Avd, '-port', '5584', '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot', '-gpu', 'swiftshader_indirect') -WindowStyle Hidden -PassThru
$env:ANDROID_SERIAL = 'emulator-5584'   # only ever talk to this emulator, never a plugged-in phone
& $adb start-server | Out-Null
& $adb wait-for-device
$deadline = (Get-Date).AddSeconds($BootSeconds)
while ((& $adb shell getprop sys.boot_completed 2>$null) -notmatch '1') {
  if ((Get-Date) -gt $deadline) { Fail "boot timeout" }
  Start-Sleep 3
}
Write-Host "booted"
& $adb shell settings put global window_animation_scale 0
& $adb shell settings put global transition_animation_scale 0
& $adb shell settings put global animator_duration_scale 0
& $adb shell input keyevent 82

& $adb install -r $Apk | Out-String | Write-Host
# Start from a fresh app every run: a pairing left over from an earlier run skips the Connect screen
& $adb shell pm clear $pkg | Out-Null
& $adb shell pm grant $pkg android.permission.POST_NOTIFICATIONS 2>$null
# am start, not monkey: monkey gives up if Play Services freezes during a cold boot
& $adb shell input keyevent 3
$main = (& $adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | Select-Object -Last 1).Trim()
& $adb shell am start -W -n $main | Out-Null
# a cold boot can swallow the first start: launch again until the sign-in screen shows
for ($t = 0; $t -lt 3 -and -not (Find 'Try the demo'); $t++) { Start-Sleep 5; if (-not (Find 'Try the demo')) { & $adb shell am start -W -n $main | Out-Null } }
