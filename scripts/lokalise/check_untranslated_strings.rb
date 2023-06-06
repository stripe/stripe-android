#!/usr/bin/env ruby

# This script fetches all the keys from Lokalise and compares them to the keys in the Android
# project. If the project contains any keys that are not in Lokalise ("unsynced" keys), it will fail
# the build.
#
# This script is meant to be run on CI. If it finds unsynced keys, run
# `scripts/lokalise/update_lokalise_with_new_strings.rb` locally to create or update the keys in
# Lokalise.

require_relative 'diff_provider'
require_relative 'local_strings'
require_relative 'lokalise_client'

def handle_result(diff)
    if diff.empty?
        puts "No unsynced strings"
        exit 0
    else
        puts "Found #{unsynced_keys.length} unsynced key(s)."
        diff.each do |key|
            puts "* #{key[:key_name]}"
        end
        puts "Run `scripts/lokalise/update_lokalise_with_new_strings.rb` locally to update Lokalise."
        exit 1
    end
end

# ---------------- Start of script ----------------

client = LokaliseClient.new
strings_provider = LocalStringsProvider.new
diff_provider = DiffProvider.new

all_keys, remote_keys = client.fetch_keys
local_keys = strings_provider.load

diff = diff_provider.determine_unsynced_keys(local_keys, remote_keys)
handle_result(diff)
