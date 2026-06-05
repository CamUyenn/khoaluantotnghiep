param(
    [string]$Root = "."
)

$patterns = @("*.java", "*.xml", "*.properties", "*.yml", "*.yaml", "*.sql", "*.md")
$fullRoot = (Resolve-Path $Root).Path

$files = Get-ChildItem -Path $fullRoot -Recurse -File -Include $patterns |
Where-Object { $_.FullName -notmatch "\\target\\|\\.git\\" }

$changed = 0

foreach ($file in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        [System.IO.File]::WriteAllBytes($file.FullName, $bytes[3..($bytes.Length - 1)])
        Write-Output "Removed BOM: $($file.FullName)"
        $changed++
    }
}

if ($changed -eq 0) {
    Write-Output "No UTF-8 BOM files found."
}
else {
    Write-Output "Removed BOM from $changed file(s)."
}