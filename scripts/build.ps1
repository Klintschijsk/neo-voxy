param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('integrations-1.21.1', 'client-1.21.1', 'client-1.20.1', 'client-26.1.2')]
    [string]$Edition
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$targets = @{
    'integrations-1.21.1' = @{
        Path = $repo
        Java = 21
        Jar = 'neo-voxy-0.4.0-beta-1-mc1.21.1-neoforge-integrations.jar'
    }
    'client-1.21.1' = @{
        Path = Join-Path $repo 'editions/neoforge-1.21.1-client'
        Java = 21
        Jar = 'neo-voxy-0.2.18-beta-mc1.21.1-neoforge-client.jar'
    }
    'client-1.20.1' = @{
        Path = Join-Path $repo 'editions/forge-1.20.1-client'
        Java = 17
        Jar = 'neo-voxy-0.3.3-1.20.1-alpha.1-forge-client.jar'
    }
    'client-26.1.2' = @{
        Path = Join-Path $repo 'editions/neoforge-26.1.2-client'
        Java = 25
        Jar = 'neo-voxy-0.2.18-beta-mc26.1.2-neoforge-client.jar'
    }
}

$target = $targets[$Edition]
$javaVariable = "JAVA_HOME_$($target.Java)"
$javaHome = [Environment]::GetEnvironmentVariable($javaVariable)
if (-not $javaHome) {
    $workspaceJdk = "D:\Java\$($target.Java)"
    if (Test-Path -LiteralPath (Join-Path $workspaceJdk 'bin/java.exe')) {
        $javaHome = $workspaceJdk
    }
}
if (-not $javaHome) {
    $currentJava = & java -version 2>&1 | Select-Object -First 1
    $javaPattern = 'version "?{0}(?:[."]|$)' -f $target.Java
    if ($currentJava -notmatch $javaPattern) {
        throw "JDK $($target.Java) is required. Set $javaVariable before building $Edition."
    }
}

$oldJavaHome = $env:JAVA_HOME
$oldPath = $env:Path
$oldGradleUserHome = $env:GRADLE_USER_HOME
try {
    if ($javaHome) {
        $env:JAVA_HOME = $javaHome
        $env:Path = "$(Join-Path $javaHome 'bin');$oldPath"
    }
    if (-not $env:GRADLE_USER_HOME) {
        $sharedGradleHome = Join-Path (Split-Path -Parent $repo) '.gradle-user-home'
        if (Test-Path -LiteralPath $sharedGradleHome) {
            $env:GRADLE_USER_HOME = $sharedGradleHome
        }
    }

    $gradleLauncher = Join-Path $target.Path 'gradlew.bat'
    if ($env:GRADLE_USER_HOME) {
        $wrapperProperties = Get-Content -LiteralPath (Join-Path $target.Path 'gradle/wrapper/gradle-wrapper.properties')
        $distributionUrl = ($wrapperProperties | Where-Object { $_ -like 'distributionUrl=*' }) -replace '^distributionUrl=', ''
        $distributionName = [System.IO.Path]::GetFileNameWithoutExtension($distributionUrl)
        $cachedLauncher = Get-ChildItem -LiteralPath (Join-Path $env:GRADLE_USER_HOME "wrapper/dists/$distributionName") `
            -Filter 'gradle.bat' -File -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.DirectoryName -like '*\bin' } |
            Select-Object -First 1
        if ($cachedLauncher) {
            $gradleLauncher = $cachedLauncher.FullName
        }
    }

    Push-Location $target.Path
    try {
        & $gradleLauncher -I init.gradle clean build --no-daemon --no-configuration-cache
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle failed for $Edition with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }

    $source = Join-Path $target.Path "build/libs/$($target.Jar)"
    if (-not (Test-Path -LiteralPath $source)) {
        throw "Expected release JAR was not generated: $source"
    }
    $dist = Join-Path $repo 'dist'
    New-Item -ItemType Directory -Force -Path $dist | Out-Null
    Copy-Item -LiteralPath $source -Destination (Join-Path $dist $target.Jar) -Force
    Get-Item -LiteralPath (Join-Path $dist $target.Jar) |
        Select-Object FullName, Length, LastWriteTime
} finally {
    $env:JAVA_HOME = $oldJavaHome
    $env:Path = $oldPath
    $env:GRADLE_USER_HOME = $oldGradleUserHome
}
