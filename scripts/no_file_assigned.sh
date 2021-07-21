#!/bin/bash
# This will download all the android files, and strings that are assigned to android
# but not to any files will be outputted.  This indicates an error.   It will cleanup
# files created at the end.
#
# This will not replace any files in the android project.
#
# This script can be run with no arguments:
#  ./no_file_assigned.sh

if [ -z "$API_TOKEN" ]; then
  echo "You need to add the API_TOKEN to: localization_vars.sh"
  exit
fi

if [[ -z $(which lokalise2) ]]; then
    echo "Installing lokalise2 via homebrew..."
    brew tap lokalise/cli-2
    brew install lokalise2
fi

if [[ -z $(which recode) ]]; then
    echo "Installing recode via homebrew..."
    brew install recode
fi

source localization_vars.sh

# This is the custom status ID for our project with which the localizers mark completed translations
FINAL_STATUS_ID=587

rm -rf android/*

lokalise2 --token $API_TOKEN \
          --project-id $PROJECT_ID \
          file download \
          --format xml \
          --custom-translation-status-ids $FINAL_STATUS_ID \
          --export-sort "a_z" \
          --directory-prefix . \

    
cat strings.xml

rm -rf payments-core
rm -rf paymentsheet
rm strings.xml
