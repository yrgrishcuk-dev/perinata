$ProjectRoot = Split-Path -Parent $PSScriptRoot
$WorkspaceRoot = Split-Path -Parent $ProjectRoot
$ToolsRoot = Join-Path $WorkspaceRoot ".tools"
$DataRoot = Join-Path $WorkspaceRoot ".data"

$env:JAVA_HOME = Join-Path $ToolsRoot "jdk21\jdk-21.0.12.1+1"
$env:MAVEN_HOME = Join-Path $ToolsRoot "maven\apache-maven-3.9.16"
$env:NODE_HOME = Join-Path $ToolsRoot "node\node-v20.18.0-win-x64"
$env:PG_BIN = Join-Path $ToolsRoot "pgsql\pgsql\bin"
$env:PGDATA = Join-Path $DataRoot "pg"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:NODE_HOME;$env:PG_BIN;" + $env:PATH

Write-Host "JAVA_HOME = $env:JAVA_HOME"
Write-Host "MAVEN_HOME = $env:MAVEN_HOME"
Write-Host "NODE_HOME = $env:NODE_HOME"
Write-Host "PG_BIN = $env:PG_BIN"
