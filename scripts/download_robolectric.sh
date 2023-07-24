#!/usr/bin/env bash

set -e # Fail on error.

# To get this list run the following commands.
# rm -rf ~/.m2/repository/org/robolectric/android-all-instrumented
# ./gradlew testDebugUnitTest
# ls ~/.m2/repository/org/robolectric/android-all-instrumented/
# The resulting list of versions should be added below.
declare -a robolectric_versions=("10-robolectric-5803371-i4" "11-robolectric-6757853-i4" "13-robolectric-9030017-i4" "4.1.2_r1-robolectric-r1-i4" "9-robolectric-4913185-2-i4")

for i in ${robolectric_versions[@]}; do
  mvn dependency:get -Dartifact=org.robolectric:android-all-instrumented:${i}
done
