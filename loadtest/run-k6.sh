#!/usr/bin/env bash

set -e

SCRIPT="$1"

if [[ -z "$SCRIPT" ]]; then
  echo "Usage: $0 <k6-script.js>"
  exit 1
fi

baseName=$(basename "$SCRIPT")
baseName="${baseName%.*}"
baseName="${baseName#k6-}"
baseName="${baseName%-test}"

timestamp=$(date +"%Y-%m-%d_%H-%M-%S")

reportDir="report"
mkdir -p "$reportDir"

reportPath="$reportDir/$baseName-$timestamp.html"
historyPath="$reportDir/metrics-history-$timestamp.json"

export K6_WEB_DASHBOARD="true"
export K6_WEB_DASHBOARD_EXPORT="$reportPath"
export K6_WEB_DASHBOARD_PERIOD="5s"
export K6_METRICS_HISTORY="$historyPath"
export K6_TEST_SCRIPT="$SCRIPT"
export NODE_EXPORTER_URL="${NODE_EXPORTER_URL:-http://localhost:9100}"

echo "Live dashboard: http://127.0.0.1:5665"
echo "Report export:  $reportPath"
echo "Metrics history: $historyPath"

collector_pid=""

if command -v node >/dev/null 2>&1; then
  node node-exporter-metrics.mjs &
  collector_pid=$!
else
  echo "WARNING: Node.js not found, metrics history will not be collected"
fi

# Run k6 test
k6 run "$SCRIPT"

# Stop collector (wait max 60s)
if [[ -n "$collector_pid" ]]; then
  timeout=60
  while kill -0 "$collector_pid" 2>/dev/null && [[ $timeout -gt 0 ]]; do
    sleep 1
    timeout=$((timeout - 1))
  done

  if kill -0 "$collector_pid" 2>/dev/null; then
    kill "$collector_pid" || true
    echo "WARNING: Metrics collector timed out"
  fi
fi

# Wait for report file
deadline=15
while [[ ! -f "$reportPath" && $deadline -gt 0 ]]; do
  sleep 1
  deadline=$((deadline - 1))
done

if [[ ! -f "$reportPath" ]]; then
  echo "WARNING: Report not found, skipping chart injection: $reportPath"
elif [[ ! -f "$historyPath" ]]; then
  echo "WARNING: Metrics history not found, skipping chart injection: $historyPath"
else
  export K6_WEB_DASHBOARD="false"
  k6 run k6-inject-charts.js
fi