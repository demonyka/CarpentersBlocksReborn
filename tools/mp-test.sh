#!/usr/bin/env bash
# Multiplayer test: a REAL dedicated server with the built jar, and two dev clients that join
# it over the network one after the other.
#   Tester A: covers a block, hammers one, opens a door, pulls a lever, places a slope.
#   Tester B: joins later and must see all of it (saved world + chunk/block entity sync).
#
# Usage: tools/mp-test.sh fabric|forge     (run tools/smoke-test.sh once first to install the server)
set -uo pipefail

PLATFORM="${1:?usage: $0 fabric|forge}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck disable=SC1090
source <(grep -E '^(mod_version|minecraft_version|forge_version)=' "$ROOT/gradle.properties")
SERVER_JAVA="${SERVER_JAVA:-$(ls -d "$HOME"/.jdks/jdk8*/ 2>/dev/null | head -1)bin/java}"
export JAVA_HOME="${JAVA_HOME:-$(ls -d "$HOME"/.jdks/jdk-17*/ 2>/dev/null | head -1)}"
DIR="$ROOT/run-dedicated/$PLATFORM"
RUN="$ROOT/$PLATFORM/run-clienttest"
MOD_JAR="$ROOT/$PLATFORM/build/libs/carpentersblocks-${mod_version}+mc${minecraft_version}-${PLATFORM}.jar"

case "$PLATFORM" in
  fabric) SERVER_JAR=fabric-server-launch.jar ;;
  forge)  SERVER_JAR="forge-${forge_version}.jar" ;;
esac
[ -f "$DIR/$SERVER_JAR" ] || { echo "Server not installed; run tools/smoke-test.sh $PLATFORM first"; exit 2; }
[ -f "$MOD_JAR" ] || { echo "Mod jar not found (./gradlew build)"; exit 2; }

cd "$DIR"
rm -f mods/carpentersblocks-*.jar
cp "$MOD_JAR" mods/
cat > server.properties <<'PROPS'
online-mode=false
level-type=flat
spawn-protection=0
server-port=25565
max-tick-time=-1
PROPS
echo "eula=true" > eula.txt
rm -rf world logs mp-server.log
mkdir -p "$RUN"
rm -f "$RUN"/cbtest-report-mp-*.txt "$RUN"/screenshots/cbtest-mp-*.png

echo "Starting $PLATFORM server..."
"$SERVER_JAVA" -Xmx2G -Dcarpentersblocks.mpTest=true -jar "$SERVER_JAR" nogui < /dev/null > mp-server.log 2>&1 &
SERVER_PID=$!
for i in $(seq 1 300); do
  grep -q "MultiplayerTest\] scene ready" mp-server.log logs/latest.log 2>/dev/null && break
  if ! kill -0 $SERVER_PID 2>/dev/null; then echo "Server died:"; tail -30 mp-server.log; exit 1; fi
  sleep 1
done
echo "Server ready."

cd "$ROOT"
for role in a b; do
  echo "Client $role..."
  timeout 420 ./gradlew ":$PLATFORM:runClientTest" -PcbTest=mp-$role -PcbUser=CBTester$(echo $role | tr a-z A-Z) \
    --console=plain > "$RUN-mp-$role.log" 2>&1
done

for i in $(seq 1 60); do kill -0 $SERVER_PID 2>/dev/null || break; sleep 1; done
kill $SERVER_PID 2>/dev/null

FAILED=0
for role in a b; do
  REPORT="$RUN/cbtest-report-mp-$role.txt"
  if [ -f "$REPORT" ]; then
    cat "$REPORT"
    grep -q "RESULT: all checks passed" "$REPORT" || FAILED=1
  else
    echo "FAIL: no report from tester $role (see $RUN-mp-$role.log)"
    grep -E "Exception|ERROR|Disconnect|kick" "$RUN-mp-$role.log" | grep -v "Realms\|appender" | head -10
    FAILED=1
  fi
done
echo "Server log (mod lines):"
grep -E "MultiplayerTest|Carpenter|Exception|ERROR" "$DIR/logs/latest.log" | grep -v "appender" | head -20
exit $FAILED
