param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Script
)

$baseName = [System.IO.Path]::GetFileNameWithoutExtension($Script) -replace '^k6-', '' -replace '-test$', ''
$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$reportDir = "report"

if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
}

$reportPath = "$reportDir/$baseName-$timestamp.html"

$env:K6_WEB_DASHBOARD = "true"
$env:K6_WEB_DASHBOARD_EXPORT = $reportPath
$env:K6_WEB_DASHBOARD_PERIOD = "5s"

Write-Host "Live dashboard: http://127.0.0.1:5665"
Write-Host "Report export:  $reportPath"

k6 run $Script

$deadline = (Get-Date).AddSeconds(15)
while (-not (Test-Path $reportPath) -and (Get-Date) -lt $deadline) {
    Start-Sleep -Milliseconds 200
}

if (-not (Test-Path $reportPath)) {
    Write-Warning "Report not found, skipping chart injection: $reportPath"
} else {
    $historyPath = "$reportDir/metrics-history-$timestamp.json"
    $baseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:4200" }

    try {
        $historyBody = (Invoke-WebRequest -Uri "$baseUrl/api/info/metrics/history" -UseBasicParsing).Content
        [System.IO.File]::WriteAllText((Join-Path (Get-Location) $historyPath), $historyBody, [System.Text.UTF8Encoding]::new($false))
    } catch {
        Write-Warning ("Failed to fetch metrics history: " + $_.Exception.Message)
        $historyPath = $null
    }

    if ($historyPath -and (Test-Path $historyPath)) {
        $env:K6_WEB_DASHBOARD = "false"
        $env:K6_METRICS_HISTORY = $historyPath
        k6 run k6-inject-charts.js
    }
}
