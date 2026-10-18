param([switch]$Device)
$ErrorActionPreference='Stop'
$projectTask=Split-Path $PSScriptRoot -Parent
$previousJavaTask=$env:JAVA_HOME
try {
    if(Test-Path -LiteralPath 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe') { $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' }
    Push-Location -LiteralPath $projectTask
    $tasksTask=@(':app:testDebugUnitTest',':app:lintDebug',':app:assembleDebug')
    if($Device) {$tasksTask+=':app:connectedDebugAndroidTest'}
    & '.\gradlew.bat' @tasksTask --console=plain
    if($LASTEXITCODE -ne 0) {throw "Verification failed with exit code $LASTEXITCODE"}
} finally {Pop-Location;$env:JAVA_HOME=$previousJavaTask}
