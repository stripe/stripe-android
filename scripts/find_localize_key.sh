#!/bin/bash
# This script will find a string in the ios or android projects, regardless of the translation status

echo "Searching for string key: $1"

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

# Load LOCALIZATION_DIRECTORIES & LANGUAGES variables
source localization_vars.sh

#lokalise2 --token $API_TOKEN \
#          --project-id $PROJECT_ID \
#          file download \
#          --format xml \
#          --filter-langs en \
#          --export-sort "a_z" \
#          --directory-prefix . \
#          --original-filenames=true

echo ""
echo ""
echo "Android  matches"
echo "key,value"
echo "-------------"
# This will print just the key name that matches the inputted regex
# The I at the end of sed makes it case insensitive
# gsed is used for case insensitive matching.
# -n in combination with the /p at the end of sed makes it print only lines that were replaced
find . -type f -name strings.xml | xargs gsed -E -n "s/<string name=\"(.*)\".*>.*<\/string>/\1/pI"

