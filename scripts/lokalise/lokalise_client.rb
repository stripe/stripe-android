require 'json'
require 'net/http'
require 'openssl'
require 'uri'

class LokaliseClient

    def set_request_x_api_token(request)
        api_token = ENV['LOKALISE_API_TOKEN']
        if api_token.nil?
            abort("Missing LOKALISE_API_TOKEN environment variable. Try \nexport LOKALISE_API_TOKEN=$(fetch-password lokalise-api-token-manual)\nin terminal ")
        else
            request["X-Api-Token"] = api_token
        end
    end

    def fetch_keys
        fetch_keys_from_lokalise
    end

    def create_key(key_object)
        url = URI("https://api.lokalise.com/api2/projects/#{project_id}/keys")

        http = Net::HTTP.new(url.host, url.port)
        http.use_ssl = true

        request = Net::HTTP::Post.new(url)
        request["accept"] = 'application/json'
        request["content-type"] = 'application/json'

        set_request_x_api_token(request)

        request_body = {
            "keys": [
                {
                    "key_name": {
                        "android": key_object[:key_name],
                        "ios": key_object[:key_name],
                        "web": key_object[:key_name],
                        "other": key_object[:key_name],
                    },
                    "platforms": ["android"],
                    "filenames": {
                        "android": key_object[:filename],
                    },
                    "translations": [
                        {
                            "language_iso": "en",
                            "translation": key_object[:lokalise_value],
                            "is_reviewed": true,
                            "is_unverified": false,
                            "is_archived": false,
                        }
                    ],
                    "description": key_object[:description],
                }
            ]
        }

        request.body = request_body.to_json
        response = http.request(request)

        if response.kind_of? Net::HTTPSuccess
            response_json = JSON.parse(response.body)
            errors = response_json['errors']
            errors.empty?
        else
            false
        end
    end

    def update_key(existing_key, key_object)
        url = URI("https://api.lokalise.com/api2/projects/#{project_id}/keys")

        http = Net::HTTP.new(url.host, url.port)
        http.use_ssl = true

        request = Net::HTTP::Put.new(url)
        request["accept"] = 'application/json'
        request["content-type"] = 'application/json'

        set_request_x_api_token(request)

        existing_key['key_name']['android'] = key_object[:key_name]
        existing_key['filenames']['android'] = key_object[:filename]
        existing_key['platforms'] << 'android'

        if existing_key['custom_attributes'].empty?
            existing_key.delete('custom_attributes')
        end

        # No need to send all the translations back
        existing_key.delete('translations')

        request_body = {
            "keys": [existing_key]
        }

        request.body = request_body.to_json
        response = http.request(request)

        if response.kind_of? Net::HTTPSuccess
            response_json = JSON.parse(response.body)
            errors = response_json['errors']
            errors.empty?
        else
            false
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
        url = URI("https://api.lokalise.com/api2/projects/#{project_id}/keys?include_translations=1&limit=500&page=#{page}")

        http = Net::HTTP.new(url.host, url.port)
        http.use_ssl = true

        request = Net::HTTP::Get.new(url)
        request["accept"] = 'application/json'

        set_request_x_api_token(request)

        response = http.request(request)
        hash = JSON.parse(response.read_body)
        hash['keys']
    end

    def project_id
        "747824695e51bc2f4aa912.89576472"
    end
end
