# This script first finds all the modules changed in a PR, then finds all their dependent modules, finally it executes testDebugUnitTest on these modules.
# If some critical dependency is changed, e.g the root build.gradle file, all unit tests will be executed.
echo -----Fetching orign master
git fetch origin master:refs/remotes/origin/master
git branch -a
echo -----Done fetching orign master

echo -----Calculating modules to test
# directory names that corresponds to a module that has unit tests - this list needs to be manually updated when a new module is added/deleted
TESTABLE_MODULES="payments payments-core paymentsheet wechatpay link stripecardscan identity stripe-core payments-ui-core camera-core"
# a function to check if a dir is in TESTABLE_MODULES
isTestableModule() {
  [[ $TESTABLE_MODULES =~ (^|[[:space:]])$1($|[[:space:]]) ]]
}

# Files or directories that trigger all the unitest run if changed
CRITICAL_DEPS="build.gradle settings.gradle"
# a function to check if a file/dir is in CRITICAL_DEPS
isCriticalDeps() {
  [[ $CRITICAL_DEPS =~ (^|[[:space:]])$1($|[[:space:]]) ]]
}

# Determines the list passed in as $1 contains the element passed in as $2
listContainsElement() {
  [[ $1 =~ (^|[[:space:]])$2($|[[:space:]]) ]]
}

# find all dirs changed through git diff
changed_dirs=""
while read line; do
  module_name=${line%%/*} # This gets the first word before '/'
  # add this dir if we haven't add it yet
  if ! listContainsElement "${changed_dirs[@]}" $module_name; then
    changed_dirs="$changed_dirs $module_name" # string concat
  fi
done < <(git diff --name-only remotes/origin/master)

# for all changed dirs, first check if it's a critical dep, if so run all tests and exit
for dir in $changed_dirs
do
  if isCriticalDeps $dir; then
    echo critical dep $dir is changed, running ALL unit tests
    echo "./gradlew testDebugUnitTest"
    eval "./gradlew testDebugUnitTest"
    exit
  fi
done

# no critical deps changed, run tests on selective modules

# changed_modules are the ones that are directly changed in the PR
changed_modules=""
for dir in $changed_dirs
do
  if isTestableModule $dir; then
    changed_modules="$changed_modules $dir"
  fi
done

# affected_modules are the ones that are not directly changed, but have a dependency on modules in changed_modules
affected_modules=""
for testable_module in $TESTABLE_MODULES
do
  # this testable_module is directly changed by the PR, will execute its test, look at next one
  if listContainsElement "${changed_modules[@]}" $testable_module; then
    continue
  fi

  # The gradle command outputs all dependencies for a testable_module. For project dependencies, it looks like this:
  #
  # +--- project :payments-core
  # |    +--- project :stripe-core
  # +--- project :stripe-core (*)
  # +--- project :payments-ui-core
  #
  # Note dependencies that appear more than once are suffixed with "(*)"
  # The output is grep-ed with all project dependency lines that don't end with ")", then the last part of the line after : is cut
  #
  # For the previous output, module_deps will be assigned these values: ["payments-core", "stripe-core", "payments-ui-core"]
  module_deps=$(./gradlew :$testable_module:dependencies --configuration debugCompileClasspath | grep '+--- project :.*\w$' | cut -d ":" -f 2)

  # check if testable_module's dependency include the modules directly changed in this PR. If so, we need to add testable_module
  for changed_module in $changed_modules
  do
    if listContainsElement "${module_deps[@]}" $changed_module; then
      if ! listContainsElement "${affected_modules[@]}" $testable_module; then
        affected_modules="$affected_modules $testable_module"
      fi
      break
    fi
  done
done

# concatenate changed_modules to affected_modules
for changed_module in $changed_modules
do
  affected_modules="$affected_modules $changed_module"
done

# print out for debug purposes
echo -----Executing tests for these modules-----
for module in $affected_modules
do
  echo $module
done
echo -------------------------------------------

# run test commands for affected_modules
test_command="./gradlew"
for module in $affected_modules
do
  test_command="$test_command :${module}:testDebugUnitTest"
done

echo $test_command
eval $test_command