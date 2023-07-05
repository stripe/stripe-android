#!/usr/bin/env ruby

# This script fetches all the keys from Lokalise and compares them to the keys in the Android
# project. If the project contains any keys that are not in Lokalise ("unsynced" keys), it will
# determine whether to create a new key in Lokalise or update an existing one. The latter happens
# if we already have a key in Lokalise with the same value.
#
# This script is meant to be run locally. On CI, we run
# `scripts/localise/check_untranslated_strings.rb` to fail a build if any string isn't translated
# yet.

require_relative 'lokalise_client'
require_relative 'processor'
require_relative 'string_resources'

def handle_result(client, actions)
    if actions.empty?
        puts "✅ No unsynced strings"
        exit 0
    else
        # Ask user to confirm actions
        puts "⛔️ Required Lokalise actions:"
        actions.each_with_index do |action, index|
            puts "#{index+1}. #{action.description}"
        end
        puts "Continue? (y/n)"

        answer = gets.chomp
        if answer == "y"
            outcomes = actions.each_with_index.map { |action, index| action.perform(client) }

            outcomes.each_with_index do |outcome, index|
                puts "#{index+1}. #{outcome}"
            end
        else
            abort("🤷 You aborted the automated script. Please create/update the keys manually… somehow…")
        end
    end
end

# ---------------- Start of script ----------------

client = LokaliseClient.new
lokalise_keys = client.fetch_keys

string_resources = StringResources.new
local_keys = string_resources.fetch

processor = Processor.new(lokalise_keys)
actions = processor.determine_required_actions(local_keys)

handle_result(client, actions)
