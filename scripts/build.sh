#!/usr/bin/env bash
set -euo pipefail

edition="${1:-}"
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

case "$edition" in
  integrations-1.21.1)
    project="$repo"; java_version=21
    jar='neo-voxy-0.3.3-mc1.21.1-neoforge-integrations.jar' ;;
  client-1.21.1)
    project="$repo/editions/neoforge-1.21.1-client"; java_version=21
    jar='neo-voxy-0.2.18-beta-mc1.21.1-neoforge-client.jar' ;;
  client-1.20.1)
    project="$repo/editions/forge-1.20.1-client"; java_version=17
    jar='neo-voxy-0.3.3-1.20.1-alpha.1-forge-client.jar' ;;
  client-26.1.2)
    project="$repo/editions/neoforge-26.1.2-client"; java_version=25
    jar='neo-voxy-0.2.18-beta-mc26.1.2-neoforge-client.jar' ;;
  *)
    echo 'Usage: scripts/build.sh {integrations-1.21.1|client-1.21.1|client-1.20.1|client-26.1.2}' >&2
    exit 2 ;;
esac

java_home_var="JAVA_HOME_${java_version}"
java_home="${!java_home_var:-}"
if [[ -n "$java_home" ]]; then
  export JAVA_HOME="$java_home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
actual_java="$(java -version 2>&1 | head -n 1)"
if [[ ! "$actual_java" =~ \"${java_version}([.\"]|$) ]]; then
  echo "JDK $java_version is required for $edition; set $java_home_var." >&2
  exit 2
fi

if [[ -z "${GRADLE_USER_HOME:-}" && -d "$(dirname "$repo")/.gradle-user-home" ]]; then
  export GRADLE_USER_HOME="$(dirname "$repo")/.gradle-user-home"
fi

chmod +x "$project/gradlew"
gradle_launcher="$project/gradlew"
if [[ -n "${GRADLE_USER_HOME:-}" ]]; then
  distribution_url="$(sed -n 's/^distributionUrl=//p' "$project/gradle/wrapper/gradle-wrapper.properties")"
  distribution_name="$(basename "${distribution_url%.zip}")"
  cached_launcher="$(find "$GRADLE_USER_HOME/wrapper/dists/$distribution_name" -type f -path '*/bin/gradle' -print -quit 2>/dev/null || true)"
  if [[ -n "$cached_launcher" ]]; then
    gradle_launcher="$cached_launcher"
  fi
fi
(cd "$project" && "$gradle_launcher" -I init.gradle clean build --no-daemon --no-configuration-cache)

source_jar="$project/build/libs/$jar"
test -f "$source_jar" || { echo "Missing release JAR: $source_jar" >&2; exit 1; }
mkdir -p "$repo/dist"
cp "$source_jar" "$repo/dist/$jar"
ls -lh "$repo/dist/$jar"
