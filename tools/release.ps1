# AII-23: build the signed release APK for Rich's phone, and (with -Serve) share it over Tailscale so the phone can
# install it from anywhere, not just on home Wi-Fi.
#
#   powershell -ExecutionPolicy Bypass -File tools\release.ps1          # build, copy to dist\, print the file and SHA-256
#   powershell -ExecutionPolicy Bypass -File tools\release.ps1 -Serve   # same, then share dist\ at https://<this PC>.ts.net/messenger/
#   powershell -ExecutionPolicy Bypass -File tools\release.ps1 -Stop    # stop sharing
#
# Signed with keystore\messenger.jks (kept out of git; see app\build.gradle.kts). Without it the build uses the debug key
# and the script stops: an APK signed with another key can't update the one on the phone.
param([switch]$Serve, [switch]$Stop)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$tailscale = 'C:\Program Files\Tailscale\tailscale.exe'
$path = '/messenger'

if ($Stop) {
    & $tailscale serve --https=443 --set-path=$path off
    Write-Host "Stopped sharing $path"
    exit 0
}

if (-not (Test-Path (Join-Path $root 'keystore\messenger.jks'))) {
    Write-Host 'No keystore\messenger.jks: copy it from the main AI-gile Android folder first.' -ForegroundColor Red
    exit 1
}
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = Join-Path $env:LOCALAPPDATA 'AndroidTools\jdk17' }

Push-Location $root
try {
    & .\gradlew.bat -q :app:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Build failed (exit $LASTEXITCODE)" }
} finally { Pop-Location }

$apk = Join-Path $root 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path $apk)) { throw "No APK at $apk" }
$version = (Select-String -Path (Join-Path $root 'app\build.gradle.kts') -Pattern 'versionName = "([^"]+)"').Matches[0].Groups[1].Value

$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Force $dist | Out-Null
$named = Join-Path $dist "aigile-messenger-$version.apk"
Copy-Item $apk $named -Force
Copy-Item $apk (Join-Path $dist 'aigile-messenger.apk') -Force
$sha = (Get-FileHash $named -Algorithm SHA256).Hash.ToLower()
$mb = [math]::Round((Get-Item $named).Length / 1MB, 1)
Write-Host "Built $named ($mb MB)"
Write-Host "SHA-256 $sha"

if ($Serve) {
    & $tailscale serve --bg --https=443 --set-path=$path $dist
    if ($LASTEXITCODE -ne 0) { throw 'tailscale serve failed: is HTTPS turned on for your tailnet (admin console, DNS page)?' }
    $dns = ((& $tailscale status --json | ConvertFrom-Json).Self.DNSName).TrimEnd('.')
    Write-Host ''
    Write-Host "On the phone (Tailscale on), open: https://$dns$path/aigile-messenger.apk" -ForegroundColor Green
}
