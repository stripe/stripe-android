#!/bin/bash

# This script will do a regular expression match to find the string value in the ios or
# android projects, regardless of the translation status
#
# The script can be run as ./find_localize_key_for_value_regex.sh <regular expression>
# For instance: ./find_localize_key_for_value_regex.sh ".*optional.*"

if [ -z "$API_TOKEN" ]; then
  echo "You need to add the API_TOKEN to: localization_vars.sh"
  exit
fi

echo "Searching for string value: $1"

if [[ -z $(which lokalise2) ]]; then
    echo "Installing lokalise2 via homebrew..."
    brew tap lokalise/cli-2
    brew install lokalise2
fi

if [[ -z $(which recode) ]]; then
    echo "Installing recode via homebrew..."
    brew install recode
fi

if [[ -z $(which gsed) ]]; then
    echo "Installing gsed via homebrew..."
    brew install gnu-sed
fi

source localization_vars.sh

lokalise2 --token $API_TOKEN \
          --project-id $PROJECT_ID \
          file download \
          --format xml \
          --filter-langs en \
          --export-sort "a_z" \
          --directory-prefix . \
          --original-filenames=true > /dev/null

echo ""
echo "Android  matches (key,value):"
# This will print just the key name that matches the inputted regex
# The I at the end of sed makes it case insensitive
# gsed is used for case insensitive matching.
# -n in combination with the /p at the end of sed makes it print only lines that were replaced
find . -type f -name strings.xml | xargs gsed -E -n "s/<string name=\"(.*)\">($1)<\/string>/\1,\2/pI"

lokalise2 --token $API_TOKEN \
          --project-id $PROJECT_ID \
          file download \
          --format ios_sdk \
          --filter-langs en \
          --export-sort "a_z" \
          --directory-prefix . \
          --original-filenames=true > /dev/null

echo ""
echo "iOS matches (key,value):"
find . -type f -name Localizable.strings | xargs gsed -E -n "s/\"(.*)\" = \"($1)\";/\1,\2/pI"

echo ""
echo ""
