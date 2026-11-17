#!/usr/bin/env bash
# Boots a dedicated server for one Stonecutter target and checks the mod end to end:
# the server starts, the config is generated, "/tablist reload" works over RCON, the server
# stops cleanly and the log contains no errors from the mod or its mixins.
#
# Usage: bash scripts/ci/server-smoke-test.sh <target>        e.g. 26.3-fabric
#
# Optional environment:
#   SMOKE_START_TIMEOUT  seconds to wait for "Done (" (default 600)
#   SMOKE_STOP_TIMEOUT   seconds to wait for the server to exit after "stop" (default 120)
#   SMOKE_SERVER_PORT    game port (default 25565)
#   SMOKE_RCON_PORT      RCON port (default 25575)
#
# Logs are copied to versions/<target>/build/smoke-test/ for CI artifact upload.
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <target>" >&2
  exit 64
fi

target=$1
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
cd "$root"

project_dir="versions/$target"
if [[ ! -d $project_dir ]]; then
  echo "error: unknown target '$target' ($project_dir does not exist)" >&2
  exit 64
fi

case ${target##*-} in
  fabric) run_dir="$project_dir/runs/server" ;;
  neoforge) run_dir="$project_dir/run" ;;
  *)
    echo "error: cannot determine the loader of target '$target'" >&2
    exit 64
    ;;
esac

start_timeout=${SMOKE_START_TIMEOUT:-600}
stop_timeout=${SMOKE_STOP_TIMEOUT:-120}
server_port=${SMOKE_SERVER_PORT:-25565}
rcon_port=${SMOKE_RCON_PORT:-25575}

out_dir="$project_dir/build/smoke-test"
gradle_log="$out_dir/gradle.log"
latest_log="$run_dir/logs/latest.log"
config_file="$run_dir/config/tablist.toml"

java_bin=java
if [[ -n ${JAVA_HOME:-} ]] && [[ -x "$JAVA_HOME/bin/java" || -x "$JAVA_HOME/bin/java.exe" ]]; then
  java_bin="$JAVA_HOME/bin/java"
fi

rcon_password=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom 2>/dev/null | head -c 24 || true)
[[ -n $rcon_password ]] || rcon_password="smoke${RANDOM}${RANDOM}${RANDOM}"

gradle_pid=""

log() { printf '==> %s\n' "$*"; }

fail() {
  if [[ ${GITHUB_ACTIONS:-} == true ]]; then
    echo "::error title=Smoke test ($target)::$*"
  else
    echo "error: $*" >&2
  fi
  exit 1
}

show_tail() {
  local file=$1 lines=${2:-80}
  [[ -f $file ]] || return 0
  [[ ${GITHUB_ACTIONS:-} == true ]] && echo "::group::Last $lines lines of $file"
  tail -n "$lines" "$file"
  [[ ${GITHUB_ACTIONS:-} == true ]] && echo "::endgroup::"
  return 0
}

stop_gradle() {
  [[ -n $gradle_pid ]] || return 0
  if kill -0 "$gradle_pid" 2>/dev/null; then
    echo "Terminating Gradle (pid $gradle_pid)"
    kill "$gradle_pid" 2>/dev/null || true
    for _ in 1 2 3 4 5 6 7 8 9 10; do
      kill -0 "$gradle_pid" 2>/dev/null || break
      sleep 1
    done
    kill -9 "$gradle_pid" 2>/dev/null || true
  fi
  wait "$gradle_pid" 2>/dev/null || true
  gradle_pid=""
}

finish() {
  local status=$?
  stop_gradle
  if [[ -f $latest_log ]]; then cp "$latest_log" "$out_dir/latest.log" || true; fi
  if [[ -d $run_dir/crash-reports ]]; then cp -r "$run_dir/crash-reports" "$out_dir/" || true; fi
  if [[ $status -ne 0 ]]; then
    show_tail "$gradle_log" 60
    show_tail "$latest_log" 80
  fi
  exit "$status"
}
trap finish EXIT

rcon() {
  "$java_bin" scripts/ci/Rcon.java 127.0.0.1 "$rcon_port" "$rcon_password" "$@"
}

# --- Prepare the run directory -------------------------------------------------------------

