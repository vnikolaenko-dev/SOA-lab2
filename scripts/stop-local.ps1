$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runDirectory = Join-Path $projectRoot '.local-run'
if (Test-Path $runDirectory) {
    foreach ($pidFile in Get-ChildItem -LiteralPath $runDirectory -Filter '*.pid') {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$([int](Get-Content $pidFile.FullName))" -ErrorAction SilentlyContinue
        # Only stop a recorded process whose command line belongs to this project.
        if ($process -and $process.CommandLine.Contains($projectRoot) -and ($process.CommandLine -like '*-1.0.0.jar*' -or $process.CommandLine -like '*collection-service-1.0.0.war*' -or $process.CommandLine -like '*srvx.mjs*')) {
            & taskkill /PID $process.ProcessId /T /F
        }
        Remove-Item -LiteralPath $pidFile.FullName
    }
}
Set-Location $projectRoot
& docker compose -p soa-lab2-local -f compose.local.yaml stop
