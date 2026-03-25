$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $projectRoot 'frontend'
$packageJsonPath = Join-Path $frontendDir 'package.json'

if (-not (Test-Path $packageJsonPath)) {
    $nestedFrontendDir = Join-Path $frontendDir 'frontend'
    $nestedPackageJsonPath = Join-Path $nestedFrontendDir 'package.json'

    if (Test-Path $nestedPackageJsonPath) {
        $frontendDir = $nestedFrontendDir
        $packageJsonPath = $nestedPackageJsonPath
    }
}

if (-not (Test-Path $packageJsonPath)) {
    throw "Frontend package.json was not found. Checked: '$packageJsonPath' and '$(Join-Path $frontendDir 'frontend\\package.json')'"
}

Set-Location $frontendDir
& npm.cmd run dev -- --host 0.0.0.0 --port 5173
