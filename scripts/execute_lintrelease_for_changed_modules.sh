# This script first finds all the modules changed in a PR then executes lintRelease on these modules.
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
#  if [[ !($changed_dirs =~ (^| )"$module_name"($| )) ]]; then
  if ! listContainsElement "${changed_dirs[@]}" $module_name; then
    changed_dirs="$changed_dirs $module_name" # string concat
  fi
done < <(git diff --name-only remotes/origin/master)


# changed_modules are the ones that are directly changed in the PR
changed_modules=""
for dir in $changed_dirs
do
  if isTestableModule $dir; then
    changed_modules="$changed_modules $dir"
  fi
done

# print out for debug purposes
echo -----Executing lintRelease for these modules-----
for module in $changed_modules
do
  echo $module
done
echo -------------------------------------------

# run lintRelease for changed_modules
for module in $changed_modules
do
    echo "./gradlew :${module}:lintRelease"
    eval "./gradlew :${module}:lintRelease"
done
