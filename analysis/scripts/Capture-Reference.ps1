param([Parameter(Mandatory)][string]$Name, [string]$Serial='emulator-5554')
$ErrorActionPreference='Stop'
if ($Name -notmatch '^[a-zA-Z0-9-]+$') { throw 'Use a simple capture name.' }
$adbTask='C:\Users\mayur\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$outTask=Join-Path $PSScriptRoot "..\evidence\$Name.xml"
$remoteTask="/sdcard/rea-$Name-$([Guid]::NewGuid().ToString('N')).xml"
$resultTask=& $adbTask -s $Serial shell uiautomator dump $remoteTask 2>&1
if (($resultTask -join "`n") -notmatch 'dumped to:') { throw "Capture failed; no current hierarchy: $resultTask" }
& $adbTask -s $Serial pull $remoteTask $outTask | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Pull failed' }
[xml]$xmlTask=Get-Content -LiteralPath $outTask -Raw
$xmlTask.SelectNodes('//node') | Where-Object { $_.text -or $_.'content-desc' } | ForEach-Object { '{0} | {1} | {2}' -f $_.text,$_.'content-desc',$_.bounds }
