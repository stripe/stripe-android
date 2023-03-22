#!/usr/bin/ruby
require 'uri'
require 'net/http'
require 'json'
require 'openssl'
require 'base64'

env_sdk_failure_notif_endpoint = ARGV[0]
env_sdk_search_endpoint_hmac_key = ARGV[1]
github_run_id = ARGV[2]
jira_project = ARGV[3]

if !env_sdk_failure_notif_endpoint || !env_sdk_search_endpoint_hmac_key
  puts "SDK_NOTIFICATION_SEARCH_ENDPOINT` or `SDK_FAILURE_NOTIFICATION_ENDPOINT_HMAC_KEY` not found"
  exit 102
end

if !github_run_id
  puts "github_run_id not found"
  exit 102
end

if !jira_project
  puts "jira_project not found"
  exit 102
end

uri = URI(env_sdk_failure_notif_endpoint)
hmac_key = Base64.decode64(env_sdk_search_endpoint_hmac_key)

http = Net::HTTP.new(uri.host, uri.port).tap do |http|
  http.use_ssl = true
end

# List of old tickets
list_req = Net::HTTP::Get.new(uri)

digest = OpenSSL::Digest.new('sha256')
header_data = OpenSSL::HMAC.digest(digest, hmac_key, "")
header_data_64 = Base64.strict_encode64(header_data)
list_req.add_field 'X-TM-Signature', header_data_64

res = http.request(list_req)

puts res