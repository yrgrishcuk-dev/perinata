$ErrorActionPreference = "Stop"
. "$PSScriptRoot\dev-env.ps1"
$ProjectRoot = Split-Path -Parent $PSScriptRoot

Push-Location (Join-Path $ProjectRoot "web")
try {
    & npm install
    if ($LASTEXITCODE -ne 0) { throw "npm install завершился с ошибкой" }
    & npm run build
    if ($LASTEXITCODE -ne 0) { throw "npm run build завершился с ошибкой" }
} finally {
    Pop-Location
}

$static = Join-Path $ProjectRoot "backend\src\main\resources\static"
if (Test-Path $static) {
    Remove-Item -Recurse -Force $static
}
New-Item -ItemType Directory -Path $static | Out-Null
Copy-Item -Recurse -Force (Join-Path $ProjectRoot "web\dist\*") $static

Push-Location (Join-Path $ProjectRoot "backend")
try {
    & mvn -q test
    if ($LASTEXITCODE -ne 0) { throw "mvn test завершился с ошибкой" }
    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "mvn package завершился с ошибкой" }
} finally {
    Pop-Location
}

Write-Host "Сборка готова: backend\target\antenatal-0.1.0.jar"
