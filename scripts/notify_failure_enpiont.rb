#!/usr/bin/ruby
require 'uri'
require 'net/http'
require 'json'
require 'openssl'
require 'base64'

env_sdk_failure_notif_endpoint = ARGV[0]
env_sdk_failure_notif_endpoint_hmac_key = ARGV[1]
github_run_id = ARGV[2]

if !env_sdk_failure_notif_endpoint || !env_sdk_failure_notif_endpoint_hmac_key
  puts "Three environment variables required: `SDK_FAILURE_NOTIFICATION_ENDPOINT` and `SDK_FAILURE_NOTIFICATION_ENDPOINT_HMAC_KEY`"
  exit 102
end

if !github_run_id
  puts "github_run_id not found"
  exit 102
end

uri = URI(env_sdk_failure_notif_endpoint)
hmac_key = Base64.decode64(env_sdk_failure_notif_endpoint_hmac_key)

http = Net::HTTP.new(uri.host, uri.port).tap do |http|
  http.use_ssl = true
end
req = Net::HTTP::Post.new(uri, 'Content-Type' => 'application/json')

# Set up base params for tickets created under either
# success or failure cases
params = {
  project: "RUN_MOBILESDK",
}

params[:summary] = "stripe-android E2E test failed"
params[:description] = "Please ACK this ticket and investigate the failure. See https://github.com/stripe/stripe-android/actions/runs/#{github_run_id}"

req.body = params.to_json

# Auth
digest = OpenSSL::Digest.new('sha256')
header_data = OpenSSL::HMAC.digest(digest, hmac_key, req.body)
header_data_64 = Base64.strict_encode64(header_data)
req.add_field 'X-TM-Signature', header_data_64

res = http.request(req)
