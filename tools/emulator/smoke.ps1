<#
 Headless emulator smoke test: boot, install, demo -> chat -> send -> verify, shut down.
 Usage: powershell -File tools/emulator/smoke.ps1 [-Apk path] [-Avd aigile_smoke] [-KeepAvd]
 Never opens a window: emulator runs -no-window with a hidden process.
#>
param(
  [string]$Apk = (Join-Path $PSScriptRoot '..\..\app\build\outputs\apk\debug\app-debug.apk'),
  [string]$Avd = 'aigile_smoke',
  [string]$Sdk = 'C:/Users/rich/AppData/Local/AndroidTools/sdk',
  [string]$Jdk = 'C:/Users/rich/AppData/Local/AndroidTools/jdk17',
  [int]$BootSeconds = 300,
  [switch]$KeepAvd
)
$ErrorActionPreference = 'Continue'   # adb talks on stderr; failures are caught by Fail below
$env:ANDROID_HOME = $Sdk; $env:ANDROID_SDK_ROOT = $Sdk; $env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$Sdk\cmdline-tools\latest\bin;$Sdk\platform-tools;$env:PATH"
$adb = "$Sdk/platform-tools/adb.exe"
$emuExe = "$Sdk/emulator/emulator.exe"
$pkg = 'com.smizflark.aigile.messenger'
$msg = 'smoke' + (Get-Random -Maximum 9999)
$script:emu = $null

function Fail($m) { Write-Host "FAIL: $m"; try { (Dump).OuterXml | Out-File (Join-Path $env:TEMP 'smoke-ui.xml') -Encoding utf8; Write-Host "screen saved to TEMP\smoke-ui.xml" } catch {}; Cleanup; exit 1 }
function Cleanup {
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
function TapWhenFound($pattern, $what, $tries = 20) {
  for ($i = 0; $i -lt $tries; $i++) {
    # a slow first boot can show "X isn't responding": tap Wait and carry on
    if ((Dump).OuterXml -match "isn.t responding") { $w = Find '^Wait$'; if ($w) { & $adb shell input tap $w[0] $w[1]; Write-Host "dismissed a not-responding popup" } }
    $p = Find $pattern
    if ($p) { & $adb shell input tap $p[0] $p[1]; Write-Host "tapped $what"; return }
    Start-Sleep 1
  }
  Fail "could not find '$what'"
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
& $adb shell pm grant $pkg android.permission.POST_NOTIFICATIONS 2>$null
# am start, not monkey: monkey gives up if Play Services freezes during a cold boot
& $adb shell input keyevent 3
$main = (& $adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | Select-Object -Last 1).Trim()
& $adb shell am start -W -n $main | Out-Null

TapWhenFound 'Try the demo' 'Try the demo'
TapWhenFound 'Tunio' 'Tunio chat' 30
TapWhenFound '^Message$|Message the' 'message field'
& $adb shell input text $msg
TapWhenFound '^Send$' 'Send'
Start-Sleep 2
$ui = (Dump).OuterXml
if ($ui -notmatch [regex]::Escape($msg)) { Fail "sent text '$msg' not visible after send" }
Write-Host "sent '$msg' shown in chat"

# AII-92: pictures on a real phone (the JVM tests can't draw them): details -> hills full screen -> swipe
TapWhenFound '^Back$' 'Back to chats'
TapWhenFound '^AI-gile$' 'AI-gile chat' 30
Start-Sleep 2
TapWhenFound '^AI-gile$' 'chat name'
TapWhenFound '^hills\.png$' 'hills picture' 15
Start-Sleep 2
$g = Find '^Got it$'   # Android's one-time "Viewing full screen" hint (AII-93 hides the bars)
if ($g) { & $adb shell input tap $g[0] $g[1]; Start-Sleep 1 }
$x = Dump
$count = ($x.SelectNodes('//node') | Where-Object { $_.text -match '^\d+ / \d+$' } | Select-Object -First 1).text
if (-not $count) { Fail "no 'N / N' count on the full screen picture" }
Write-Host "full screen at $count"
if ($count -match '^1 /') { & $adb shell input swipe 900 1200 150 1200 250 } else { & $adb shell input swipe 150 1200 900 1200 250 }
Start-Sleep 2
$after = ((Dump).SelectNodes('//node') | Where-Object { $_.text -match '^\d+ / \d+$' } | Select-Object -First 1).text
if ($after -eq $count) { Fail "swipe did not change picture (still $count)" }
Write-Host "swiped to $after"
# AII-95: Save puts the picture in Pictures/AI-gile
TapWhenFound '^Save to phone$' 'Save'
Start-Sleep 2
$media = & $adb shell content query --uri content://media/external/images/media --projection _display_name:relative_path 2>$null | Out-String
if ($media -notmatch 'AI-gile') { Fail "saved picture not found in Pictures/AI-gile" }
Write-Host "picture saved to Pictures/AI-gile"
& $adb shell screencap -p /sdcard/smoke.png; & $adb pull /sdcard/smoke.png (Join-Path $env:TEMP 'smoke-pass.png') 2>$null | Out-Null
# AII-96: Share opens Android's share menu
TapWhenFound '^Share$' 'Share'
$shared = $false
for ($i = 0; $i -lt 10 -and -not $shared; $i++) { Start-Sleep 1; $shared = (Dump).OuterXml -match 'package="(com\.android\.intentresolver|android)"' }
if (-not $shared) { Fail "share menu did not open" }
Write-Host "share menu opened"
& $adb shell input keyevent 4
Write-Host "PASS: message sent, pictures swipe, save and share (screen in TEMP\smoke-pass.png)"; Cleanup; exit 0
