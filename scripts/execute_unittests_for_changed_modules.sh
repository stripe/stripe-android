# This script first finds all the modules changed in a PR, then finds all their dependent modules, and executes testDebugUnitTest on these modules.
# If some critical dependency is changed, e.g the root build.gradle file, all unit tests will be executed.


# directory names that corresponds to a module that has unit tests - this list needs to be manually updated when a new module is added
TESTABLE_MODULES="payments payments-core paymentsheet wechatpay link stripecardscan identity stripe-core payments-ui-core camera-core"
# a function to check if a dir is in TESTABLE_MODULES
isTestableModule() {
  [[ $TESTABLE_MODULES =~ (^|[[:space:]])$1($|[[:space:]]) ]]
}

# Files or directories that if changed, will trigger all the unitest run
CRITICAL_DEPS="build.gradle settings.gradle"
# a function to check if a file/dir is in CRITICAL_DEPS
isCriticalDeps() {
  [[ $CRITICAL_DEPS =~ (^|[[:space:]])$1($|[[:space:]]) ]]
}

# find all dirs changed through git diff
changed_dirs=""
while read line; do
  module_name=${line%%/*} # This gets the first word before '/'
  # add this dir if we haven't add it yet
  if [[ ${changed_dirs} != *"${module_name}"* ]]; then
    changed_dirs="$changed_dirs $module_name" # string concat
  fi
done < <(git diff --name-only origin/master)

# for all changed dirs, first determine if it is a module, then find all its project dependencies
modules_to_test=""
for dir in $changed_dirs
do
  if isCriticalDeps $dir; then
    echo critical dep $dir is changed, running ALL unit tests
    echo "./gradlew testDebugUnitTest"
    eval "./gradlew testDebugUnitTest"
    exit
  elif isTestableModule $dir; then
    # add this module if we haven't add it yet
    if [[ $modules_to_test != *"$dir"* ]]; then
      modules_to_test="$modules_to_test $dir"
    fi
    # The gradle command outputs all dependencies, for project dependencies, it looks like this:
    #
    # +--- project :payments-core
    # |    +--- project :stripe-core
    # +--- project :stripe-core (*)
    # +--- project :payments-ui-core
    #
    # Note dependencies that appears more than once are suffixed with "(*)"
    #
    # The output is grep-ed with all project dependency lines that doesn't end with ")", then the last part of the line after : is cut
    #
    # For the previous output, module_deps will be assigned these values: ["payments-core", "stripe-core", "payments-ui-core"]
    module_deps=$(./gradlew :$dir:dependencies --configuration debugCompileClasspath | grep '+--- project :.*\w$' | cut -d ":" -f 2)
    for dep in $module_deps
    do
      # add this dep module if we haven't added it yet
      if [[ $modules_to_test != *"$dep"* ]]; then
        modules_to_test="$modules_to_test $dep"
      fi
    done
  fi
done

# print out for debug purposes
echo -----Executing tests for these modules-----
for module in $modules_to_test
do
    echo $module
done
echo -------------------------------------------

# run test commands for modules_to_test
for module in $modules_to_test
do
    echo "./gradlew :${module}:testDebugUnitTest"
    eval "./gradlew :${module}:testDebugUnitTest"
done
