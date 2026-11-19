param([switch]$SkipBuild, [switch]$WithFrontend)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$runDirectory = Join-Path $projectRoot '.local-run'
New-Item -ItemType Directory -Force -Path $runDirectory | Out-Null
if (-not $SkipBuild) {
    & .\mvnw.cmd -B clean package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
}
& docker compose -p soa-lab2-local -f compose.local.yaml up -d --wait
if ($LASTEXITCODE -ne 0) { throw 'Local database did not start' }
$env:DB_URL = 'jdbc:postgresql://localhost:55432/space_marine'
$env:DB_USER = 'root'
$env:DB_PASSWORD = 'local-development'
$env:CONFIG_SERVER_URL = 'http://localhost:8888'
$env:EUREKA_URL = 'http://localhost:8761/eureka/'
$env:COLLECTION_URL = 'http://localhost:8081/api/v1'
$env:STARSHIP_URL = 'http://localhost:8083'
$env:STARSHIP_SOAP_URL = 'http://localhost:8082/services/starships'
$env:SPACE_MARINES_API_URL = 'http://localhost:18080/collection-service'
$env:STARSHIPS_API_URL = 'http://localhost:18080/starship-service'
$env:HTTP_ACCESS_LOG_DIRECTORY = $runDirectory
$env:SERVER_SSL_ENABLED = 'false'
$env:JAVA_TOOL_OPTIONS = '-Xmx256m'
function Wait-Http([string]$Url, [int]$Seconds = 150) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Service did not become ready: $Url. Inspect .local-run/*.err.log and *.out.log"
}
function Start-ServiceJar([string]$Name, [string]$Artifact, [string[]]$ExtraArguments, [string]$ReadyUrl) {
    $pidFile = Join-Path $runDirectory "$Name.pid"
    if (Test-Path $pidFile) {
        $oldProcess = Get-Process -Id ([int](Get-Content $pidFile)) -ErrorAction SilentlyContinue
        if ($oldProcess) { throw "$Name already has a live process; use scripts/stop-local.ps1 first" }
    }
    $artifactPath = Join-Path $projectRoot "$Name/target/$Artifact"
    $arguments = @('-jar', ('"' + $artifactPath + '"'), "--server.tomcat.accesslog.prefix=$Name") + $ExtraArguments
    $process = Start-Process java -ArgumentList $arguments -WindowStyle Hidden -WorkingDirectory $projectRoot -PassThru -RedirectStandardOutput (Join-Path $runDirectory "$Name.out.log") -RedirectStandardError (Join-Path $runDirectory "$Name.err.log")
    $process.Id | Set-Content $pidFile
    Wait-Http $ReadyUrl
    Write-Host "$Name ready: $ReadyUrl"
}
Start-ServiceJar 'config-server' 'config-server-1.0.0.jar' @('--server.port=8888') 'http://localhost:8888/collection-service/default'
Start-ServiceJar 'eureka-server' 'eureka-server-1.0.0.jar' @('--server.port=8761') 'http://localhost:8761/'
Start-ServiceJar 'collection-service' 'collection-service-1.0.0.war' @('--server.port=8081') 'http://localhost:8081/api/v1/space-marines'
Start-ServiceJar 'starship-service' 'starship-service-1.0.0.jar' @('--server.port=8082') 'http://localhost:8082/services/starships?wsdl'
Start-ServiceJar 'starship-rest-adapter' 'starship-rest-adapter-1.0.0.jar' @('--server.port=8083') 'http://localhost:8083/api/v1/starships'
# Static collection URL avoids waiting for the Eureka client registry refresh locally.
Start-ServiceJar 'api-gateway' 'api-gateway-1.0.0.jar' @('--server.port=18080', '--zuul.routes.collection-api.url=http://localhost:8081', '--zuul.routes.collection-resources.url=http://localhost:8081') 'http://localhost:18080/api/starship-service/starships'
if ($WithFrontend) {
    $frontendRoot = Join-Path $projectRoot 'soa-front'
    if (-not (Test-Path (Join-Path $frontendRoot 'node_modules/.bin/rsbuild.cmd'))) {
        throw 'Install frontend dependencies first: cd soa-front; npx pnpm install --frozen-lockfile'
    }
    if (-not $SkipBuild -or -not (Test-Path (Join-Path $frontendRoot 'dist/server/index.js'))) {
        Push-Location $frontendRoot
        try {
            & .\node_modules\.bin\rsbuild.cmd build
            if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
        } finally { Pop-Location }
    }
    $env:PORT = '3000'
    $env:HOST = '127.0.0.1'
    $srvxPath = Join-Path $frontendRoot 'node_modules/srvx/bin/srvx.mjs'
    $frontend = Start-Process node -ArgumentList @(('"' + $srvxPath + '"'), '--prod', '-s', '../client', 'dist/server/index.js') -WindowStyle Hidden -WorkingDirectory $frontendRoot -PassThru -RedirectStandardOutput (Join-Path $runDirectory 'frontend.out.log') -RedirectStandardError (Join-Path $runDirectory 'frontend.err.log')
    $frontend.Id | Set-Content (Join-Path $runDirectory 'frontend.pid')
    Wait-Http 'http://localhost:3000/'
    Write-Host 'Frontend ready: http://localhost:3000/'
}
Write-Host 'REST: http://localhost:18080/api/starship-service/starships'
Write-Host 'WSDL: http://localhost:8082/services/starships?wsdl'
