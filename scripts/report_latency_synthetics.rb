#!/usr/bin/env ruby
# frozen_string_literal: true

require 'cgi'
require 'json'
require 'net/http'
require 'open3'
require 'optparse'
require 'securerandom'
require 'uri'

PROJECT_ROOT = File.expand_path('..', __dir__)
APP_PACKAGE = 'com.stripe.android.paymentsheet.example'
LOGCAT_TAG = 'StripeSdk'
LATENCY_TEST_CLASS = 'com.stripe.android.latency.TestLatency'
DURATION_KEY = 'Loading'
MAX_INVOCATION_ATTEMPTS = 3
LOGCAT_BUFFER_SIZE = ENV.fetch('LOGCAT_BUFFER_SIZE', '16M')
ANALYTICS_HOST = ENV.fetch('STRIPE_SYNTHETICS_ANALYTICS_HOST', 'https://q.stripe.com')
ANALYTICS_UA = 'analytics.stripe_android-1.0'

def run_latency_tests(sample_count)
  (1..MAX_INVOCATION_ATTEMPTS).each do |attempt|
    puts "Collecting #{sample_count} sample(s) in one Gradle invocation..."
    puts "Attempt #{attempt}/#{MAX_INVOCATION_ATTEMPTS}"

    output = run_android_latency_tests(sample_count)
    print_raw_result_debug_summary(output)
    results = parse_latency_results(output)
    expected_test_count = extract_expected_test_count(output)

    if valid_invocation_results?(
      results: results,
      sample_count: sample_count,
      expected_test_count: expected_test_count
    )
      puts "\nFound #{results.length} test(s) with results"
      return results
    end

    puts "\nInvocation did not produce a complete sample set."
    puts "  Expected tests: #{expected_test_count || 'unknown'}"
    puts "  Actual tests:   #{results.length}"
    if results.any?
      puts '  Samples per test:'
      results.sort.each do |test_name, durations|
        puts "    #{test_name}: #{durations.length}"
      end
    end
  end

  raise "Failed to collect a complete sample set after #{MAX_INVOCATION_ATTEMPTS} attempts"
end

def run_android_latency_tests(sample_count)
  clear_logcat

  command = [
    './gradlew',
    '--no-configuration-cache',
    ':paymentsheet-example:connectedBaseDebugAndroidTest',
    "-Pandroid.testInstrumentationRunnerArguments.class=#{LATENCY_TEST_CLASS}",
    "-PLATENCY_EXPERIMENT_ITERATIONS=#{sample_count}"
  ]

  output = +''

  Dir.chdir(PROJECT_ROOT) do
    Open3.popen2e(*command) do |stdin, stdout_stderr, wait_thread|
      stdin.close

      stdout_stderr.each_line do |line|
        print line
        output << line
      end

      exit_status = wait_thread.value
      unless exit_status.success?
        puts "\nWarning: Gradle command exited with status #{exit_status.exitstatus}"
        puts 'Continuing to parse available logcat output...'
      end
    end
  end

  output + "\n" + dump_logcat
end

def clear_logcat
  resize_logcat_buffer

  stdout, status = Open3.capture2e('adb', 'logcat', '-c', chdir: PROJECT_ROOT)
  return if status.success?

  warn stdout
  raise 'Failed to clear adb logcat. Ensure adb is installed and a device/emulator is available.'
end

def resize_logcat_buffer
  stdout, status = Open3.capture2e('adb', 'logcat', '-G', LOGCAT_BUFFER_SIZE, chdir: PROJECT_ROOT)
  return if status.success?

  warn stdout
  raise "Failed to resize adb logcat buffer to #{LOGCAT_BUFFER_SIZE}."
end

def dump_logcat
  stdout, status = Open3.capture2e(
    'adb',
    'logcat',
    '-d',
    '-s',
    "#{LOGCAT_TAG}:D",
    '*:S',
    chdir: PROJECT_ROOT
  )

  return stdout if status.success?

  warn stdout
  raise 'Failed to read adb logcat. Ensure adb is installed and a device/emulator is available.'
end

def parse_latency_results(output)
  results = Hash.new { |h, k| h[k] = [] }
  current_test_name = nil
  start_time = nil

  output.each_line do |line|
    if line =~ /LATENCY_TEST_CASE_STARTED:\s*(.+?)\s*$/
      current_test_name = Regexp.last_match(1).strip
      start_time = nil
      next
    end

    if line =~ /LATENCY_TEST_CASE_FINISHED:\s*(.+?)\s*$/
      finished_test_name = Regexp.last_match(1).strip
      current_test_name = nil if finished_test_name == current_test_name
      start_time = nil
      next
    end

    if line =~ /DURATION_STARTED:\s*#{Regexp.escape(DURATION_KEY)}:\s*(\d+)/
      start_time = Regexp.last_match(1).to_i
      next
    end

    next unless line =~ /DURATION_ENDED:\s*#{Regexp.escape(DURATION_KEY)}:\s*(\d+)/
    next if current_test_name.nil? || start_time.nil?

    end_time = Regexp.last_match(1).to_i
    duration_seconds = (end_time - start_time) / 1000.0
    results[current_test_name] << duration_seconds
    puts "  Found result: #{current_test_name} = #{format('%.4f', duration_seconds)}s"
    start_time = nil
  end

  results
end

def print_raw_result_debug_summary(output)
  counts = Hash.new(0)

  output.each_line do |line|
    next unless line =~ /LATENCY_TEST_CASE_STARTED:\s*(.+?)\s*$/

    test_name = Regexp.last_match(1).strip
    counts[test_name] += 1
  end

  puts "\nRaw test start counts for this invocation:"
  if counts.empty?
    puts '  No LATENCY_TEST_CASE_STARTED lines found'
    return
  end

  counts.sort.each do |test_name, count|
    puts "  #{test_name}: #{count}"
  end
