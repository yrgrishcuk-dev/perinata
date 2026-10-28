$ErrorActionPreference = "Stop"
. "$PSScriptRoot\dev-env.ps1"

$secrets = Join-Path $PSScriptRoot "local.secrets.ps1"
if (-not (Test-Path $secrets)) {
    throw "Create scripts\local.secrets.ps1 from local.secrets.example.ps1"
}
. $secrets

if (-not $env:TELEGRAM_BOT_TOKEN -or $env:TELEGRAM_BOT_TOKEN -like "123456789:*") {
    throw "Fill TELEGRAM_BOT_TOKEN in scripts\local.secrets.ps1"
}
if (-not $env:TELEGRAM_BOT_USERNAME) {
    throw "Fill TELEGRAM_BOT_USERNAME in scripts\local.secrets.ps1"
}
if (-not $env:TELEGRAM_WEBHOOK_SECRET) {
    throw "Fill TELEGRAM_WEBHOOK_SECRET in scripts\local.secrets.ps1"
}
if (-not $env:PUBLIC_BASE_URL) {
    throw "Set PUBLIC_BASE_URL to the HTTPS tunnel URL"
}
if (-not $env:PUBLIC_BASE_URL.StartsWith("https://")) {
    throw "PUBLIC_BASE_URL must start with https://"
}

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $ProjectRoot "backend\target\antenatal-0.1.0.jar"
if (-not (Test-Path $jar)) {
    throw "Jar not found. Run build-dev.ps1 first."
}

$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    throw "Port 8080 is busy. Stop the current app first."
}

& "$env:PG_BIN\pg_isready.exe" -h 127.0.0.1 -p 5432 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Starting PostgreSQL..."
    & "$env:PG_BIN\pg_ctl.exe" -D $env:PGDATA -l (Join-Path $env:PGDATA "server.log") -o "-p 5432" start
    Start-Sleep -Seconds 2
}

$env:DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/antenatal"
$env:DATABASE_USER = "antenatal"
$env:DATABASE_PASSWORD = "antenatal"
$env:DEV_MODE = "false"

Write-Host "Public URL: $env:PUBLIC_BASE_URL"
Write-Host "Telegram bot: @$env:TELEGRAM_BOT_USERNAME"
Write-Host "DEV_MODE=false"
Write-Host "App:     $env:PUBLIC_BASE_URL/app"
Write-Host "Cabinet: $env:PUBLIC_BASE_URL/cabinet/login"

$javaArgs = @("-Djava.net.useSystemProxies=true")
$proxySettings = Get-ItemProperty -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings" -ErrorAction SilentlyContinue
$proxyServer = $null
if ($proxySettings -and $proxySettings.ProxyEnable -eq 1) {
    $proxyServer = $proxySettings.ProxyServer
}
if ($proxyServer) {
    $proxyUri = [uri]$proxyServer
    $javaArgs += "-Dhttps.proxyHost=$($proxyUri.Host)"
    $javaArgs += "-Dhttps.proxyPort=$($proxyUri.Port)"
    $javaArgs += "-Dhttp.proxyHost=$($proxyUri.Host)"
    $javaArgs += "-Dhttp.proxyPort=$($proxyUri.Port)"
    $javaArgs += "-Dhttp.nonProxyHosts=localhost|127.0.0.1"
    Write-Host "Using system proxy: $($proxyUri.Host):$($proxyUri.Port)"
}
$javaArgs += @("-jar", $jar)

& "$env:JAVA_HOME\bin\java.exe" @javaArgs
