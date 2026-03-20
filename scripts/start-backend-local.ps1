$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $projectRoot 'backend'
$javaHome = 'C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot'

if (-not (Test-Path $javaHome)) {
  throw "未找到 Java 17 安装目录: $javaHome"
}

Set-Location $backendDir
$env:JAVA_HOME = $javaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

& "$env:JAVA_HOME\bin\java.exe" -jar 'target\course-platform-demo-0.0.1-SNAPSHOT.jar' --spring.profiles.active=local
