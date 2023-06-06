require 'json'
require 'net/http'
require 'openssl'
require 'uri'

class LokaliseClient

    def fetch_keys
        all_keys = fetch_keys_from_lokalise
        android_keys = filter_android_keys(all_keys)
        return all_keys, android_keys
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
                    "key_name": {
                        "android": key_object['key_name'],
                    },
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
        success = response.kind_of? Net::HTTPSuccess

        success
    end

    def update_key(existing_key, key_object)
        url = URI("https://api.lokalise.com/api2/projects/project_id/keys")

        http = Net::HTTP.new(url.host, url.port)
        http.use_ssl = true

        request = Net::HTTP::Put.new(url)
        request["accept"] = 'application/json'
        request["content-type"] = 'application/json'

        api_token = ENV['LOKALISE_API_TOKEN']
        if api_token.nil?
            abort("Missing LOKALISE_API_TOKEN environment variable.")
        else
            request["X-Api-Token"] = api_token
        end

        key['key_name']['android'] = key_object['key_name']
        key['filenames']['android'] = key_object['filename']
        key['platforms'] << 'android'

        body = {
            "keys": [key]
        }

        request.body = body.to_s

        response = http.request(request)
        success = response.kind_of? Net::HTTPSuccess

        if !success
            puts "Failed to update key: #{key_object[:key_name]}"
        else
            puts "Updated key: #{key_object[:key_name]}"
        end
    end

    private

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
end
