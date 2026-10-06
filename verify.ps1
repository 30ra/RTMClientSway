param(
    [Parameter(Mandatory = $true)][string]$KaizJar,
    [string]$Cache = (Join-Path $env:USERPROFILE '.gradle\caches'),
    [string]$TuningFile
)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1') -Cache $Cache
$verify = Join-Path $PSScriptRoot 'build\verify'
New-Item -ItemType Directory -Force -Path $verify | Out-Null
$deps = @(Get-ChildItem -LiteralPath $Cache -Recurse -File -Filter '*.jar' | Where-Object {
    $_.Name -eq 'asm-debug-all-5.0.3.jar' -or $_.Name -eq 'launchwrapper-1.12.jar' -or
    $_.Name -like 'forgeBin-1.7.10-*.jar' -or $_.Name -eq 'guava-17.0.jar' -or
    $_.Name -eq 'log4j-api-2.0-beta9.jar' -or $_.Name -eq 'log4j-core-2.0-beta9.jar'
})
$cp = (@((Join-Path $PSScriptRoot 'build\classes')) + @($deps.FullName)) -join ';'
& javac -encoding UTF-8 -cp $cp -d $verify (Join-Path $PSScriptRoot 'tools\VerifyTransformer.java')
if ($LASTEXITCODE -ne 0) { throw 'verification compile failed' }
& java -cp "$verify;$cp" VerifyTransformer $KaizJar
if ($LASTEXITCODE -ne 0) { throw 'bytecode verification failed' }
& javac -encoding UTF-8 -source 8 -target 8 -cp $cp -d $verify (Join-Path $PSScriptRoot 'tools\VerifyMotion.java')
if ($LASTEXITCODE -ne 0) { throw 'motion test compile failed' }
& java -cp "$verify;$cp" rtmsway.VerifyMotion
if ($LASTEXITCODE -ne 0) { throw 'motion tests failed' }
& javac -encoding UTF-8 -source 8 -target 8 -cp $cp -d $verify (Join-Path $PSScriptRoot 'tools\VerifyTuning.java')
if ($LASTEXITCODE -ne 0) { throw 'tuning test compile failed' }
if ($TuningFile) { & java -cp "$verify;$cp" rtmsway.VerifyTuning $TuningFile }
else { & java -cp "$verify;$cp" rtmsway.VerifyTuning }
if ($LASTEXITCODE -ne 0) { throw 'tuning tests failed' }
& javac -encoding UTF-8 -source 8 -target 8 -cp $cp -d $verify (Join-Path $PSScriptRoot 'tools\VerifyTuningConfig.java')
if ($LASTEXITCODE -ne 0) { throw 'config test compile failed' }
& java -cp "$verify;$cp" rtmsway.VerifyTuningConfig
if ($LASTEXITCODE -ne 0) { throw 'config tests failed' }
