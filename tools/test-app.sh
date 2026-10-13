#!/usr/bin/env bash
# Runs every app test on the JVM, including the API tests against a throwaway board (tools/test-board.mjs).
# Rich's own board on 4321 is never touched.   Usage: bash tools/test-app.sh
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-$LOCALAPPDATA/AndroidTools/jdk17}"

fifo=$(mktemp -u); mkfifo "$fifo"
exec 3<> "$fifo"; rm "$fifo"
node tools/test-board.mjs <&3 > board.out.local 2>&1 &
pid=$!
trap 'exec 3>&-; wait $pid 2>/dev/null || true; rm -f board.out.local' EXIT
for _ in $(seq 60); do grep -q '"base"' board.out.local 2>/dev/null && break; sleep 0.5; done
info=$(grep '"base"' board.out.local)
get() { node -e "console.log(JSON.parse(process.argv[1])[process.argv[2]])" "$info" "$1"; }
export TEST_BOARD=$(get base) TEST_SLUG=$(get slug) TEST_DM=$(get dm) TEST_ASK=$(get ask)
echo "test board: $TEST_BOARD"
./gradlew testDebugUnitTest --rerun-tasks --console=plain -q 2>&1 | grep -v '^w: ' || true
grep -ho 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml
! grep -q '<failure' app/build/test-results/testDebugUnitTest/*.xml
