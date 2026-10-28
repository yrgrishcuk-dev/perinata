$ErrorActionPreference = "Stop"
. "$PSScriptRoot\dev-env.ps1"
& "$env:PG_BIN\pg_ctl.exe" -D $env:PGDATA stop
