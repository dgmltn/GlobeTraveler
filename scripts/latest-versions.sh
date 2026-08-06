#!/usr/bin/env bash
# Prints the latest stable version of every dependency in gradle/libs.versions.toml,
# queried from live registry metadata (Maven Central + Google Maven + services.gradle.org).
# Usage: scripts/latest-versions.sh
set -euo pipefail

STABLE_FILTER='alpha|beta|rc|dev|snapshot|-M[0-9]|eap|compat'

# Latest stable version of group:artifact on Maven Central.
latest_central() {
  local group_path="${1//.//}" artifact="$2" extra_filter="${3:-.}"
  curl -sfL "https://repo1.maven.org/maven2/${group_path}/${artifact}/maven-metadata.xml" |
    grep -o '<version>[^<]*</version>' | sed -E 's/<\/?version>//g' |
    grep -viE "$STABLE_FILTER" | grep -E "$extra_filter" | sort -V | tail -1
}

# Latest stable version of an artifact in a Google Maven group (group-index.xml).
latest_google() {
  local group_path="${1//.//}" artifact="$2" extra_filter="${3:-.}"
  curl -sfL "https://maven.google.com/${group_path}/group-index.xml" |
    grep -o "<${artifact} versions=\"[^\"]*\"" | sed -E 's/.*versions="([^"]*)".*/\1/' |
    tr ',' '\n' | grep -viE "$STABLE_FILTER" | grep -E "$extra_filter" | sort -V | tail -1
}

echo "gradle=$(curl -sf https://services.gradle.org/versions/current | grep '"version"' | sed -E 's/.*"version" *: *"([^"]+)".*/\1/')"
echo "kotlin=$(latest_central org.jetbrains.kotlin kotlin-gradle-plugin)"
echo "agp=$(latest_google com.android.tools.build gradle '^8\.')"
echo "composeMultiplatform=$(latest_central org.jetbrains.compose org.jetbrains.compose.gradle.plugin)"
echo "composeMaterial3=$(latest_central org.jetbrains.compose.material3 material3)"
echo "ksp=$(latest_central com.google.devtools.ksp symbol-processing-api)"
echo "room3=$(latest_google androidx.room3 room3-runtime)"
echo "sqlite=$(latest_google androidx.sqlite sqlite-bundled)"
echo "activityCompose=$(latest_google androidx.activity activity-compose)"
echo "koin=$(latest_central io.insert-koin koin-core)"
echo "koinCompose=$(latest_central io.insert-koin koin-compose)"
echo "koinComposeViewmodel=$(latest_central io.insert-koin koin-compose-viewmodel)"
echo "kotlinxSerialization=$(latest_central org.jetbrains.kotlinx kotlinx-serialization-json)"
echo "kotlinxDatetime=$(latest_central org.jetbrains.kotlinx kotlinx-datetime)"
echo "kotlinxCollectionsImmutable=$(latest_central org.jetbrains.kotlinx kotlinx-collections-immutable)"
echo "kotlinxCoroutines=$(latest_central org.jetbrains.kotlinx kotlinx-coroutines-core)"
echo "kermit=$(latest_central co.touchlab kermit)"
echo "maplibreCompose=$(latest_central org.maplibre.compose maplibre-compose)"
echo "lifecycleViewmodelCompose=$(latest_central org.jetbrains.androidx.lifecycle lifecycle-viewmodel-compose)"
