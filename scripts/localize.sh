#!/bin/bash

#This script will pull down the strings for each of the modules and copies
#them into the respective string directories.
#
#It will remove the android directory from which it works at the beginning
#It will not replace the default string value.
#It will do iso renames as needed.
#It will not perform a commit.
#
#It does generate an android/$MODULE-strings.xml file for use by other scripts.

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

echo "AVAILABLE LANGUAGES: "
lokalise2 --token $API_TOKEN --project-id $PROJECT_ID language list | grep iso

echo "DOWNLOADING LANGUAGES: ${LANGUAGES}"

for MODULE in "paymentsheet" "payments-core"
do
    echo ""
    echo ""
    echo "----------------------------------------------------------"
    echo "DOWNLOADING STRINGS in $MODULE MODULE: $MODULE/strings.xml"
    echo "----------------------------------------------------------"
    lokalise2 --token $API_TOKEN \
          --project-id $PROJECT_ID \
          file download \
          --format xml \
          --filter-langs $LANGUAGES \
          --filter-filenames $MODULE/strings.xml \
          --custom-translation-status-ids $FINAL_STATUS_ID \
          --export-sort "a_z" \
          --directory-prefix . \
          --original-filenames=false \
          --bundle-structure "android/$MODULE/values-%LANG_ISO%/strings.xml"

    # Need to download english separately because their strings are not marked final (this is what we uploaded)
    # This must be done after the first one.
    lokalise2 --token $API_TOKEN \
          --project-id $PROJECT_ID \
          file download \
          --format xml \
          --filter-filenames $MODULE/strings.xml \
          --filter-langs "en" \
          --export-sort "a_z" \
          --directory-prefix . \
          --original-filenames=false \
          --bundle-structure "android/$MODULE/values-%LANG_ISO%/strings.xml"

    #There is a command line switch that might be better than this, see: --language-mapping
    mv android/$MODULE/values-es-r419 android/$MODULE/values-b+es+419
    mv android/$MODULE/values-zh-rHant android/$MODULE/values-zh-rTW
    mv android/$MODULE/values-zh-rHans android/$MODULE/values-zh

    # This is used by the untranslated_project_keys.sh script
    cp android/$MODULE/values-en-rGB/strings.xml android/$MODULE-lokalize-strings.xml

    # Remove the existing strings files
    find ../$MODULE/res -type f -name strings.xml | xargs rm

    # Copy in the new strings files
    cp -R  android/$MODULE/* ../$MODULE/res/

    echo ""
    echo "TRANSLATED STRINGS (country codes): "
    echo "----------------------------------------------------------"
    ls -1 android/$MODULE/ | paste -sd "," - | sed 's/[,]*values[-]*//g'
done

