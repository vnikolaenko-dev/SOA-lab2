param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$outputDir = Join-Path $projectRoot 'api-gateway/target/live-verification'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$processes = [Collections.Generic.List[System.Diagnostics.Process]]::new()
$checks = [Collections.Generic.List[object]]::new()
$containerName = 'lab2-zuul-check-' + [Guid]::NewGuid().ToString('N').Substring(0, 10)
$containerCreated = $false
$javaExecutable = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    Join-Path $env:JAVA_HOME 'bin/java.exe'
} else {
    throw 'Set JAVA_HOME to a JDK 21 directory (use the real java.exe, not an Oracle javapath launcher).'
}
function Free-Port {
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $number = $listener.LocalEndpoint.Port
    $listener.Stop()
    return $number
}
function Start-App([string]$module, [string]$artifact, [int]$port, [string[]]$extra = @()) {
    $arguments = @('-Xmx256m', '-jar', ('"' + (Join-Path $projectRoot "$module/target/$artifact") + '"'),
        "--server.port=$port", '--eureka.instance.ip-address=127.0.0.1',
        "--eureka.instance.instance-id=${module}:$port", '--eureka.client.registry-fetch-interval-seconds=2',
        '--eureka.client.initial-instance-info-replication-interval-seconds=1') + $extra
    $process = Start-Process -FilePath $javaExecutable -ArgumentList $arguments -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $outputDir "$module-$port.out.log") `
        -RedirectStandardError (Join-Path $outputDir "$module-$port.err.log")
    $processes.Add($process)
    return $process
}
function Wait-Url([string]$url, [int]$seconds = 120) {
    $until = [DateTime]::UtcNow.AddSeconds($seconds)
    while ([DateTime]::UtcNow -lt $until) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 4
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        foreach ($process in $processes) {
            if ($process.HasExited) { throw "Java process $($process.Id) exited. Inspect $outputDir" }
        }
        Start-Sleep -Milliseconds 750
    }
    throw "Timed out waiting for $url. Inspect $outputDir"
}
function Check([string]$name, [bool]$condition) {
    if (-not $condition) { throw "FAILED: $name" }
    $checks.Add([pscustomobject]@{ name = $name; result = 'passed' })
    Write-Output "PASS: $name"
}
function Request([string]$url, [string]$method = 'GET', [string]$body = '') {
    $options = @{ Uri = $url; Method = $method; UseBasicParsing = $true; TimeoutSec = 25 }
    if ($body) { $options.Body = $body; $options.ContentType = 'application/json' }
    try { return Invoke-WebRequest @options } catch {
        if ($_.Exception.Response) { return $_.Exception.Response }
        throw
    }
}
try {
    if (-not $SkipBuild) {
        & (Join-Path $projectRoot 'mvnw.cmd') -B verify
        if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed' }
    }
    & docker run -d --rm --name $containerName -e POSTGRES_DB=space_marine -e POSTGRES_USER=lab2 `
        -e POSTGRES_PASSWORD=lab2-local-verification -p '127.0.0.1::5432' postgres:16-alpine | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not start verification PostgreSQL' }
    $containerCreated = $true
    $binding = (& docker port $containerName '5432/tcp').Trim()
    $dbPort = [int]($binding.Split(':')[-1])
    $env:DB_URL = "jdbc:postgresql://localhost:$dbPort/space_marine"
    $env:DB_USER = 'lab2'
    $env:DB_PASSWORD = 'lab2-local-verification'
    $configPort = Free-Port
    $discoveryPort = Free-Port
    $gatewayPort = Free-Port
    $collectionPort = Free-Port
    $starshipPort = Free-Port
    $env:CONFIG_SERVER_URL = "http://localhost:$configPort"
    $env:EUREKA_URL = "http://localhost:$discoveryPort/eureka/"
    Start-App 'config-server' 'config-server-1.0.0.jar' $configPort | Out-Null
    Wait-Url "$env:CONFIG_SERVER_URL/api-gateway/default"
    $remoteConfig = Invoke-RestMethod "$env:CONFIG_SERVER_URL/starship-service/default"
    Check 'Config Server supplies gateway URL to Starship' `
        ($remoteConfig.propertySources[0].source.'collection.base-url' -eq 'http://api-gateway/api/collection-service')
    Start-App 'eureka-server' 'eureka-server-1.0.0.jar' $discoveryPort `
        @('--eureka.server.response-cache-update-interval-ms=1000', '--eureka.server.use-read-only-response-cache=false') | Out-Null
    Wait-Url "http://localhost:$discoveryPort/"
    Start-App 'api-gateway' 'api-gateway-1.0.0.jar' $gatewayPort `
        @('--ribbon.ServerListRefreshInterval=1000') | Out-Null
    Start-App 'collection-service' 'collection-service-1.0.0.war' $collectionPort | Out-Null
    Start-App 'starship-service' 'starship-service-1.0.0.war' $starshipPort | Out-Null
    $gateway = "http://localhost:$gatewayPort"
    Wait-Url "$gateway/collection-service/openapi.yaml"
    Wait-Url "$gateway/starship-service/openapi.yaml"
    $registry = Invoke-RestMethod -Headers @{Accept='application/json'} ($env:EUREKA_URL.TrimEnd('/') + '/apps')
    $registered = @($registry.applications.application | ForEach-Object { $_.name })
    Check 'Eureka contains gateway and both business services' `
        (($registered -contains 'API-GATEWAY') -and ($registered -contains 'COLLECTION-SERVICE') -and ($registered -contains 'STARSHIP-SERVICE'))
    $collectionApi = "$gateway/api/collection-service"
    $starshipApi = "$gateway/api/starship-service"
    Wait-Url "$collectionApi/space-marines"
    Wait-Url "$starshipApi/starships"
    $body = '{"name":"Gateway check","coordinates":{"x":1.5,"y":10},"health":100,"weaponType":"COMBI_FLAMER","chapter":{"name":"Ultramarines","world":"Macragge"}}'
    $created = Request "$collectionApi/space-marines" 'POST' $body
    Check 'POST collection through Zuul returns 201' ([int]$created.StatusCode -eq 201)
    $marine = $created.Content | ConvertFrom-Json
    $marineId = $marine.id
    $location = [string]$created.Headers['Location']
    Check 'Location header points to public gateway path' ($location -eq "/api/collection-service/space-marines/$marineId")
    $patched = Request "$collectionApi/space-marines/$marineId" 'PATCH' '{"health":80,"meleeWeapon":null}'
    Check 'PATCH body reaches collection unchanged' `
        (([int]$patched.StatusCode -eq 200) -and (($patched.Content | ConvertFrom-Json).health -eq 80))
    $filtered = Request "$collectionApi/space-marines?page=0&size=10&filter=health,gt,50&sort=health,desc"
    Check 'Pagination filter and sort survive gateway' `
        (([int]$filtered.StatusCode -eq 200) -and (($filtered.Content | ConvertFrom-Json).totalElements -eq 1))
    $loaded = Request "$starshipApi/starship/1/load/$marineId" 'POST'
    Check 'Starship -> Eureka/Ribbon -> Zuul -> Collection works' `
        (([int]$loaded.StatusCode -eq 200) -and (($loaded.Content | ConvertFrom-Json).id -eq $marineId))
    $duplicate = Request "$starshipApi/starship/1/load/$marineId" 'POST'
    Check 'Conflict remains HTTP 409 through Zuul' ([int]$duplicate.StatusCode -eq 409)
    $crew = Request "$starshipApi/starship/1/space-marines?page=0&size=10&filter=health,gt,50"
    Check 'Crew query performs nested calls through gateway' `
        (([int]$crew.StatusCode -eq 200) -and (($crew.Content | ConvertFrom-Json).totalElements -eq 1))
    foreach ($service in @('collection-service','starship-service')) {
        $ui = Request "$gateway/$service/swagger-ui/index.html"
        Check "$service Swagger UI is available through gateway" ([int]$ui.StatusCode -eq 200)
        $swaggerConfig = Request "$gateway/$service/v3/api-docs/swagger-config"
        $swaggerConfig.Content | Set-Content -Encoding UTF8 (Join-Path $outputDir "$service-swagger-config.json")
        Check "$service Swagger config uses public OpenAPI URL" `
            (([int]$swaggerConfig.StatusCode -eq 200) -and (($swaggerConfig.Content | ConvertFrom-Json).url -eq "/$service/openapi.yaml"))
    }
    Check 'Unknown gateway route returns 404' ([int](Request "$gateway/api/unknown/space-marines").StatusCode -eq 404)
    Check 'Unload works through gateway' ([int](Request "$starshipApi/starship/1/unload-all" 'POST').StatusCode -eq 200)
    Check 'DELETE collection returns 204' ([int](Request "$collectionApi/space-marines/$marineId" 'DELETE').StatusCode -eq 204)
    [pscustomobject]@{ checkedAt = (Get-Date).ToString('o'); scope = 'real Config, Eureka, Zuul, business services and PostgreSQL'; checks = $checks } `
        | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 (Join-Path $outputDir 'results.json')
    Write-Output "Results: $outputDir/results.json"
} finally {
    foreach ($process in $processes) {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force }
    }
    if ($containerCreated) { & docker stop $containerName | Out-Null }
}
