#!/usr/bin/env bash
# Writes the boilerplate resources of a coverable block whose geometry is generated in code:
# block state (all states -> block/coverable), item model (transforms only) and a loot table
# that drops the block's own item when it survives the explosion.
#
# Usage: tools/gen-block.sh <id> [--no-item] [--no-loot]
set -euo pipefail
ID="${1:?usage: $0 <id> [--no-item] [--no-loot]}"
shift
ROOT="$(cd "$(dirname "$0")/.." && pwd)/common/src/main/resources"
A="$ROOT/assets/carpentersblocks"
D="$ROOT/data/carpentersblocks"
ITEM=1
LOOT=1
for arg in "$@"; do
  case "$arg" in
    --no-item) ITEM=0 ;;
    --no-loot) LOOT=0 ;;
  esac
done
mkdir -p "$A/blockstates" "$A/models/item" "$D/loot_tables/blocks"
echo '{ "variants": { "": { "model": "carpentersblocks:block/coverable" } } }' > "$A/blockstates/$ID.json"
if [ $ITEM = 1 ]; then
  echo '{ "parent": "carpentersblocks:block/coverable" }' > "$A/models/item/$ID.json"
fi
if [ $LOOT = 1 ]; then
  cat > "$D/loot_tables/blocks/$ID.json" <<EOF
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [ { "type": "minecraft:item", "name": "carpentersblocks:$ID" } ],
      "conditions": [ { "condition": "minecraft:survives_explosion" } ]
    }
  ]
}
EOF
fi
echo "generated $ID"
