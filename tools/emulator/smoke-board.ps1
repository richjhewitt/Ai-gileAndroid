<#
 Emulator smoke with a real board (AII-318): its own throwaway test board (never the one paired with Rich's phone),
 the app paired with it by link, a message sent from the app must land on the board, and the Lead's answer on the
 board must show in the app. Needs the debug APK (only it allows plain http to the PC, src/debug network config).
 Usage: powershell -File tools/emulator/smoke-board.ps1 [-Apk path] [-Avd aigile_smoke] [-KeepAvd]
#>
param(
  [string]$Apk = (Join-Path $PSScriptRoot '..\..\app\build\outputs\apk\debug\app-debug.apk'),
  [string]$Avd = 'aigile_smoke',
  [string]$Sdk = 'C:/Users/rich/AppData/Local/AndroidTools/sdk',
  [string]$Jdk = 'C:/Users/rich/AppData/Local/AndroidTools/jdk17',
  [int]$BootSeconds = 300,
  [switch]$KeepAvd
)
$tb = Join-Path $PSScriptRoot '..\testboard\testboard.mjs'
$env:AIGILE_TESTBOARD_STATE = Join-Path $env:TEMP 'aigile-testboard-emu.json'
node $tb stop | Out-Null   # a board left over from a broken run
$base = (node $tb start | Select-Object -Last 1)
if ($base -notmatch '^http://127\.0\.0\.1:\d+$') { Write-Host "FAIL: test board did not start: $base"; exit 1 }
$link = (node $tb emulator | Select-Object -Last 1)
if ($link -notmatch '^aigile://pair\?url=http://10\.0\.2\.2:\d+&id=') { node $tb stop | Out-Null; Write-Host "FAIL: no pairing link: $link"; exit 1 }
$script:onCleanup = { node $tb stop | Out-Null }
$H = @{ Authorization = 'Bearer test-key' }
function BoardText { (Invoke-RestMethod "$base/api/projects/test-team" -Headers $H) | ConvertTo-Json -Depth 20 -Compress }

. (Join-Path $PSScriptRoot 'emu.ps1')   # helpers, boot, install, first screen
$msg = 'boardsmoke' + (Get-Random -Maximum 9999)
$answer = 'leadanswer' + (Get-Random -Maximum 9999)

# Pair: the link fills in Connect (as a tapped link would), then Connect
& $adb shell "am start -a android.intent.action.VIEW -d '$link' $pkg" | Out-Null
TapWhenFound '^Connect$' 'Connect'
TapWhenFound '^Test Team$' 'Test Team chat' 30
WaitFor 'the test board is up' "the Lead's seeded message"
Write-Host "paired: the board's chat opened"

# App -> board
TapWhenFound '^Message$|Message the' 'message field'
& $adb shell input text $msg
WaitFor ([regex]::Escape($msg)) "typed '$msg'" 5   # the keyboard can lag a key behind: never send half a word
TapWhenFound '^Send$' 'Send'
$ok = $false
for ($i = 0; $i -lt 15 -and -not $ok; $i++) { Start-Sleep 2; $ok = (BoardText) -match $msg }
if (-not $ok) { Write-Host ((BoardText) -replace '.*"comments":', '') ; Fail "sent '$msg' never reached the board" }
Write-Host "sent '$msg' is on the board"

# Board -> app: the Lead answers on the chat's card
$card = ((Invoke-RestMethod "$base/api/projects/test-team" -Headers $H).cards | Where-Object { $_.title -match 'Hello team' } | Select-Object -First 1).id
$body = @{ author = 'Lead'; text = $answer } | ConvertTo-Json
Invoke-RestMethod "$base/api/projects/test-team/cards/$card/comments" -Method Post -Headers $H -ContentType 'application/json' -Body $body | Out-Null
WaitFor ([regex]::Escape($answer)) "the Lead's answer '$answer'" 30
& $adb shell screencap -p /sdcard/smoke.png; & $adb pull /sdcard/smoke.png (Join-Path $env:TEMP 'smoke-board-pass.png') 2>$null | Out-Null
Write-Host "PASS: paired with a test board, message reached the board, the Lead's answer showed in the app (screen in TEMP\smoke-board-pass.png)"; Cleanup; exit 0
