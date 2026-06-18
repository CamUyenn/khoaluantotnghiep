param(
    [string]$Root = "."
)

$patterns = @("*.java", "*.xml", "*.properties", "*.yml", "*.yaml", "*.sql", "*.md")
$fullRoot = (Resolve-Path $Root).Path

$files = Get-ChildItem -Path $fullRoot -Recurse -File -Include $patterns |
    Where-Object { $_.FullName -notmatch "\\target\\|\\.git\\" }

$found = @()

foreach ($file in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $found += $file.FullName
    }
}

if ($found.Count -gt 0) {
    Write-Output "UTF-8 BOM detected in:"
    $found | ForEach-Object { Write-Output $_ }
    exit 1
}

Write-Output "No UTF-8 BOM detected."
exit 0