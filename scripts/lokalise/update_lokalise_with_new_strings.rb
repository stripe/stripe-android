#!/usr/bin/env ruby

# This script fetches all the keys from Lokalise and compares them to the keys in the Android
# project. If the project contains any keys that are not in Lokalise ("unsynced" keys), it will
# determine whether to create a new key in Lokalise or update an existing one. The latter happens
# if we already have a key in Lokalise with the same value.
#
# This script is meant to be run locally. On CI, we run
# `scripts/localise/check_untranslated_strings.rb` to fail a build if any string isn't translated
# yet.

require_relative 'diff_provider'
require_relative 'local_strings'
require_relative 'lokalise_client'

def handle_result(client, diff_provider, all_keys, diff)
    if diff.empty?
        puts "No unsynced strings"
    else
        puts "Found #{diff.length} unsynced key(s):"

        diff.each do |unsynced_key|
            key_name = unsynced_key[:key_name]
            existing_key = diff_provider.find_existing_key(all_keys, unsynced_key)

            if existing_key.nil?
#                 success = client.create_key(key_object)
#
#                 if success
#                     puts "Created key: #{key_object[:key_name]}"
#                 else
#                     puts "Failed to create key: #{key_object[:key_name]}"
#                 end =end
            else
                existing_key_name = existing_key['key_name']['ios']
#                 success = client.update_key(existing_key, key_object)

#                 if success
#                     puts "Updated key: #{key_object[:key_name]}"
#                 else
#                     puts "Failed to update key: #{key_object[:key_name]}"
#                 end
            end
        end
    end
end

# ---------------- Start of script ----------------

client = LokaliseClient.new
strings_provider = LocalStringsProvider.new
diff_provider = DiffProvider.new

all_keys, remote_keys = client.fetch_keys
local_keys = strings_provider.load

diff = diff_provider.determine_unsynced_keys(local_keys, remote_keys)
handle_result(client, diff_provider, all_keys, diff)
