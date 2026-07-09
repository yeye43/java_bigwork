$ErrorActionPreference = "Stop"

$port = if ($args.Count -gt 0) { $args[0] } else { "8080" }
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolsDir = Join-Path $root ".tools"
$cloudflared = Join-Path $toolsDir "cloudflared.exe"

New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null

if (!(Test-Path $cloudflared)) {
    Write-Host "Downloading cloudflared for public demo URL..."
    $url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"
    Invoke-WebRequest -Uri $url -OutFile $cloudflared
}

Write-Host "Starting Jieqi web server..."
$server = Start-Process powershell `
    -ArgumentList @("-ExecutionPolicy", "Bypass", "-File", (Join-Path $root "web.ps1"), $port) `
    -WorkingDirectory $root `
    -PassThru `
    -WindowStyle Hidden

Start-Sleep -Seconds 3

try {
    $localUrl = "http://127.0.0.1:" + $port
    Write-Host ""
    Write-Host "The public demo URL will appear below, like https://xxxx.trycloudflare.com"
    Write-Host "Keep this window open while your teammates are using the demo. Press Ctrl+C to stop."
    Write-Host ""
    & $cloudflared tunnel --url $localUrl
} finally {
    if ($server -and !$server.HasExited) {
        Stop-Process -Id $server.Id -Force
    }
}
