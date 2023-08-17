#!/usr/bin/env ruby

# This script fetches all the keys from Lokalise and compares them to the keys in the Android
# project. If the project contains any keys that are not in Lokalise ("unsynced" keys), it will fail
# the build.
#
# This script is meant to be run on CI. If it finds unsynced keys, run
# `scripts/lokalise/update_lokalise_with_new_strings.rb` locally to create or update the keys in
# Lokalise.

require_relative 'lokalise_client'
require_relative 'processor'
require_relative 'string_resources'

def handle_result(actions)
    if actions.empty?
        puts "✅ No Lokalise actions required."
        exit 0
    else
        puts "⛔️ Required Lokalise actions:"
        actions.each_with_index do |action, index|
            puts "#{index+1}. #{action.description}"
        end
        puts "👉 Run `scripts/lokalise/update_lokalise_with_new_strings.rb` locally to update Lokalise."
        exit 1
    end
end

# ---------------- Start of script ----------------

client = LokaliseClient.new
lokalise_keys = client.fetch_keys

string_resources = StringResources.new
local_keys = string_resources.fetch

processor = Processor.new(lokalise_keys)
actions = processor.determine_required_actions(local_keys)

handle_result(actions)
