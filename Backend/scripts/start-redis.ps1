param(
    [int]$Port = 6379,
    [string]$RedisServerPath = $env:REDIS_SERVER_PATH
)

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$pidFile = Join-Path $projectRoot ".redis-$Port.pid"

function Get-ListeningProcessId {
    param([int]$TargetPort)

    try {
        $conn = Get-NetTCPConnection -LocalPort $TargetPort -State Listen -ErrorAction Stop |
        Select-Object -First 1
        if ($null -ne $conn) {
            return [int]$conn.OwningProcess
        }
    }
    catch {
        $line = netstat -ano -p tcp | Select-String -Pattern (":{0}\s+.*LISTENING\s+(\d+)$" -f $TargetPort) |
        Select-Object -First 1
        if ($null -ne $line) {
            $parts = ($line.ToString() -replace "\s+", " ").Trim().Split(" ")
            if ($parts.Length -gt 0) {
                return [int]$parts[-1]
            }
        }
    }

    return $null
}

function Resolve-RedisServerPath {
    param(
        [string]$CandidatePath,
        [string]$RootPath
    )

    if ($CandidatePath -and (Test-Path $CandidatePath)) {
        return (Resolve-Path $CandidatePath).Path
    }

    $candidates = @(
        (Join-Path $RootPath "tools\\redis\\dist\\redis-server.exe"),
        "D:\\KLTN\\tools\\redis\\dist\\redis-server.exe"
    )

    foreach ($item in $candidates) {
        if (Test-Path $item) {
            return (Resolve-Path $item).Path
        }
    }

    $command = Get-Command redis-server.exe -ErrorAction SilentlyContinue
    if ($null -ne $command -and $command.Source) {
        return $command.Source
    }

    return $null
}

$existingPid = Get-ListeningProcessId -TargetPort $Port
if ($null -ne $existingPid) {
    Set-Content -Path $pidFile -Value $existingPid -Encoding ascii
    Write-Output ("Redis is already listening on port {0} (PID {1})." -f $Port, $existingPid)
    exit 0
}

$serverPath = Resolve-RedisServerPath -CandidatePath $RedisServerPath -RootPath $projectRoot
if ($null -eq $serverPath) {
    Write-Error "Cannot find redis-server.exe. Set REDIS_SERVER_PATH or pass -RedisServerPath."
    exit 1
}

$process = Start-Process -FilePath $serverPath -ArgumentList @("--port", $Port) -PassThru -WindowStyle Hidden

$ready = $false
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Milliseconds 500
    $pidOnPort = Get-ListeningProcessId -TargetPort $Port
    if ($null -ne $pidOnPort) {
        Set-Content -Path $pidFile -Value $pidOnPort -Encoding ascii
        Write-Output ("Redis started on port {0} (PID {1})." -f $Port, $pidOnPort)
        $ready = $true
        break
    }
}

if (-not $ready) {
    if (-not $process.HasExited) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }
    Write-Error ("Redis failed to start on port {0}." -f $Port)
    exit 1
}
