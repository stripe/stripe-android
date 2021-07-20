#!/bin/bash

#xml is for android, strings is for iOS
FORMAT=xml

if [[ -z $(which lokalise2) ]]; then
    echo "Installing lokalise2 via homebrew..."
    brew tap lokalise/cli-2
    brew install lokalise2
fi

if [[ -z $(which recode) ]]; then
    echo "Installing recode via homebrew..."
    brew install recode
fi

# Load LOCALIZATION_DIRECTORIES & LANGUAGES variables
source localization_vars.sh

# This is the custom status ID for our project with which the localizers mark completed translations
FINAL_STATUS_ID=587

rm -rf android/*

#strings.xml -- are android strings not assigned to a file.
#          --filter-langs $LANGUAGES \

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
