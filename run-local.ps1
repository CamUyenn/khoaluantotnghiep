param(
    [switch]$SkipFrontendInstall,
    [switch]$ChecklistOnly
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ("[run-local] {0}" -f $Message)
}

function Write-Check {
    param(
        [bool]$Ok,
        [string]$Message
    )

    if ($Ok) {
        Write-Host ("[x] {0}" -f $Message) -ForegroundColor Green
    }
    else {
        Write-Host ("[ ] {0}" -f $Message) -ForegroundColor Yellow
    }
}

function Test-CommandExists {
    param([string]$CommandName)
    return $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Get-NodeVersion {
    try {
        $raw = (& node -v).Trim()
        if ($raw.StartsWith("v")) {
            $raw = $raw.Substring(1)
        }
        return [Version]$raw
    }
    catch {
        return $null
    }
}

function Test-PortListening {
    param([int]$Port)

    try {
        $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
        Select-Object -First 1
        return $null -ne $conn
    }
    catch {
        $line = netstat -ano -p tcp | Select-String -Pattern (":{0}\s+.*LISTENING\s+\d+$" -f $Port) |
        Select-Object -First 1
        return $null -ne $line
    }
}

function ConvertTo-PowerShellLiteral {
    param([string]$Value)
    return "'" + $Value.Replace("'", "''") + "'"
}

function Import-DotEnv {
    param([string]$EnvPath)

    if (-not (Test-Path $EnvPath)) {
        return
    }

    $lines = Get-Content -Path $EnvPath -ErrorAction SilentlyContinue
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $parts = $trimmed.Split("=", 2)
        if ($parts.Count -lt 2) {
            continue
        }

        $key = $parts[0].Trim()
        $value = $parts[1]

        if ($value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        if ($value.StartsWith("'") -and $value.EndsWith("'")) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if ($key) {
            Set-Item -Path ("env:{0}" -f $key) -Value $value
        }
    }
}

$repoRoot = (Resolve-Path $PSScriptRoot).Path
$backendPath = Join-Path $repoRoot "Backend"
$frontendPath = Join-Path $repoRoot "Frontend"

$startRedisCmd = Join-Path $backendPath "scripts\start-redis.cmd"
$stopRedisCmd = Join-Path $backendPath "scripts\stop-redis.cmd"
$mvnwCmd = Join-Path $backendPath "mvnw.cmd"
$frontendPackage = Join-Path $frontendPath "package.json"
$frontendNodeModules = Join-Path $frontendPath "node_modules"

$backendEnvPath = Join-Path $backendPath ".env"
Import-DotEnv -EnvPath $backendEnvPath

if (-not (Test-Path $backendPath)) {
    throw "Cannot find Backend folder. Expected path: $backendPath"
}

if (-not (Test-Path $frontendPath)) {
    throw "Cannot find Frontend folder. Expected path: $frontendPath"
}

if (-not (Test-Path $startRedisCmd)) {
    throw "Cannot find start Redis script: $startRedisCmd"
}

if (-not (Test-Path $mvnwCmd)) {
    throw "Cannot find Maven wrapper: $mvnwCmd"
}

if (-not (Test-Path $frontendPackage)) {
    throw "Cannot find frontend package.json: $frontendPackage"
}

$hasJava = Test-CommandExists -CommandName "java"
$hasNode = Test-CommandExists -CommandName "node"
$hasNpm = Test-CommandExists -CommandName "npm"
$minimumNodeVersion = [Version]"18.18.0"
$nodeVersion = if ($hasNode) { Get-NodeVersion } else { $null }
$isSupportedNodeVersion = $null -ne $nodeVersion -and $nodeVersion -ge $minimumNodeVersion
$nodeVersionText = if ($null -ne $nodeVersion) { $nodeVersion.ToString() } else { "unknown" }
$isPostgresListening = Test-PortListening -Port 5432

Write-Host ""
Write-Host "==== FULL LOCAL CHECKLIST ====" -ForegroundColor Cyan
Write-Check -Ok $hasJava -Message "Java is available in PATH"
Write-Check -Ok $hasNode -Message "Node.js is available in PATH"
Write-Check -Ok $isSupportedNodeVersion -Message ("Node.js version >= {0} (current: {1})" -f $minimumNodeVersion, $nodeVersionText)
Write-Check -Ok $hasNpm -Message "npm is available in PATH"
Write-Check -Ok $isPostgresListening -Message "PostgreSQL appears to be listening on port 5432"
Write-Check -Ok (Test-Path $startRedisCmd) -Message "Redis start script exists"
Write-Check -Ok (Test-Path $mvnwCmd) -Message "Backend Maven wrapper exists"
Write-Check -Ok (Test-Path $frontendPackage) -Message "Frontend package.json exists"
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

if (-not $hasJava -or -not $hasNode -or -not $hasNpm) {
    throw "Missing required tools in PATH. Please install Java + Node.js and retry."
}

if (-not $isSupportedNodeVersion) {
    throw ("Node.js {0} is not compatible. Frontend requires Node.js >= {1}. Please upgrade Node.js and retry." -f $nodeVersionText, $minimumNodeVersion)
}

if (-not $isPostgresListening) {
    Write-Host "Warning: PostgreSQL is not detected on port 5432. Backend may fail if DB is unavailable." -ForegroundColor Yellow
}

if ($ChecklistOnly) {
    Write-Step "Checklist only mode completed."
    exit 0
}

Write-Step "Starting Redis on port 6379..."
$redisStart = Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", ('"{0}"' -f $startRedisCmd)) -WorkingDirectory $backendPath -PassThru -Wait -NoNewWindow
if ($redisStart.ExitCode -ne 0) {
    throw "Redis startup failed. Check Backend/scripts/start-redis.ps1 output."
}

if ((-not $SkipFrontendInstall) -and (-not (Test-Path $frontendNodeModules))) {
    Write-Step "node_modules not found. Running npm install in Frontend..."
    Push-Location $frontendPath
    try {
        & npm install
        if ($LASTEXITCODE -ne 0) {
            throw "npm install failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}
elseif (-not $SkipFrontendInstall) {
    Write-Step "node_modules already exists. Skipping npm install for faster startup."
}

$backendLiteral = ConvertTo-PowerShellLiteral -Value $backendPath
$frontendLiteral = ConvertTo-PowerShellLiteral -Value $frontendPath

Write-Step "Opening Backend window (mvnw spring-boot:run)..."
$backendCommand = "Set-Location $backendLiteral; .\mvnw.cmd spring-boot:run"
Start-Process -FilePath "powershell.exe" -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $backendCommand) -WorkingDirectory $backendPath | Out-Null

Write-Step "Opening Frontend window (npm run dev)..."
$frontendCommand = "Set-Location $frontendLiteral; npm run dev"
Start-Process -FilePath "powershell.exe" -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $frontendCommand) -WorkingDirectory $frontendPath | Out-Null

Write-Host ""
Write-Host "Local environment started." -ForegroundColor Green
Write-Host "Frontend: http://localhost:3000"
Write-Host "Backend API: http://localhost:8081"
Write-Host "Swagger UI: http://localhost:8081/swagger-ui.html"
Write-Host ""
Write-Host "When you finish, stop services with Ctrl+C in each opened window." -ForegroundColor Cyan
$stopRedisHint = 'To stop Redis: cmd /c "' + $stopRedisCmd + '"'
Write-Host $stopRedisHint
