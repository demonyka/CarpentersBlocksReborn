#!/usr/bin/env bash
# Starts a REAL dedicated server (not the dev environment, which has client classes on the
# classpath) with the built mod jar, runs SmokeTest and checks the result.
#
# Usage: tools/smoke-test.sh fabric|forge
# Env:   SERVER_JAVA  java used to run the server (default: JDK 8 from ~/.jdks)
#
# Build first: ./gradlew build
set -euo pipefail

PLATFORM="${1:?usage: $0 fabric|forge}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck disable=SC1090
source <(grep -E '^(mod_version|minecraft_version|fabric_loader_version|fabric_api_version|forge_version)=' "$ROOT/gradle.properties")

SERVER_JAVA="${SERVER_JAVA:-$(ls -d "$HOME"/.jdks/jdk8*/ 2>/dev/null | head -1)bin/java}"
DIR="$ROOT/run-dedicated/$PLATFORM"
MOD_JAR="$ROOT/$PLATFORM/build/libs/carpentersblocks-${mod_version}+mc${minecraft_version}-${PLATFORM}.jar"
FABRIC_INSTALLER_VERSION=1.0.1

[ -f "$MOD_JAR" ] || { echo "Mod jar not found: $MOD_JAR (run ./gradlew build)"; exit 2; }
mkdir -p "$DIR/mods"
cd "$DIR"

download() { [ -f "$2" ] || curl -fsSL -o "$2" "$1"; }

case "$PLATFORM" in
  fabric)
    download "https://meta.fabricmc.net/v2/versions/loader/${minecraft_version}/${fabric_loader_version}/${FABRIC_INSTALLER_VERSION}/server/jar" fabric-server-launch.jar
    # The Maven artifact for 1.16 is an empty pom-jar; the full jar is on Modrinth.
    FAPI_ENC="${fabric_api_version/+/%2B}"
    download "https://cdn.modrinth.com/data/P7dR8mSH/versions/${FAPI_ENC}/fabric-api-${FAPI_ENC}.jar" "mods/fabric-api-${fabric_api_version}.jar"
    SERVER_JAR=fabric-server-launch.jar
    ;;
  forge)
    SERVER_JAR="forge-${forge_version}.jar"
    if [ ! -f "$SERVER_JAR" ]; then
      download "https://maven.minecraftforge.net/net/minecraftforge/forge/${forge_version}/forge-${forge_version}-installer.jar" installer.jar
      "$SERVER_JAVA" -jar installer.jar --installServer > installer.log 2>&1
    fi
    ;;
  *) echo "unknown platform $PLATFORM"; exit 2 ;;
esac

rm -f mods/carpentersblocks-*.jar
cp "$MOD_JAR" mods/
echo "eula=true" > eula.txt
cat > server.properties <<'PROPS'
online-mode=false
level-type=flat
spawn-protection=0
max-tick-time=-1
PROPS
rm -rf world logs

echo "Starting $PLATFORM server with $SERVER_JAVA ..."
set +e
timeout 600 "$SERVER_JAVA" -Xmx2G -Dcarpentersblocks.smokeTest=true -jar "$SERVER_JAR" nogui < /dev/null > server-stdout.log 2>&1
CODE=$?
set -e

LOG=logs/latest.log
[ -f "$LOG" ] || LOG=server-stdout.log
if grep -q "CARPENTERS SMOKE TEST PASSED" "$LOG" server-stdout.log; then
  echo "PASS ($PLATFORM): server started, smoke test passed, exit code $CODE"
  # Only our own warnings; vanilla 1.16 logs harmless errors when stopped right after world gen.
  grep -E "\[Carpenter's Blocks/?\]" "$LOG" | grep -iE "warn|error" | head -20 || true
  exit 0
fi
echo "FAIL ($PLATFORM), exit code $CODE. Last log lines:"
grep -E "SMOKE TEST|Exception|Error|Caused by" "$LOG" server-stdout.log | head -40
tail -30 server-stdout.log
exit 1
