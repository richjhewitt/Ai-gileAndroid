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
$ErrorActionPreference = 'Stop'
$env:ANDROID_HOME = $Sdk; $env:ANDROID_SDK_ROOT = $Sdk; $env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$Sdk\cmdline-tools\latest\bin;$Sdk\platform-tools;$env:PATH"
$adb = "$Sdk/platform-tools/adb.exe"
$emuExe = "$Sdk/emulator/emulator.exe"
$pkg = 'com.smizflark.aigile.messenger'
$msg = 'smoke' + (Get-Random -Maximum 9999)
$script:emu = $null

function Fail($m) { Write-Host "FAIL: $m"; Cleanup; exit 1 }
function Cleanup {
  try { & $adb -e emu kill 2>$null | Out-Null } catch {}
  Start-Sleep 3
  if ($script:emu -and -not $script:emu.HasExited) { Stop-Process -Id $script:emu.Id -Force -ErrorAction SilentlyContinue }
  if (-not $KeepAvd) { try { & avdmanager delete avd -n $Avd 2>$null | Out-Null } catch {} }
}
function Dump {
  & $adb shell uiautomator dump /sdcard/ui.xml 2>&1 | Out-Null
  [xml](& $adb exec-out cat /sdcard/ui.xml)
}
# find a node whose text/desc/resource-id matches, return centre point
function Find($pattern) {
  $x = Dump
  foreach ($n in $x.SelectNodes('//node')) {
    if (("$($n.text)|$($n.'content-desc')|$($n.'resource-id')") -match $pattern -and $n.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
      return @([int](([int]$Matches[1]+[int]$Matches[3])/2), [int](([int]$Matches[2]+[int]$Matches[4])/2))
    }
  }
  $null
}
function TapWhenFound($pattern, $what, $tries = 20) {
  for ($i = 0; $i -lt $tries; $i++) {
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
$script:emu = Start-Process $emuExe -ArgumentList @('-avd', $Avd, '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot', '-gpu', 'swiftshader_indirect') -WindowStyle Hidden -PassThru
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
& $adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Out-Null

TapWhenFound 'Try the demo' 'Try the demo'
TapWhenFound 'Tunio' 'Tunio chat' 30
TapWhenFound '^Message$|Message the' 'message field'
& $adb shell input text $msg
TapWhenFound '^Send$' 'Send'
Start-Sleep 2
$ui = (Dump).OuterXml
if ($ui -match [regex]::Escape($msg)) { Write-Host "PASS: '$msg' shown in chat"; Cleanup; exit 0 }
Fail "sent text '$msg' not visible after send"
