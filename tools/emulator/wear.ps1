<#
 Wear OS smoke (AII-412): boots the headless Wear emulator (aigile_wear, port 5586, no window), installs the watch app
 and prints the texts on its screen. Pairing with a phone emulator is not done here: expect "Waiting for your phone...".
 Build first: ./gradlew :wear:assembleDebug
#>
param(
  [string]$Apk = (Join-Path $PSScriptRoot '../../wear/build/outputs/apk/debug/wear-debug.apk'),
  [string]$Avd = 'aigile_wear',
  [string]$Sdk = "$env:LOCALAPPDATA/AndroidTools/sdk",
  [string]$Jdk = "$env:LOCALAPPDATA/AndroidTools/jdk17",
  [int]$BootSeconds = 400
)
$ErrorActionPreference = 'Continue'
$env:ANDROID_HOME = $Sdk; $env:ANDROID_SDK_ROOT = $Sdk; $env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$Sdk\cmdline-tools\latest\bin;$Sdk\platform-tools;$env:PATH"
$adb = "$Sdk/platform-tools/adb.exe"
$serial = 'emulator-5586'
$pkg = 'com.smizflark.aigile.messenger'
$emu = $null
function Done($code) {
  try { & $adb -s $serial emu kill 2>$null | Out-Null } catch {}
  Start-Sleep 3
  if ($emu -and -not $emu.HasExited) { Stop-Process -Id $emu.Id -Force -ErrorAction SilentlyContinue }
  exit $code
}
function Fail($m) { Write-Host "FAIL: $m"; Done 1 }
if (-not (Test-Path $Apk)) { Fail "APK not found: $Apk (run gradlew :wear:assembleDebug first)" }
if (-not (Test-Path "$Sdk/system-images/android-34/android-wear/x86_64")) { Fail "no Wear image: sdkmanager 'system-images;android-34;android-wear;x86_64'" }
if (-not (Test-Path "$env:USERPROFILE/.android/avd/$Avd.avd")) {
  'no' | & avdmanager create avd -n $Avd -k 'system-images;android-34;android-wear;x86_64' -d wearos_small_round --force | Out-Null
}
Write-Host "starting headless Wear emulator ($Avd)"
$emu = Start-Process "$Sdk/emulator/emulator.exe" -ArgumentList @('-avd', $Avd, '-port', '5586', '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot', '-gpu', 'swiftshader_indirect') -WindowStyle Hidden -PassThru
$env:ANDROID_SERIAL = $serial
& $adb start-server | Out-Null
& $adb wait-for-device
$deadline = (Get-Date).AddSeconds($BootSeconds)
while ((& $adb shell getprop sys.boot_completed 2>$null) -notmatch '1') {
  if ((Get-Date) -gt $deadline) { Fail 'boot timeout' }
  Start-Sleep 3
}
Write-Host 'booted'
& $adb install -r $Apk | Out-String | Write-Host
$main = (& $adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | Select-Object -Last 1).Trim()
& $adb shell am start -W -n $main | Out-Null
$texts = @()
for ($i = 0; $i -lt 15 -and -not $texts; $i++) {
  Start-Sleep 3
  & $adb shell uiautomator dump /sdcard/ui.xml 2>&1 | Out-Null
  try { $x = [xml](& $adb exec-out cat /sdcard/ui.xml 2>$null); $texts = @($x.SelectNodes('//node') | Where-Object { $_.text } | ForEach-Object { $_.text }) } catch {}
}
Write-Host ("screen texts: " + ($texts -join ' | '))
if ($texts -match 'Waiting|Wachten|chat') { Write-Host 'PASS'; Done 0 }
Fail 'watch app showed no expected text'