end

def extract_expected_test_count(output)
  match = output.match(/Starting (\d+) tests on/)
  match && match[1].to_i
end

def valid_invocation_results?(results:, sample_count:, expected_test_count:)
  return false if expected_test_count.nil?
  return false unless results.length == expected_test_count

  results.values.all? { |durations| durations.length == sample_count }
end

def inside_clean_worktree?
  Dir.chdir(PROJECT_ROOT) do
    stdout, status = Open3.capture2e('git', '-c', 'core.fsmonitor=false', 'status', '--porcelain')
    raise stdout unless status.success?

    stdout.strip.empty?
  end
end

def checkout_commit(commit)
  Dir.chdir(PROJECT_ROOT) do
    success = system('git', '-c', 'core.fsmonitor=false', 'checkout', commit)
    raise "Failed to checkout commit #{commit}" unless success
  end
end

def current_checkout
  Dir.chdir(PROJECT_ROOT) do
    branch, branch_status = Open3.capture2e('git', 'rev-parse', '--abbrev-ref', 'HEAD')
    raise branch unless branch_status.success?

    commit, commit_status = Open3.capture2e('git', 'rev-parse', 'HEAD')
    raise commit unless commit_status.success?

    branch = branch.strip
    commit = commit.strip

    branch == 'HEAD' ? commit : branch
  end
end

def restore_checkout(target)
  puts "\nRestoring original checkout: #{target}"
  Dir.chdir(PROJECT_ROOT) do
    system('git', '-c', 'core.fsmonitor=false', 'checkout', target)
  end
end

def adb_getprop(property)
  stdout, status = Open3.capture2e('adb', 'shell', 'getprop', property, chdir: PROJECT_ROOT)
  return stdout.strip if status.success?

  warn stdout
  raise "Failed to read adb property #{property.inspect}."
end

def installed_app_version_code
  stdout, status = Open3.capture2e('adb', 'shell', 'dumpsys', 'package', APP_PACKAGE, chdir: PROJECT_ROOT)
  return Regexp.last_match(1) if status.success? && stdout =~ /versionCode=(\d+)/

  warn stdout unless status.success?
  nil
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

def device_info(locale_override)
  manufacturer = adb_getprop('ro.product.manufacturer')
  brand = adb_getprop('ro.product.brand')
  model = adb_getprop('ro.product.model')

  {
    os_name: adb_getprop('ro.build.version.codename'),
    os_release: adb_getprop('ro.build.version.release'),
    os_version: adb_getprop('ro.build.version.sdk'),
    device_type: [manufacturer, brand, model].join('_'),
    model: model,
    locale: locale_override || adb_getprop('persist.sys.locale').yield_self { |value| value.empty? ? ENV.fetch('LANG', 'en_US.UTF-8').split('.').first : value }
  }
end

def analytics_params(test_name:, duration_ms:, session_id:, publishable_key:, device_info:, app_version:, app_name:)
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
    'locale' => device_info.fetch(:locale),
    'app_name' => app_name,
    'app_version' => app_version,
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

def emit_synthetics_events(results:, publishable_key:, locale_override:, dry_run:)
  device = device_info(locale_override)
  app_version = installed_app_version_code || 'unknown'
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
        app_version: app_version,
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
  iterations: 1,
  publishable_key: ENV['STRIPE_SYNTHETICS_PUBLISHABLE_KEY'],
  dry_run: false
}

OptionParser.new do |opts|
  opts.banner = <<~BANNER
    Usage: report_latency_synthetics.rb [options]

    Runs TestLatency, parses StripeSdk DurationProvider logcat output for the Loading key,
    and emits mpe.synthetic_latency analytics events directly from this script.

    Examples:
      ./scripts/report_latency_synthetics.rb --commit HEAD --publishable-key pk_test_123
      ./scripts/report_latency_synthetics.rb --iterations 3 --dry-run

    Options:
  BANNER

  opts.on('--commit COMMIT', 'Commit to test. Defaults to the current checkout.') do |commit|
    options[:commit] = commit
  end

  opts.on('--iterations N', Integer, 'Samples to collect per test (default: 1)') do |value|
    options[:iterations] = value
  end

  opts.on('--publishable-key KEY', 'Publishable key to include in analytics (optional)') do |value|
    options[:publishable_key] = value
  end

  opts.on('--locale LOCALE', 'Override locale param for analytics (defaults to device locale)') do |value|
    options[:locale] = value
  end

  opts.on('--dry-run', 'Print analytics requests without sending them') do
    options[:dry_run] = true
  end

  opts.on('-h', '--help', 'Prints this help') do
    puts opts
    exit
  end
end.parse!

if options[:iterations] < 1
  warn "Error: Iterations must be at least 1 (got #{options[:iterations]})"
  exit 1
end

if options[:commit] && !inside_clean_worktree?
  warn 'Error: stripe-android has uncommitted changes. Commit or stash them before testing a different commit.'
  exit 1
end

original_checkout = current_checkout
target_checkout = options[:commit] || original_checkout

puts 'Configuration:'
puts "  Target checkout: #{target_checkout}"
puts "  Iterations:      #{options[:iterations]}"
puts "  Publishable key: #{options[:publishable_key] ? '[provided]' : '[omitted]'}"
puts "  Dry run:         #{options[:dry_run]}"

begin
  checkout_commit(options[:commit]) if options[:commit]
  results = run_latency_tests(options[:iterations])
  emit_synthetics_events(
    results: results,
    publishable_key: options[:publishable_key],
    locale_override: options[:locale],
    dry_run: options[:dry_run]
  )
ensure
  restore_checkout(original_checkout) if options[:commit]
end
