Set-Location -Path "$PSScriptRoot"
$ErrorActionPreference = 'Stop'
if (Get-Command gradle -ErrorAction SilentlyContinue) {
    Write-Host 'Running: gradle run'
    gradle run
    exit $LASTEXITCODE
}
Write-Host 'Compiling with javac (UTF-8) and running main class...'
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
$files = Get-ChildItem -Path src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $files
java -cp out com.gestiontaches.Application
