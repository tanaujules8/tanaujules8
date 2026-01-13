Set-Location -Path "$PSScriptRoot"
$ErrorActionPreference = 'Stop'
Write-Host 'Compiling sources (UTF-8)...'
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
$files = Get-ChildItem -Path src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $files
Write-Host 'Starting WebServer in background on port 8080...'
Start-Process -FilePath java -ArgumentList '-cp','out','com.gestiontaches.web.WebServer' -NoNewWindow
Write-Host 'Server started. Open http://localhost:8080 in your browser.'
