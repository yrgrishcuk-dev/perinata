$ErrorActionPreference = "Stop"
. "$PSScriptRoot\dev-env.ps1"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $ProjectRoot "backend\target\antenatal-0.1.0.jar"

if (-not (Test-Path $jar)) {
    throw "Jar не найден. Сначала выполните .\scripts\build-dev.ps1"
}

& "$env:PG_BIN\pg_isready.exe" -h 127.0.0.1 -p 5432 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Запускаю PostgreSQL..."
    & "$env:PG_BIN\pg_ctl.exe" -D $env:PGDATA -l (Join-Path $env:PGDATA "server.log") -o "-p 5432" start
    Start-Sleep -Seconds 2
}

$env:DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/antenatal"
$env:DATABASE_USER = "antenatal"
$env:DATABASE_PASSWORD = "antenatal"
$env:DEV_MODE = "true"
$env:PUBLIC_BASE_URL = "http://localhost:8080"

Write-Host "Приложение: http://localhost:8080/app"
Write-Host "Кабинет:    http://localhost:8080/cabinet/login"
Write-Host "Демо-врач:  doctor@example.com / doctor123"
Write-Host "Демо-админ: admin@example.com / admin123456"

& "$env:JAVA_HOME\bin\java.exe" -jar $jar
