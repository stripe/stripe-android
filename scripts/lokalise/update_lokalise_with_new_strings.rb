#!/usr/bin/env ruby

# This script fetches all the keys from Lokalise and compares them to the keys in the Android
# project. If the project contains any keys that are not in Lokalise ("unsynced" keys), it will
# determine whether to create a new key in Lokalise or update an existing one. The latter happens
# if we already have a key in Lokalise with the same value.
#
# Run this script locally from the scripts directory.

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
        if answer != "y" && answer != "Y"
            abort("🤷 You aborted the automated script. Please create/update the keys manually… somehow…")
        end

        outcomes = actions.each_with_index.map { |action, index| action.perform(client) }

        outcomes.each_with_index do |outcome, index|
            puts "#{index+1}. #{outcome}"
        end

        fetch_updated_translations
    end
end

def fetch_updated_translations
    puts "Fetching updated translation from Lokalise…"
    command = "./localize.sh ENGLISH_ONLY"
    system(command) or raise "Failed to execute #{command}"
    puts "✅ Fetched updated translations"
end

# ---------------- Start of script ----------------

client = LokaliseClient.new
lokalise_keys = client.fetch_keys

string_resources = StringResources.new
local_keys = string_resources.fetch

processor = Processor.new(lokalise_keys)
actions = processor.determine_required_actions(local_keys)

handle_result(client, actions)
