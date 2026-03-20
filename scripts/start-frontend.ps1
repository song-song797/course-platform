$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $projectRoot 'frontend'

Set-Location $frontendDir
& npm.cmd run dev -- --host 0.0.0.0 --port 5173
