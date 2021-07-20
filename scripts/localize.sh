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


#strings.xml -- are android strings not assigned to a file.

for MODULE in "paymentsheet" "payments-core"
do
    echo "Downloading strings in $MODULE module: $MODULE/strings.xml"
    lokalise2 --token $API_TOKEN \
          --project-id $PROJECT_ID \
          file download \
          --format xml \
          --filter-filenames $MODULE/strings.xml \
          --filter-langs $LANGUAGES \
          --custom-translation-status-ids $FINAL_STATUS_ID \
          --export-sort "a_z" \
          --directory-prefix . \
          --original-filenames=false \
          --bundle-structure "android/$MODULE/values-%LANG_ISO%/strings.xml" \


    #There is a command line switch that might be better than this, see: --language-mapping
    mv android/$MODULE/values-es android/$MODULE/values-b+es+419
    mv android/$MODULE/values-zh-rHant android/$MODULE/values-zh-rTW
    mv android/$MODULE/values-zh-rHans android/$MODULE/values-zh
    #mv android/$MODULE/values-id android/$MODULE/values-in
done

# --language-mapping string                 List of languages to override default iso codes for this export (JSON, see https://lokalise.com/api2docs/curl/#transition-download-files-post).


exit

for DIRECTORY in ${LOCALIZATION_DIRECTORIES[@]}
do
  for f in ${DIRECTORY}/Resources/Localizations/*.lproj/*.strings
  do

    # Don't modify the en.lproj strings file or it could get out of sync with
    # genstrings and our linters won't pass
    if [[ "$(basename "$(dirname "$f")")" == "en.lproj" ]]
    then
      continue
    fi

    # lokalise doesn't consistently add lines in between keys, but genstrings does
    # so here we add an empty line every two lines (first line is comment, second is key=val)
    TMP_FILE=$(mktemp /tmp/download_localized_strings_from_lokalise.XXXXXX)

    awk 'BEGIN {last_empty = 0; last_content = 0; row = 0;}; {if (NR == last_empty + 3 && NF > 1) {print ""; last_empty = NR - 1} else if (NF <= 1) {last_empty = NR}}; {if (NF > 1) {last_content = NR}}; {row = row + 1}; 1; END {if (row == last_content) {print ""}}' $f > $TMP_FILE && mv $TMP_FILE $f
  done
done

