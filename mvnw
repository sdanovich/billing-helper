#!/usr/bin/env bash
# Lightweight, self-bootstrapping Maven wrapper.
# Downloads a pinned Apache Maven into ./.tools on first use, then delegates.
# No global Maven install required.
set -euo pipefail

MAVEN_VERSION="3.9.11"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
TOOLS_DIR="$PROJECT_DIR/.tools"
MAVEN_HOME="$TOOLS_DIR/apache-maven-$MAVEN_VERSION"
MAVEN_BIN="$MAVEN_HOME/bin/mvn"
DIST_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"

if [ ! -x "$MAVEN_BIN" ]; then
  echo "[mvnw] Bootstrapping Apache Maven $MAVEN_VERSION ..." >&2
  mkdir -p "$TOOLS_DIR"
  tmp_zip="$TOOLS_DIR/apache-maven-$MAVEN_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DIST_URL" -o "$tmp_zip"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DIST_URL" -O "$tmp_zip"
  else
    echo "[mvnw] Need curl or wget to bootstrap Maven." >&2
    exit 1
  fi
  unzip -q "$tmp_zip" -d "$TOOLS_DIR"
  rm -f "$tmp_zip"
fi

exec "$MAVEN_BIN" "$@"
