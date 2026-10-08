# Compile and run Swift (JavaFX media player)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$javafxHome = $env:JAVAFX_HOME
if ([string]::IsNullOrWhiteSpace($javafxHome)) {
    $javafxHome = Join-Path $PSScriptRoot "javafx-sdk-25"
}

$javafx = Join-Path $javafxHome "lib"
if (-not (Test-Path (Join-Path $javafx "javafx.controls.jar"))) {
    throw "JavaFX SDK not found under '$javafxHome'. JAVAFX_HOME must contain lib\javafx.controls.jar."
}

$out = "out"

if (-not (Test-Path $out)) { New-Item -ItemType Directory $out | Out-Null }

Write-Host "Compiling..."
$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
& javac --module-path $javafx --add-modules javafx.controls,javafx.media -d $out $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

New-Item -ItemType Directory -Force "$out\com\swift\resources" | Out-Null
Copy-Item "src\com\swift\resources\*" "$out\com\swift\resources\" -Recurse -Force

Write-Host "Starting Swift..."
& java "-Djava.library.path=$(Join-Path $javafxHome 'bin')" --module-path "$javafx;$out" --add-modules javafx.controls,javafx.media -m swift/com.swift.SwiftApp
exit $LASTEXITCODE
