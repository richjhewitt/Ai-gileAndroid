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
. (Join-Path $PSScriptRoot 'emu.ps1')   # helpers, boot, install, first screen
$msg = 'smoke' + (Get-Random -Maximum 9999)

TapWhenFound 'Try the demo' 'Try the demo'
TapWhenFound 'Got it' 'welcome tips' 30   # AII-276
TapWhenFound 'Tunio' 'Tunio chat' 30
# A tap while the welcome tips are still closing can be lost: tap the row again until the chat is open
for ($t = 0; $t -lt 3 -and -not (Find '^Message$|Message the'); $t++) { Start-Sleep 2; if (-not (Find '^Message$|Message the')) { TapWhenFound 'Tunio' 'Tunio chat (again)' 10 } }
TapWhenFound '^Message$|Message the' 'message field'
& $adb shell input text $msg
TapWhenFound '^Send$' 'Send'
$ok = $false
for ($i = 0; $i -lt 5 -and -not $ok; $i++) { Start-Sleep 2; DismissPopup; $ok = (Dump).OuterXml -match [regex]::Escape($msg) }
if (-not $ok) { Fail "sent text '$msg' not visible after send" }
Write-Host "sent '$msg' shown in chat"

# AII-201/251: hold the message -> Remind me -> In 1 hour -> (note dialog) Skip -> a bell line shows under it
$bell = [string][char]0xD83D + [char]0xDD14
$ok = $false
for ($try = 0; $try -lt 4 -and -not $ok; $try++) {
  $p = Find ([regex]::Escape($msg))
  if ($p) { & $adb shell input swipe $p[0] $p[1] $p[0] $p[1] 900 }
  for ($i = 0; $i -lt 4 -and -not $ok; $i++) { Start-Sleep 1; $ok = [bool](Find '^Remind me$') }
  if (-not $ok) { & $adb shell input keyevent 4; Start-Sleep 1 }   # menu did not open (message still sending?): retry
}
if (-not $ok) { Fail "hold on message did not show 'Remind me'" }
TapWhenFound '^Remind me$' 'Remind me'
TapWhenFound '^In 1 hour$' 'In 1 hour'
TapWhenFound '^Skip$' 'Skip note'
$ok = $false
for ($i = 0; $i -lt 10 -and -not $ok; $i++) { Start-Sleep 1; $ok = (Dump).OuterXml -match [regex]::Escape($bell) }
if (-not $ok) { Fail "no bell line under the message after Remind me" }
Write-Host "Remind me in 1 hour shows the bell line"

# AII-204: another demo chat is unread and this one is at the bottom, so Next unread shows
$ok = $false
for ($i = 0; $i -lt 5 -and -not $ok; $i++) { $ok = [bool](Find '^Next unread'); if (-not $ok) { Start-Sleep 1 } }
if (-not $ok) { Fail "no 'Next unread' button at the bottom of the chat" }
Write-Host "Next unread button shown"

# AII-273/279: the friends demo path: open the TUN-7 card to test, Accept, and the buttons go
for ($i = 0; $i -lt 6 -and -not (Find '^Open TUN-7$'); $i++) { & $adb shell input swipe 540 700 540 1700 300; Start-Sleep 1 }   # AII-292: the sent message and bell line push its header off the top
TapWhenFound '^Open TUN-7$' 'TUN-7 card'
for ($i = 0; $i -lt 6 -and -not (Find '^Accept$'); $i++) { & $adb shell input swipe 540 700 540 1700 300; Start-Sleep 1 }   # the card view opens at the newest message: scroll up to its steps
TapWhenFound '^Accept$' 'Accept'
$ok = $false
for ($i = 0; $i -lt 5 -and -not $ok; $i++) { Start-Sleep 1; $ok = -not (Find '^Accept$') }
if (-not $ok) { Fail "Accept still showing after tapping it" }
Write-Host "card accepted"
TapWhenFound '^Back$' 'Back to the chat'

# AII-92: pictures on a real phone (the JVM tests can't draw them): details -> hills full screen -> swipe
TapWhenFound '^Back$' 'Back to chats'

# AII-232/202: Settings has its section headings and the Undo send switch (scroll down to find each)
function ScrollFind($pattern, $what) {
  for ($i = 0; $i -lt 12; $i++) {
    for ($j = 0; $j -lt 3; $j++) { if (Find $pattern) { Write-Host "settings shows '$what'"; return }; Start-Sleep 1 }   # the screen may still be drawing
    & $adb shell input swipe 540 1700 540 700 300; Start-Sleep 1
  }
  Fail "Settings: '$what' not found"
}
TapWhenFound '^Settings$' 'Settings'
foreach ($h in 'Connection', 'Look', 'Chats') { ScrollFind "^$h$" "$h heading" }
ScrollFind '^Undo send$' 'Undo send switch'   # in the Chats section
foreach ($h in 'Alerts', 'Privacy') { ScrollFind "^$h$" "$h heading" }
TapWhenFound '^Back$' 'Back to chats'
TapWhenFound '^AI-gile$' 'AI-gile chat' 30
Start-Sleep 2
TapWhenFound '^AI-gile$' 'chat name'
for ($i = 0; $i -lt 6 -and -not (Find '^hills\.png$'); $i++) { & $adb shell input swipe 540 1800 540 900 300; Start-Sleep 1 }   # the details sheet is long now: scroll to the pictures
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
# AII-278/279: Leave the demo goes back to the first screen
for ($i = 0; $i -lt 4 -and -not (Find '^Settings$'); $i++) { & $adb shell input keyevent 4; Start-Sleep 1 }
TapWhenFound '^Settings$' 'Settings'
ScrollFind '^Leave the demo$' 'Leave the demo'
TapWhenFound '^Leave the demo$' 'Leave the demo'
TapWhenFound '^Leave$' 'Leave'
$ok = $false; for ($i = 0; $i -lt 10 -and -not $ok; $i++) { Start-Sleep 1; $ok = [bool](Find 'Try the demo') }
if (-not $ok) { Fail "Leave the demo did not go back to the first screen" }
Write-Host "left the demo"
# AII-289/293: only a real Sign out puts Scan QR code first; leaving the demo keeps Try the demo on top
$d = Find '^Try the demo$'; $q = Find '^Scan QR code$'
if (-not $d -or -not $q) { Fail "first screen is missing Try the demo or Scan QR code" }
if ($d[1] -gt $q[1]) { Fail "after Leave the demo, Scan QR code is above Try the demo" }
Write-Host "Leave the demo keeps Try the demo first"
Write-Host "PASS: message sent, card accepted, pictures swipe, save and share, left the demo (screen in TEMP\smoke-pass.png)"; Cleanup; exit 0
