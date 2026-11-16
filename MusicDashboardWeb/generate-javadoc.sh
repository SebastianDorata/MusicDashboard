#!/bin/bash
#
# Regenerates the Javadoc HTML for the web edition into docs/.
#
# It runs the JDK's javadoc tool with the options IntelliJ's
# "Tools > Generate JavaDoc" uses by default (protected scope, an index split
# per letter), so the pages match the desktop edition's docs/ folder.
#
# Usage, from anywhere:
#     ./generate-javadoc.sh
#
# The javadoc tool comes from JAVA_HOME when it is set, otherwise from PATH.
# To pick a specific one:
#     JAVADOC=/path/to/jdk/bin/javadoc ./generate-javadoc.sh
#
# The pages are built in a temporary folder first and only replace docs/ when
# javadoc reports no errors, so a failed run never leaves docs/ half written.
# docs/README.md is kept across runs. The full tool output is written to
# target/javadoc.log.

set -euo pipefail
cd "$(dirname "$0")"

PACKAGE=com.sebastiandorata.musicdashboard
OUT=docs
TMP=target/javadoc-html
LOG=target/javadoc.log
CLASSPATH_FILE=target/javadoc-classpath.txt

# Pick the javadoc tool.
if [ -n "${JAVADOC:-}" ]; then
    JAVADOC_BIN="$JAVADOC"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javadoc" ]; then
    JAVADOC_BIN="$JAVA_HOME/bin/javadoc"
else
    JAVADOC_BIN="$(command -v javadoc || true)"
fi
if [ -z "$JAVADOC_BIN" ] || [ ! -x "$JAVADOC_BIN" ]; then
    echo "No javadoc tool found. Set JAVA_HOME or JAVADOC." >&2
    exit 1
fi

mkdir -p target
rm -rf "$TMP"

# javadoc needs the project's libraries (Spring, JPA, Lombok...) to resolve the
# types the sources mention. Maven writes that classpath to a file.
echo "Resolving the project classpath with Maven..."
mvn -q dependency:build-classpath \
    -Dmdep.outputFile="$CLASSPATH_FILE" -Dmdep.includeScope=compile

echo "Running $("$JAVADOC_BIN" --version)..."
# -notimestamp keeps the generation date out of every page, so regenerating
# unchanged sources produces identical files and a clean git diff.
set +e
"$JAVADOC_BIN" -protected -splitindex -notimestamp \
    -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 \
    -d "$TMP" \
    -classpath "$(cat "$CLASSPATH_FILE")" \
    -sourcepath src/main/java \
    -subpackages "$PACKAGE" > "$LOG" 2>&1
STATUS=$?
set -e

ERRORS=$(grep -cE ': error:' "$LOG" || true)
WARNINGS=$(grep -cE ': warning:' "$LOG" || true)
# Classes without an explicit constructor (Spring beans, mostly) always produce
# this warning; it is counted separately so real documentation gaps stand out.
CONSTRUCTOR_WARNINGS=$(grep -c 'use of default constructor' "$LOG" || true)
GAPS=$((WARNINGS - CONSTRUCTOR_WARNINGS))

if [ "$STATUS" -ne 0 ] || [ "$ERRORS" -ne 0 ]; then
    echo "javadoc failed with $ERRORS error(s); docs/ was left unchanged." >&2
    grep -E ': error:' "$LOG" >&2 || true
    echo "Full output: $LOG" >&2
    exit 1
fi

# Replace the old pages, keeping the hand-written README.
if [ -f "$OUT/README.md" ]; then
    cp "$OUT/README.md" "$TMP/README.md"
fi
rm -rf "$OUT"
mv "$TMP" "$OUT"

echo "Wrote $(find "$OUT" -name '*.html' | wc -l | tr -d ' ') HTML pages to $OUT/ (open $OUT/index.html)."
echo "Documentation warnings: $GAPS (plus $CONSTRUCTOR_WARNINGS expected default-constructor warnings)."
if [ "$GAPS" -ne 0 ]; then
    grep -E ': warning:' "$LOG" | grep -v 'use of default constructor' || true
    echo "Full output: $LOG"
fi
