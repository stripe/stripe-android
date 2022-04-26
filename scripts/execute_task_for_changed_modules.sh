# This script first finds all the modules changed in a PR then executes the taskname passed as the first parameter $1 to the script.
echo -----Fetching orign master
git fetch origin master:refs/remotes/origin/master
echo -----Done fetching orign master

# directory names that corresponds to a module that contains the task - this list needs to be manually updated when a new module is added/deleted
TESTABLE_MODULES="payments payments-core payments-model paymentsheet wechatpay link stripecardscan identity financial-connections financial-connections-compose stripe-core payments-ui-core camera-core"
# a function to check if a dir is in TESTABLE_MODULES
isTestableModule() {
  [[ $TESTABLE_MODULES =~ (^|[[:space:]])$1($|[[:space:]]) ]]
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


# changed_modules are the ones that are directly changed in the PR
changed_modules=""
for dir in $changed_dirs
do
  if isTestableModule $dir; then
    changed_modules="$changed_modules $dir"
  fi
done

task_to_run=$1

# print out for debug purposes
echo -----Executing $task_to_run for these modules-----
for module in $changed_modules
do
  echo $module
done
echo -------------------------------------------

# run ktlint for changed_modules
for module in $changed_modules
do
    echo "./gradlew :${module}:$task_to_run"
    eval "./gradlew :${module}:$task_to_run"
done
