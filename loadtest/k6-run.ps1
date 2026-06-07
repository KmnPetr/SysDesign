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

$env:K6_WEB_DASHBOARD = "false"
$env:BASE_URL = "http://localhost:4200"
k6 run k6-inject-charts.js
