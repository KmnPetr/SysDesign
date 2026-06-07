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
$historyPath = "$reportDir/metrics-history-$timestamp.json"

$scriptContent = Get-Content $Script -Raw
if ($scriptContent -match "TEST_DURATION\s*=\s*(?:__ENV\.TEST_DURATION\s*\|\|\s*)?'([^']+)'") {
    $env:TEST_DURATION = $Matches[1]
} elseif (-not $env:TEST_DURATION) {
    $env:TEST_DURATION = "30s"
}

$env:K6_WEB_DASHBOARD = "true"
$env:K6_WEB_DASHBOARD_EXPORT = $reportPath
$env:K6_WEB_DASHBOARD_PERIOD = "5s"
$env:K6_METRICS_HISTORY = $historyPath
$env:NODE_EXPORTER_URL = if ($env:NODE_EXPORTER_URL) { $env:NODE_EXPORTER_URL } else { "http://localhost:9100" }

Write-Host "Live dashboard: http://127.0.0.1:5665"
Write-Host "Report export:  $reportPath"
Write-Host "Metrics history: $historyPath"
Write-Host "Test duration:   $env:TEST_DURATION"

$collector = $null
if (Get-Command node -ErrorAction SilentlyContinue) {
    $collector = Start-Process -FilePath "node" -ArgumentList "collect-metrics.mjs" -WorkingDirectory (Get-Location) -PassThru -NoNewWindow
} else {
    Write-Warning "Node.js not found, metrics history will not be collected"
}

k6 run $Script

if ($collector) {
    $waitDeadline = (Get-Date).AddSeconds(60)
    while (-not $collector.HasExited -and (Get-Date) -lt $waitDeadline) {
        Start-Sleep -Milliseconds 200
    }
    if (-not $collector.HasExited) {
        $collector.Kill()
        Write-Warning "Metrics collector timed out"
    }
}

$deadline = (Get-Date).AddSeconds(15)
while (-not (Test-Path $reportPath) -and (Get-Date) -lt $deadline) {
    Start-Sleep -Milliseconds 200
}

if (-not (Test-Path $reportPath)) {
    Write-Warning "Report not found, skipping chart injection: $reportPath"
} elseif (-not (Test-Path $historyPath)) {
    Write-Warning "Metrics history not found, skipping chart injection: $historyPath"
} else {
    $env:K6_WEB_DASHBOARD = "false"
    k6 run k6-inject-charts.js
}
