#!/usr/bin/env bash
# Hidden, silent client audit: Xvfb, every sound category 0, OpenAL null backend.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk ALSOFT_DRIVERS=null
mkdir -p run-audit
[ -f run-audit/options.txt ] || printf 'onboardAccessibility:false\ntutorialStep:none\n' > run-audit/options.txt
sed -i -E '/^soundCategory_/d' run-audit/options.txt
for c in master music record weather block hostile neutral player ambient voice; do echo "soundCategory_$c:0.0" >> run-audit/options.txt; done
exec timeout 900 xvfb-run -a -s "-screen 0 1280x960x24" env JAVA_TOOL_OPTIONS=-Dgravisuite.audit=client ./gradlew runAudit -q "$@"
