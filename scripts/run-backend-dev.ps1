$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $projectRoot 'backend'
$javaHome = 'C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot'
$mavenHome = Join-Path $projectRoot '.tools\apache-maven-3.9.11'
$mavenRepo = Join-Path $projectRoot '.m2\repository'
$appMainClass = 'com.demo.courseplatform.CoursePlatformApplication'

if (-not (Test-Path $javaHome)) {
  throw "未找到 Java 17 安装目录: $javaHome"
}

if (-not (Test-Path $mavenHome)) {
  throw "未找到 Maven 安装目录: $mavenHome"
}

Set-Location $backendDir
$null = New-Item -ItemType Directory -Force -Path $mavenRepo
$env:JAVA_HOME = $javaHome
$env:MAVEN_HOME = $mavenHome
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

$existingProcesses = Get-CimInstance Win32_Process |
  Where-Object {
    $_.Name -eq 'java.exe' -and
    $_.CommandLine -and
    $_.CommandLine.Contains($appMainClass)
  }

if ($existingProcesses) {
  $existingProcesses | ForEach-Object {
    Write-Host "Stopping existing backend process: PID=$($_.ProcessId)"
    Stop-Process -Id $_.ProcessId -Force
  }
  Start-Sleep -Seconds 2
}

& "$env:MAVEN_HOME\bin\mvn.cmd" "-Dmaven.repo.local=$mavenRepo" spring-boot:run "-Dspring-boot.run.profiles=local"
