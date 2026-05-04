#!/usr/bin/env sh
set -e
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if [ -z "$JAVA_HOME" ]; then
  JAVA_EXE="java"
else
  JAVA_EXE="$JAVA_HOME/bin/java"
fi

if [ ! -f "$WRAPPER_JAR" ]; then
  mkdir -p "$WRAPPER_DIR"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  else
    echo "curl or wget is required to download the Maven Wrapper jar." >&2
    exit 1
  fi
fi

exec "$JAVA_EXE" -jar "$WRAPPER_JAR" "$@"
