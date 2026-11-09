#!/usr/bin/env bash
# Automated client run: opens Minecraft, builds a test scene, clicks like a player,
# takes screenshots, writes a report and quits. Do not touch the window while it runs.
#
# Usage: tools/client-test.sh fabric|forge
# Output: <platform>/run-clienttest/cbtest-report.txt and .../screenshots/cbtest-*.png
set -uo pipefail

PLATFORM="${1:?usage: $0 fabric|forge}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/$PLATFORM/run-clienttest"
export JAVA_HOME="${JAVA_HOME:-$(ls -d "$HOME"/.jdks/jdk-17*/ 2>/dev/null | head -1)}"

rm -rf "$RUN/saves" "$RUN/screenshots" "$RUN/cbtest-report.txt"
cd "$ROOT"
timeout 420 ./gradlew ":$PLATFORM:runClientTest" ${GRADLE_ARGS:-} --console=plain > "$RUN.log" 2>&1
CODE=$?
if [ "$CODE" = 124 ]; then
  echo "Timed out; killing the dev client of this project."
  # The client JVM is a child of the Gradle daemon, so timeout does not stop it.
  powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'architectury.main.class' -and \$_.CommandLine -match 'CarpertnersBlocksReborn' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }"
fi

if [ ! -f "$RUN/cbtest-report.txt" ]; then
  echo "FAIL ($PLATFORM): no report, gradle exit $CODE. See $RUN.log"
  grep -E "Exception|ERROR|FATAL|Caused by" "$RUN.log" | grep -v "Realms\|appender\|Appenders" | head -30
  exit 1
fi
cat "$RUN/cbtest-report.txt"
grep -q "RESULT: all checks passed" "$RUN/cbtest-report.txt"
