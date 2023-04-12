#!/usr/bin/ruby
require 'uri'
require 'net/http'
require 'json'
require 'openssl'
require 'base64'

env_sdk_failure_notif_endpoint = ARGV[0]
env_sdk_failure_notif_endpoint_hmac_key = ARGV[1]

if !env_sdk_failure_notif_endpoint || !env_sdk_failure_notif_endpoint_hmac_key
  puts "SDK_FAILURE_NOTIFICATION_ENDPOINT` or `SDK_FAILURE_NOTIFICATION_ENDPOINT_HMAC_KEY` not found"
  exit 102
end

failing_run_url = ARGV[2]

if !failing_run_url
  puts "failing_run_url not found"
  exit 102
end

jira_project = ARGV[3]

if !jira_project
  puts "jira_project not found"
  exit 102
end

uri = URI(env_sdk_failure_notif_endpoint)
hmac_key = Base64.decode64(env_sdk_failure_notif_endpoint_hmac_key)

http = Net::HTTP.new(uri.host, uri.port).tap do |http|
  http.use_ssl = true
end
req = Net::HTTP::Post.new(uri, 'Content-Type' => 'application/json')

params = {
  project: jira_project,
}

params[:summary] = "stripe-android E2E test failed"
params[:description] = "Please ACK this ticket and investigate the failure. See %s" % failing_run_url
params[:components] = %w[Android]

req.body = params.to_json

# Auth
digest = OpenSSL::Digest.new('sha256')
header_data = OpenSSL::HMAC.digest(digest, hmac_key, req.body)
header_data_64 = Base64.strict_encode64(header_data)
req.add_field 'X-TM-Signature', header_data_64

res = http.request(req)
