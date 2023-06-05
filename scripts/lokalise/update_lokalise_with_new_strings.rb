#!/usr/bin/env ruby

# This script fetches all the keys from Lokalise and compares them to the keys in the Android
# project. If the project contains any keys that are not in Lokalise ("unsynced" keys), it will
# determine whether to create a new key in Lokalise or update an existing one. The latter happens
# if we already have a key in Lokalise with the same value.
#
# This script is meant to be run locally. On CI, we run
# `scripts/localise/check_untranslated_strings.rb` to fail a build if any string isn't translated
# yet.

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
    android_keys.map { |key| key['key_name']['android'] }
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

def create_key(key_object)
    url = URI("https://api.lokalise.com/api2/projects/project_id/keys")

    http = Net::HTTP.new(url.host, url.port)
    http.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["accept"] = 'application/json'
    request["content-type"] = 'application/json'

    api_token = ENV['LOKALISE_API_TOKEN']
    if api_token.nil?
        abort("Missing LOKALISE_API_TOKEN environment variable.")
    else
        request["X-Api-Token"] = api_token
    end

    body = {
        "keys": [
            {
                "platforms": ["android"],
                "filenames": {
                    "android": key_object['filename'],
                },
                "translations": [
                    {
                        "language_iso": "en",
                        "translation": key_object['value'],
                        "is_reviewed": true,
                        "is_unverified": false,
                        "is_archived": false,
                    }
                ],
                "description": key_object['description'],
            }
        ]
    }

    request.body = body.to_s

    response = http.request(request)
    puts response.read_body
end

def find_existing_key(all_keys, key_object)
    value = key_object[:value]
    all_keys.each do |key|
        translations = key['translations']

        translations.each do |translation|
            # If we find a translation that matches the key_object's value, then this is the key
            # that we need to update
            if translation['language_iso'] == 'en' && translation['translation'] == value
                return key
            end
        end
    end
    return nil
end

def update_key(existing_key, key_object)
    puts "WIP"
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
else
    puts "Found #{unsynced_keys.length} unsynced key(s):"

    unsynced_keys.each do |unsynced_key|
        key_name = unsynced_key[:key_name]
        existing_key = find_existing_key(all_keys, unsynced_key)

        if existing_key.nil?
            puts "* Creating key \"#{key_name}\""
#             create_key(key_object)
        else
            existing_key_name = existing_key['key_name']['ios']
            puts "* Updating key \"#{existing_key_name}\""
#             update_key(existing_key, key_object)
        end
    end
end
