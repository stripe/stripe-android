#!/usr/bin/env ruby
# frozen_string_literal: true

require 'cgi'
require 'json'
require 'net/http'
require 'open3'
require 'optparse'
require 'securerandom'
require 'uri'
require_relative 'latency_test_utils'

PROJECT_ROOT = LatencyTestUtils::PROJECT_ROOT
APP_PACKAGE = 'com.stripe.android.paymentsheet.example'
ANALYTICS_HOST = ENV.fetch('STRIPE_SYNTHETICS_ANALYTICS_HOST', 'https://q.stripe.com')
ANALYTICS_UA = 'analytics.stripe_android-1.0'

def adb_getprop(property)
  stdout, status = Open3.capture2e('adb', 'shell', 'getprop', property, chdir: PROJECT_ROOT)
  return stdout.strip if status.success?

  warn stdout
  raise "Failed to read adb property #{property.inspect}."
end

def sdk_version
  @sdk_version ||= begin
    gradle_properties = File.read(File.join(PROJECT_ROOT, 'gradle.properties'))
    gradle_properties[/^VERSION_NAME=(.+)$/, 1]&.strip || 'unknown'
  end
end

def analytics_headers(device_info)
  x_stripe_user_agent = {
    lang: 'kotlin',
    bindings_version: sdk_version,
    os_version: device_info.fetch(:os_version),
    type: device_info.fetch(:device_type),
    model: device_info.fetch(:model)
  }

  {
    'User-Agent' => "Stripe/v1 AndroidBindings/#{sdk_version}",
    'Accept-Charset' => 'UTF-8',
    'X-Stripe-User-Agent' => x_stripe_user_agent.to_json
  }
end

def device_info
  manufacturer = adb_getprop('ro.product.manufacturer')
  brand = adb_getprop('ro.product.brand')
  model = adb_getprop('ro.product.model')

  {
    os_name: adb_getprop('ro.build.version.codename'),
    os_release: adb_getprop('ro.build.version.release'),
    os_version: adb_getprop('ro.build.version.sdk'),
    device_type: [manufacturer, brand, model].join('_'),
    model: model
  }
end

def analytics_params(test_name:, duration_ms:, session_id:, publishable_key:, device_info:, app_name:)
  {
    'analytics_ua' => ANALYTICS_UA,
    'os_name' => device_info.fetch(:os_name),
    'os_release' => device_info.fetch(:os_release),
    'os_version' => device_info.fetch(:os_version),
    'device_type' => device_info.fetch(:device_type),
    'bindings_version' => sdk_version,
    'is_development' => true,
    'session_id' => session_id,
    'timestamp' => Time.now.to_f,
    'app_name' => app_name,
    'event' => 'mpe.synthetic_latency',
    'test' => test_name,
    'duration' => duration_ms
  }.tap do |params|
    params['publishable_key'] = publishable_key if publishable_key && !publishable_key.empty?
  end
end

def encode_query(params)
  params.map do |key, value|
    "#{CGI.escape(key.to_s)}=#{CGI.escape(value.to_s)}"
  end.join('&')
end

def emit_synthetics_events(results:, publishable_key:, dry_run:)
  device = device_info
  app_name = APP_PACKAGE
  session_id = SecureRandom.uuid
  headers = analytics_headers(device)

  results.sort.each do |test_name, durations|
    durations.each_with_index do |duration_seconds, index|
      duration_ms = (duration_seconds * 1000).round
      params = analytics_params(
        test_name: test_name,
        duration_ms: duration_ms,
        session_id: session_id,
        publishable_key: publishable_key,
        device_info: device,
        app_name: app_name
      )
      uri = URI("#{ANALYTICS_HOST}?#{encode_query(params)}")

      if dry_run
        puts "DRY_RUN analytics event #{test_name} sample #{index + 1}: #{uri}"
        next
      end

      request = Net::HTTP::Get.new(uri)
      headers.each do |key, value|
        request[key] = value
      end

      response = Net::HTTP.start(uri.host, uri.port, use_ssl: uri.scheme == 'https') do |http|
        http.request(request)
      end

      unless response.is_a?(Net::HTTPSuccess)
        raise "Failed to send analytics for #{test_name} sample #{index + 1}: #{response.code} #{response.message}"
      end

      puts "Sent analytics event #{test_name} sample #{index + 1} (#{duration_ms}ms)"
    end
  end
end

options = {
  publishable_key: ENV['STRIPE_SYNTHETICS_PUBLISHABLE_KEY'],
  dry_run: false
}

OptionParser.new do |opts|
  opts.banner = <<~BANNER
    Usage: report_latency_synthetics.rb [options]

    Runs TestLatency, parses StripeSdk DurationProvider logcat output for the Loading key,
    and emits mpe.synthetic_latency analytics events directly from this script.

    Examples:
      ./scripts/report_latency_synthetics.rb
      ./scripts/report_latency_synthetics.rb --dry-run

    Options:
  BANNER

  opts.on('--dry-run', 'Print analytics requests without sending them') do
    options[:dry_run] = true
  end

  opts.on('-h', '--help', 'Prints this help') do
    puts opts
    exit
  end
end.parse!

puts 'Configuration:'
puts "  Publishable key: #{options[:publishable_key] ? '[provided]' : '[omitted]'}"
puts "  Dry run:         #{options[:dry_run]}"

results = LatencyTestUtils.run_latency_tests(1)
emit_synthetics_events(
  results: results,
  publishable_key: options[:publishable_key],
  dry_run: options[:dry_run]
)