mkdir -p "$run_dir" "$out_dir"
rm -f "$latest_log" "$gradle_log"
# On CI start from a clean config so the generation itself is tested; keep local edits otherwise.
if [[ ${CI:-} == true ]]; then rm -f "$config_file"; fi

echo "eula=true" >"$run_dir/eula.txt"
cat >"$run_dir/server.properties" <<EOF
motd=Better TabList smoke test
online-mode=false
white-list=false
enforce-whitelist=false
enable-rcon=true
rcon.port=$rcon_port
rcon.password=$rcon_password
server-port=$server_port
view-distance=2
simulation-distance=2
spawn-protection=0
EOF

# --- Start the server ----------------------------------------------------------------------

log "Starting $target server (run dir: $run_dir)"
./gradlew ":$target:runServer" --configure-on-demand --console=plain </dev/null >"$gradle_log" 2>&1 &
gradle_pid=$!

deadline=$((SECONDS + start_timeout))
started=$SECONDS
until [[ -f $latest_log ]] && grep -qE 'Done \([0-9.,]+s\)!' "$latest_log"; do
  if ! kill -0 "$gradle_pid" 2>/dev/null; then
    code=0
    wait "$gradle_pid" || code=$?
    gradle_pid=""
    fail "Gradle exited with code $code before the server finished starting (see $gradle_log)"
  fi
  if ((SECONDS >= deadline)); then
    fail "server did not report 'Done (' within ${start_timeout}s"
  fi
  sleep 2
done
done_line=$(grep -m1 -oE 'Done \([0-9.,]+s\)!' "$latest_log")
log "Server ready after $((SECONDS - started))s: $done_line"

# --- Exercise the mod over RCON ------------------------------------------------------------

log "rcon: tablist reload"
reload_reply=$(rcon tablist reload) || fail "RCON command 'tablist reload' failed"
echo "$reload_reply"
[[ $reload_reply == *"TabList config reloaded"* ]] ||
  fail "unexpected reply to 'tablist reload': ${reload_reply:-<empty>}"

log "rcon: list"
list_reply=$(rcon list) || fail "RCON command 'list' failed"
echo "$list_reply"

log "rcon: stop"
rcon stop || fail "RCON command 'stop' failed"

deadline=$((SECONDS + stop_timeout))
while kill -0 "$gradle_pid" 2>/dev/null; do
  if ((SECONDS >= deadline)); then
    stop_gradle
    fail "server did not stop within ${stop_timeout}s"
  fi
  sleep 2
done
gradle_status=0
wait "$gradle_pid" || gradle_status=$?
gradle_pid=""
((gradle_status == 0)) || fail "Gradle runServer exited with code $gradle_status"
log "Server stopped"

# --- Assertions ----------------------------------------------------------------------------

[[ -f $config_file ]] || fail "$config_file was not generated"
grep -q 'TabList config loaded' "$latest_log" || fail "latest.log does not contain 'TabList config loaded'"
config_loads=$(grep -c 'TabList config loaded' "$latest_log" || true)

problems=$(
  {
    # "tablist" must not follow a letter or '-', so the checkout path (.../Better-TabList/...) never matches.
    grep -E '/ERROR\]|Exception' "$latest_log" | grep -iE '(^|[^-A-Za-z])tablist|com\.sennecools|mixin' || true
    grep -E '^[[:space:]]+at com\.sennecools\.' "$latest_log" || true
  } | head -n 40 || true
)
if [[ -n $problems ]]; then
  echo "$problems"
  fail "latest.log contains errors from TabList or mixins"
fi

# --- Summary -------------------------------------------------------------------------------

summary="| $target | $done_line | $config_loads | ok | ${list_reply:-<empty>} |"
echo
echo "Smoke test passed for $target"
echo "  startup:        $done_line"
echo "  config loads:   $config_loads (startup + reload)"
echo "  tablist reload: ok"
echo "  list:           ${list_reply:-<empty>}"
if [[ -n ${GITHUB_STEP_SUMMARY:-} ]]; then
  {
    echo "| Target | Startup | Config loads | Reload | list |"
    echo "| --- | --- | --- | --- | --- |"
    echo "$summary"
  } >>"$GITHUB_STEP_SUMMARY" || true
fi
