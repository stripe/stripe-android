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
        actions = diff.map do |unsynced_key|
            key_name = unsynced_key[:key_name]
            value = unsynced_key[:value]
            existing_key = diff_provider.find_existing_key(all_keys, value)

            if existing_key.nil?
                {
                    action: "create",
                    message: "Create key \"#{key_name}\" with value \"#{value}\"",
                    key_object: unsynced_key,
                }
            else
                existing_key_name = existing_key['key_name']['ios']
                {
                    action: "update",
                    message: "Update key \"#{existing_key_name}\" with local string \"#{key_name}\"",
                    existing_key: existing_key,
                    key_object: unsynced_key,
                }
            end
        end

        # Ask user to confirm actions
        puts "Found #{diff.length} unsynced key(s). Do you want to perform the following actions? (y/n)"
        actions.each do |action|
            puts "* #{action[:message]}"
        end

        answer = gets.chomp
        if answer == "y"
            actions.each do |action|
                key_object = action[:key_object]
                if action[:action] == "create"
                    puts "Creating key: #{key_object[:key_name]}"
#                     success = client.create_key(key_object)
#                     if success
#                         puts "Created key: #{key_object[:key_name]}"
#                     else
#                         puts "Failed to create key: #{key_object[:key_name]}"
#                     end
                else
                    puts "Updating key: #{key_object[:key_name]}"
#                     existing_key = action[:existing_key]
#                     success = client.update_key(existing_key, key_object)
#                     if success
#                         puts "Updated key: #{key_object[:key_name]}"
#                     else
#                         puts "Failed to update key: #{key_object[:key_name]}"
#                     end
                end
            end
        else
            abort("You aborted the automated script. Please create/update the keys manually.")
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
