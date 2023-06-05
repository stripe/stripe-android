#!/usr/bin/env ruby

# This script fetches all the keys from Lokalise and compares them to the keys in the Android
# project. If the project contains any keys that are not in Lokalise ("unsynced" keys), it will fail
# the build.
#
# This script is meant to be run on CI. If it finds unsynced keys, run
# `scripts/lokalise/update_lokalise_with_new_strings.rb` locally to create or update the keys in
# Lokalise.

require 'json'
require 'net/http'
require 'openssl'
require 'rexml/document'
require 'set'
require 'uri'

include REXML

def fetch_keys_from_lokalise
    page = 1
    results = []

    puts "Loading keys from Lokalise…"

    while true
        page_results = fetch_keys_for_page(page)
        break if page_results.empty?
        results += page_results
        page += 1
    end

    results
end

def fetch_keys_for_page(page)
    url = URI("https://api.lokalise.com/api2/projects/747824695e51bc2f4aa912.89576472/keys?include_translations=1&limit=500&page=#{page}")

    http = Net::HTTP.new(url.host, url.port)
    http.use_ssl = true

    request = Net::HTTP::Get.new(url)
    request["accept"] = 'application/json'

    api_token = ENV['LOKALISE_API_TOKEN']
    if api_token.nil?
        abort("Missing LOKALISE_API_TOKEN environment variable.")
    else
        request["X-Api-Token"] = api_token
    end

    response = http.request(request)
    hash = JSON.parse(response.read_body)
    hash['keys']
end

def filter_android_keys(all_keys)
    android_keys = all_keys.select { |key| key['platforms'].include? 'android' }
    android_keys.map { |key| key["key_name"]["android"] }
end

def list_local_keys
    project_root = File.dirname(Dir.pwd)
    modules = [
        'identity',
        'link',
        'paymentsheet',
        'payments-core',
        'payments-ui-core',
        'stripe-core',
        'stripe-ui-core',

#         These following modules don't use Lokalise
#         'camera-core',
#         'financial-connections',
#         'stripecardscan',
    ]

    files = modules.map { |mod| "#{project_root}/#{mod}/res/values/strings.xml" }

    key_objects = []

    files.each_with_index.each do |filepath, index|
        file = File.open(filepath).readlines.map(&:chomp).map(&:strip)
        xml_file = File.new(filepath)
        xml_doc = Document.new(xml_file)

        filename = "#{modules[index]}/strings.xml"

        xml_doc.elements.each('resources/string') do |element|
            # Get the main element information
            key_name = element.attributes['name']
            value = element.text

            # Get the line number with this shitty workaround because REXML changes the quotes from
            # double to single quotes.
            line_number = -1
            file.each_with_index do |line, index|
                # One key_name could be a substring of another key_name, so we need to match against
                # this text instead.
                text = 'name="#{key_name}"'

                if line.include?(text)
                    line_number = index
                    break
            	end
            end

            if line_number == -1
                abort("Some weird failure. Bother tillh about it.")
            end

            # Once we have the line of the string, its description is the comment in the line above.
            description = file[line_number - 1]
            if description.start_with?('<!--') && description.end_with?('-->')
                # Remove the leading <!-- and trailing --> from description
                description = description.delete_prefix('<!--').delete_suffix('-->').strip
            end

            key_object = {
                "key_name": key_name,
                "value": value,
                "filename": filename,
                "description": description,
            }

            key_objects << key_object
        end
    end

    key_objects
end

def determine_unsynced_keys(key_objects, remote_android_keys)
    unsynced_keys = []
    key_objects.each do |key|
        key_name = key[:key_name]

        if !remote_android_keys.include?(key_name)
            unsynced_keys << key
        end
    end
    unsynced_keys
end

# ---------------- Start of script ----------------

all_keys = fetch_keys_from_lokalise
remote_keys = filter_android_keys(all_keys)
local_keys = list_local_keys

unsynced_keys = determine_unsynced_keys(local_keys, remote_keys)

if unsynced_keys.empty?
    puts "No unsynced strings"
    exit 0
else
    puts "Found #{unsynced_keys.length} unsynced key(s)."
    unsynced_keys.each do |key|
        puts "* #{key[:key_name]}"
    end
    puts "Run `scripts/lokalise/update_lokalise_with_new_strings.rb` locally to update Lokalise."
    exit 1
end
