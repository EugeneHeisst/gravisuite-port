#!/usr/bin/env bash
# Hidden, silent multiplayer flight audit: dedicated dev server (allow-flight=false, port 25585) plus a dev client joining it.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk ALSOFT_DRIVERS=null
mkdir -p run-mp
[ -f run-mp/options.txt ] || printf 'onboardAccessibility:false\ntutorialStep:none\n' > run-mp/options.txt
sed -i -E '/^soundCategory_/d' run-mp/options.txt
for c in master music record weather block hostile neutral player ambient voice; do echo "soundCategory_$c:0.0" >> run-mp/options.txt; done
grep -q '^allow-flight=false' run-server/server.properties
JAVA_TOOL_OPTIONS=-Dgravisuite.audit=mpflight timeout 900 ./gradlew runServer -q > run-server/mp-audit.log 2>&1 &
server=$!
for i in $(seq 1 180); do grep -q 'Done (' run-server/mp-audit.log 2>/dev/null && break; sleep 1; done
timeout 600 xvfb-run -a -s "-screen 0 1280x960x24" env JAVA_TOOL_OPTIONS=-Dgravisuite.audit=mpclient ./gradlew runMpclient -q > run-mp/mp-client.log 2>&1 || true
wait $server || true
grep -h '\[gravi-mp-audit\]' run-server/mp-audit.log run-mp/logs/latest.log
