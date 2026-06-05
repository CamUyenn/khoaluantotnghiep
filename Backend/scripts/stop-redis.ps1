param(
    [int]$Port = 6379
)

function Get-ListeningOwnerId {
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
            if ($parts.Length -gt 0 -and $parts[-1] -match "^\d+$") {
                return [int]$parts[-1]
            }
        }
    }

    return $null
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$markerFile = Join-Path $projectRoot (".redis-{0}.pid" -f $Port)
$ownerIds = New-Object System.Collections.Generic.List[int]

if (Test-Path $markerFile) {
    $savedValue = Get-Content -Path $markerFile -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($savedValue -and $savedValue -match "^\d+$") {
        [void]$ownerIds.Add([int]$savedValue)
    }
}

$currentOwner = Get-ListeningOwnerId -TargetPort $Port
if ($null -ne $currentOwner) {
    [void]$ownerIds.Add([int]$currentOwner)
}

$stoppedAny = $false
$uniqueOwnerIds = $ownerIds | Select-Object -Unique
foreach ($ownerId in $uniqueOwnerIds) {
    $proc = Get-Process -Id $ownerId -ErrorAction SilentlyContinue
    if ($null -eq $proc) {
        continue
    }

    if ($proc.ProcessName -ne "redis-server") {
        continue
    }

    Stop-Process -Id $ownerId -Force -ErrorAction SilentlyContinue
    Write-Output ("Stopped Redis process PID {0}." -f $ownerId)
    $stoppedAny = $true
}

if (Test-Path $markerFile) {
    Remove-Item -Path $markerFile -Force -ErrorAction SilentlyContinue
}

Start-Sleep -Milliseconds 300
$stillListening = $null -ne (Get-ListeningOwnerId -TargetPort $Port)
if ($stillListening) {
    Write-Error ("Port {0} is still listening after stop attempt." -f $Port)
    exit 1
}

if ($stoppedAny) {
    Write-Output ("Redis has been stopped on port {0}." -f $Port)
}
else {
    Write-Output ("No Redis process was running on port {0}." -f $Port)
}
