set -e

copy_test_results () {
  directories=$(find . -type d -regex "$1")
  for directory in $directories
  do
    relative_directory=$(realpath --relative-to="." "$directory")
    destination_directory="/tmp/test_results/$relative_directory"
    mkdir -p "$destination_directory"
    cp -R "$relative_directory" "$destination_directory"
  done
}

copy_test_results ".*/build/reports/tests/testDebugUnitTest$"
copy_test_results ".*/build/reports/androidTests/connected$"
