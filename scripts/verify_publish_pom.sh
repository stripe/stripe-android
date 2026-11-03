#!/bin/bash
set -euo pipefail

# Verifies that published POMs resolve correctly by publishing all modules to
# Maven Local, then compiling paymentsheet-example against real Maven coordinates
# instead of project dependencies.

echo "==> Setting test version to 99.0.0"
sed -i.bak 's/^VERSION_NAME=.*/VERSION_NAME=99.0.0/' gradle.properties

echo "==> Publishing all modules to Maven Local"
./gradlew publishToMavenLocal --no-configuration-cache

echo "==> Adding mavenLocal() to root build.gradle"
sed -i.bak 's/mavenCentral()/mavenLocal()\
        mavenCentral()/' build.gradle

echo "==> Replacing project dependencies with Maven coordinates in paymentsheet-example"
sed -i.bak "s|implementation project(':payments')|implementation 'com.stripe:stripe-android:99.0.0'|" paymentsheet-example/build.gradle
sed -i.bak "s|implementation project(':financial-connections')|implementation 'com.stripe:financial-connections:99.0.0'|" paymentsheet-example/build.gradle
rm -f gradle.properties.bak build.gradle.bak paymentsheet-example/build.gradle.bak

echo "==> Verifying replacements took effect"
grep -q "com.stripe:stripe-android:99.0.0" paymentsheet-example/build.gradle || { echo "ERROR: stripe-android replacement not found"; exit 1; }
grep -q "com.stripe:financial-connections:99.0.0" paymentsheet-example/build.gradle || { echo "ERROR: financial-connections replacement not found"; exit 1; }

echo "==> Compiling paymentsheet-example against Maven Local artifacts"
./gradlew :paymentsheet-example:assembleBaseDebug --no-configuration-cache

echo "==> POM verification passed"
