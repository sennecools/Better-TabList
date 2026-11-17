#!/usr/bin/env bash
# Lists the Stonecutter targets declared in settings.gradle, without running Gradle.
#
#   list-targets.sh             JSON array (one line) of {target, minecraft, loader, loader_name}
#   list-targets.sh --plain     "<target> <minecraft> <loader>" per line
#   list-targets.sh --versions  unique Minecraft versions, in declaration order
set -euo pipefail

mode=${1:---json}
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
settings="$root/settings.gradle"

# Matches lines like:  version '26.3-fabric', '26.3'   (single or double quotes; comments are skipped)
pairs=$(tr -d '\r' <"$settings" |
  sed -nE "s/^[[:space:]]*version[[:space:]]*\(?[[:space:]]*['\"]([^'\"]+)['\"][[:space:]]*,[[:space:]]*['\"]([^'\"]+)['\"].*/\1 \2/p")

if [[ -z $pairs ]]; then
  echo "error: no Stonecutter targets found in $settings" >&2
  exit 1
fi

json=""
plain=""
versions=""
while read -r target minecraft; do
  loader=${target##*-}
  case $loader in
    fabric) loader_name=Fabric ;;
    neoforge) loader_name=NeoForge ;;
    *)
      echo "error: cannot determine the loader of target '$target'" >&2
      exit 1
      ;;
  esac
  json+="${json:+,}{\"target\":\"$target\",\"minecraft\":\"$minecraft\",\"loader\":\"$loader\",\"loader_name\":\"$loader_name\"}"
  plain+="$target $minecraft $loader"$'\n'
  [[ $'\n'$versions == *$'\n'"$minecraft"$'\n'* ]] || versions+="$minecraft"$'\n'
done <<<"$pairs"

case $mode in
  --json) echo "[$json]" ;;
  --plain) printf '%s' "$plain" ;;
  --versions) printf '%s' "$versions" ;;
  *)
    echo "usage: $0 [--json|--plain|--versions]" >&2
    exit 64
    ;;
esac
